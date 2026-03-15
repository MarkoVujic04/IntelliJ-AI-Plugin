package com.markolukarami.copilotclone.frameworks.editor

import com.markolukarami.copilotclone.domain.entities.code.CodeAnalysis
import com.markolukarami.copilotclone.domain.entities.code.ConsistencyIssue
import com.markolukarami.copilotclone.domain.entities.code.IssueKind

object ConsistencyChecker {

    fun check(analysis: CodeAnalysis, rawLlmResponse: String): List<ConsistencyIssue> {
        val issues = mutableListOf<ConsistencyIssue>()

        val newSourceBlocks = extractNewSourceBlocks(rawLlmResponse)
        val methodSourceBlocks = extractMethodSourceBlocks(rawLlmResponse)
        val allBlocks = newSourceBlocks + methodSourceBlocks

        if (allBlocks.isEmpty()) return issues

        val knownFieldNames = analysis.fields.map { it.name }.toSet()
        val knownMethodNames = analysis.methods.map { it.name }.toSet()
        val knownImportTails = analysis.imports.map { it.substringAfterLast('.') }.toSet()

        val allKnownNames = knownFieldNames + knownMethodNames + knownImportTails +
                JAVA_BUILTIN_NAMES + JAVA_LANG_TYPES

        for (block in allBlocks) {
            issues += checkUndefinedNames(block, allKnownNames, analysis)
            issues += checkReturnMismatch(block, analysis)
            issues += checkNullHandling(block, analysis)
            issues += checkMissingImports(block, analysis)
        }

        return issues.distinctBy { "${it.kind}:${it.description}" }
    }

    private fun checkUndefinedNames(
        code: String,
        allKnownNames: Set<String>,
        analysis: CodeAnalysis
    ): List<ConsistencyIssue> {
        val issues = mutableListOf<ConsistencyIssue>()

        val callPattern = Regex("""(\b[a-z][A-Za-z0-9_]*)\s*\(""")
        val calls = callPattern.findAll(code).map { it.groupValues[1] }.toSet()

        val accessPattern = Regex("""(?:this\s*\.\s*)?(\b[a-z][A-Za-z0-9_]*)\b""")
        val accesses = accessPattern.findAll(code).map { it.groupValues[1] }.toSet()

        val referencedNames = calls + accesses

        for (name in referencedNames) {
            if (name.length < 3) continue
            if (name in allKnownNames) continue
            if (name in JAVA_KEYWORDS) continue
            if (name in COMMON_METHOD_NAMES) continue

            val knownTypes = analysis.fields.map { it.type.lowercase() } +
                    analysis.methods.flatMap { m -> m.parameters.map { it.type.lowercase() } }
            if (name.lowercase() in knownTypes) continue

            if (name in calls && name !in analysis.methods.map { it.name }) {
                val hasMatchingImport = analysis.imports.any { imp ->
                    imp.substringAfterLast('.').equals(name, ignoreCase = true) ||
                            imp.lowercase().contains(name.lowercase())
                }
                if (!hasMatchingImport) {
                    issues += ConsistencyIssue(
                        IssueKind.UNDEFINED_NAME,
                        "Method call '$name(...)' not found in target class methods or imports"
                    )
                }
            }
        }

        return issues.take(5)
    }

    private fun checkReturnMismatch(
        code: String,
        analysis: CodeAnalysis
    ): List<ConsistencyIssue> {
        val issues = mutableListOf<ConsistencyIssue>()

        for (existing in analysis.methods) {
            val sigPattern = Regex(
                """(?:public|private|protected)?\s*(?:static\s+)?(\S+)\s+${Regex.escape(existing.name)}\s*\("""
            )
            val match = sigPattern.find(code) ?: continue
            val declaredReturn = match.groupValues[1].trim()

            if (declaredReturn.isNotBlank() &&
                declaredReturn != existing.returnType &&
                declaredReturn !in setOf("@Override", "abstract", "final", "synchronized")
            ) {
                issues += ConsistencyIssue(
                    IssueKind.RETURN_MISMATCH,
                    "Method '${existing.name}' declared with return type '$declaredReturn' " +
                            "but existing signature has '${existing.returnType}'"
                )
            }
        }

        for (existing in analysis.methods) {
            val ktSigPattern = Regex(
                """fun\s+${Regex.escape(existing.name)}\s*\([^)]*\)\s*:\s*(\S+)"""
            )
            val match = ktSigPattern.find(code) ?: continue
            val declaredReturn = match.groupValues[1].trim()

            if (declaredReturn.isNotBlank() &&
                declaredReturn != existing.returnType &&
                declaredReturn.removeSuffix("?") != existing.returnType.removeSuffix("?")
            ) {
                issues += ConsistencyIssue(
                    IssueKind.RETURN_MISMATCH,
                    "Kotlin method '${existing.name}' declared return type '$declaredReturn' " +
                            "but existing signature has '${existing.returnType}'"
                )
            }
        }

        return issues
    }

    private fun checkNullHandling(
        code: String,
        analysis: CodeAnalysis
    ): List<ConsistencyIssue> {
        val issues = mutableListOf<ConsistencyIssue>()

        val nullableNames = mutableSetOf<String>()

        for (field in analysis.fields) {
            if (field.type.endsWith("?") ||
                field.type.startsWith("Optional") ||
                field.type == "String"
            ) {
                nullableNames += field.name
            }
        }

        for (method in analysis.methods) {
            for (param in method.parameters) {
                if (param.type.endsWith("?") || param.type.startsWith("Optional")) {
                    nullableNames += param.name
                }
            }
        }

        for (name in nullableNames) {
            val directDeref = Regex("""\b${Regex.escape(name)}\s*\.\s*\w+""")
            if (directDeref.containsMatchIn(code)) {
                val hasNullCheck = code.contains("$name != null") ||
                        code.contains("$name == null") ||
                        code.contains("$name?.") ||
                        code.contains("$name!!.") ||
                        code.contains("if ($name") ||
                        code.contains("$name?.let") ||
                        code.contains("$name.isPresent")

                if (!hasNullCheck) {
                    issues += ConsistencyIssue(
                        IssueKind.MISSING_NULL_HANDLING,
                        "Nullable-typed '$name' (${analysis.fields.find { it.name == name }?.type ?: "?"}) " +
                                "is dereferenced without null check"
                    )
                }
            }
        }

        return issues.take(5)
    }

    private fun checkMissingImports(
        code: String,
        analysis: CodeAnalysis
    ): List<ConsistencyIssue> {
        val issues = mutableListOf<ConsistencyIssue>()

        val typePattern = Regex("""\b([A-Z][A-Za-z0-9_]+)\b""")
        val referencedTypes = typePattern.findAll(code).map { it.groupValues[1] }.toSet()

        val knownTypes = analysis.imports.map { it.substringAfterLast('.') }.toSet() +
                analysis.fields.map { it.type.substringBefore('<').trim() }.toSet() +
                analysis.methods.flatMap { m ->
                    listOf(m.returnType.substringBefore('<').trim()) +
                            m.parameters.map { it.type.substringBefore('<').trim() }
                }.toSet() +
                JAVA_LANG_TYPES

        for (type in referencedTypes) {
            if (type in knownTypes) continue
            if (type.length <= 2) continue
            if (type.all { it.isUpperCase() || it == '_' }) continue

            issues += ConsistencyIssue(
                IssueKind.MISSING_IMPORT,
                "Type '$type' is used but not found in target file imports"
            )
        }

        return issues.take(5)
    }


    private fun extractNewSourceBlocks(raw: String): List<String> {
        return extractJsonStringValues(raw, "newSource") +
                extractJsonStringValues(raw, "newContent")
    }

    private fun extractMethodSourceBlocks(raw: String): List<String> {
        return extractJsonStringValues(raw, "methodSource")
    }

    private fun extractJsonStringValues(raw: String, key: String): List<String> {
        val results = mutableListOf<String>()
        val pattern = """"$key"\s*:\s*""""
        var searchFrom = 0

        while (true) {
            val keyIdx = raw.indexOf(pattern.trimEnd('"'), searchFrom)
            if (keyIdx < 0) break

            val colonIdx = raw.indexOf(':', keyIdx + key.length + 1)
            if (colonIdx < 0) break

            val quoteStart = raw.indexOf('"', colonIdx + 1)
            if (quoteStart < 0) break

            val sb = StringBuilder()
            var i = quoteStart + 1
            var escaped = false
            while (i < raw.length) {
                val c = raw[i]
                if (escaped) {
                    when (c) {
                        'n' -> sb.append('\n')
                        't' -> sb.append('\t')
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        else -> { sb.append('\\'); sb.append(c) }
                    }
                    escaped = false
                } else {
                    if (c == '\\') {
                        escaped = true
                    } else if (c == '"') {
                        break
                    } else {
                        sb.append(c)
                    }
                }
                i++
            }

            val value = sb.toString().trim()
            if (value.isNotBlank()) {
                results += value
            }

            searchFrom = i + 1
        }

        return results
    }

    private val JAVA_KEYWORDS = setOf(
        "if", "else", "for", "while", "do", "switch", "case", "break",
        "continue", "return", "try", "catch", "finally", "throw", "throws",
        "new", "this", "super", "class", "interface", "enum", "extends",
        "implements", "import", "package", "public", "private", "protected",
        "static", "final", "abstract", "void", "int", "long", "double",
        "float", "boolean", "char", "byte", "short", "null", "true", "false",
        "var", "val", "fun", "when", "is", "as", "in", "out", "override",
        "object", "companion", "data", "sealed", "lateinit", "lazy"
    )

    private val COMMON_METHOD_NAMES = setOf(
        "get", "set", "put", "add", "remove", "contains", "size", "isEmpty",
        "toString", "equals", "hashCode", "println", "print", "format",
        "valueOf", "parse", "trim", "split", "join", "map", "filter",
        "forEach", "let", "run", "apply", "also", "with", "take", "drop",
        "first", "last", "find", "any", "all", "none", "count", "sum",
        "max", "min", "sorted", "reversed", "distinct", "toList", "toSet",
        "toMap", "keys", "values", "entries", "iterator", "next", "hasNext",
        "close", "read", "write", "flush", "append", "insert", "delete",
        "replace", "substring", "indexOf", "lastIndexOf", "startsWith",
        "endsWith", "matches", "compile", "execute", "invoke", "call",
        "log", "warn", "error", "info", "debug", "getName", "setName",
        "getId", "getClass", "notify", "notifyAll", "wait", "clone",
        "compareTo", "length", "charAt", "getOrNull", "orEmpty",
        "isNullOrBlank", "isNullOrEmpty", "isBlank", "isNotBlank",
        "isEmpty", "isNotEmpty", "require", "check", "assert",
        "emptyList", "listOf", "mutableListOf", "setOf", "mutableSetOf",
        "mapOf", "mutableMapOf", "arrayOf", "arrayListOf"
    )

    private val JAVA_BUILTIN_NAMES = setOf(
        "System", "String", "Integer", "Long", "Double", "Float", "Boolean",
        "Object", "Class", "Math", "Arrays", "Collections", "List", "Map",
        "Set", "ArrayList", "HashMap", "HashSet", "Optional", "Stream",
        "StringBuilder", "StringBuffer", "Exception", "RuntimeException",
        "Throwable", "Thread", "Runnable", "Comparable", "Iterable",
        "Iterator", "Override", "Deprecated", "SuppressWarnings",
        "FunctionalInterface", "SafeVarargs"
    )

    private val JAVA_LANG_TYPES = setOf(
        "String", "Integer", "Long", "Double", "Float", "Boolean", "Byte",
        "Short", "Character", "Object", "Class", "System", "Math", "Void",
        "Number", "Comparable", "Iterable", "Throwable", "Exception",
        "RuntimeException", "Error", "Thread", "Runnable", "Override",
        "Deprecated", "SuppressWarnings", "StringBuilder", "StringBuffer",
        "Enum", "Record", "Cloneable", "AutoCloseable",
        "Unit", "Any", "Nothing", "Int", "Char", "Pair", "Triple",
        "Sequence", "Lazy", "Result", "Regex", "MatchResult"
    )
}


