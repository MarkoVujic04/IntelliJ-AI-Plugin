package com.markolukarami.copilotclone.domain.entities.code

data class MethodSignature(
    val name: String,
    val returnType: String,
    val parameters: List<ParameterInfo> = emptyList(),
    val filePath: String? = null
)

data class ParameterInfo(
    val name: String,
    val type: String
)

data class FieldInfo(
    val name: String,
    val type: String,
    val annotations: List<String> = emptyList()
)

data class CodeAnalysis(
    val targetFilePath: String,
    val imports: List<String> = emptyList(),
    val methods: List<MethodSignature> = emptyList(),
    val fields: List<FieldInfo> = emptyList(),
    val similarMethods: List<MethodSignature> = emptyList()
) {
    fun formatForPrompt(): String {
        val sb = StringBuilder()

        if (imports.isNotEmpty()) {
            sb.appendLine("IMPORTS IN TARGET FILE:")
            imports.forEach { sb.appendLine("  $it") }
            sb.appendLine()
        }

        if (fields.isNotEmpty()) {
            sb.appendLine("FIELDS / DEPENDENCIES IN TARGET CLASS:")
            fields.forEach { f ->
                val annots = if (f.annotations.isNotEmpty()) f.annotations.joinToString(" ", postfix = " ") else ""
                sb.appendLine("  $annots${f.type} ${f.name}")
            }
            sb.appendLine()
        }

        if (methods.isNotEmpty()) {
            sb.appendLine("EXISTING METHODS IN TARGET CLASS:")
            methods.forEach { m ->
                val params = m.parameters.joinToString(", ") { "${it.type} ${it.name}" }
                sb.appendLine("  ${m.returnType} ${m.name}($params)")
            }
            sb.appendLine()
        }

        if (similarMethods.isNotEmpty()) {
            sb.appendLine("SIMILAR METHODS FOUND IN OTHER FILES:")
            similarMethods.forEach { m ->
                val params = m.parameters.joinToString(", ") { "${it.type} ${it.name}" }
                val loc = if (m.filePath != null) " [${m.filePath}]" else ""
                sb.appendLine("  ${m.returnType} ${m.name}($params)$loc")
            }
            sb.appendLine()
        }

        return sb.toString().trimEnd()
    }
}

enum class IssueKind {
    UNDEFINED_NAME,
    TYPE_MISMATCH,
    MISSING_NULL_HANDLING,
    RETURN_MISMATCH,
    MISSING_IMPORT
}

data class ConsistencyIssue(
    val kind: IssueKind,
    val description: String
)

