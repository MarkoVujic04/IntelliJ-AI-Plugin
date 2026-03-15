package com.markolukarami.copilotclone.frameworks.editor

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import com.markolukarami.copilotclone.domain.entities.code.CodeAnalysis
import com.markolukarami.copilotclone.domain.entities.code.FieldInfo
import com.markolukarami.copilotclone.domain.entities.code.MethodSignature
import com.markolukarami.copilotclone.domain.entities.code.ParameterInfo

class CodeInspector(private val project: Project) {

    fun analyze(absolutePath: String): CodeAnalysis {
        val normalized = absolutePath.replace('\\', '/').trim()
        if (normalized.isBlank()) return CodeAnalysis(targetFilePath = normalized)

        return ReadAction.compute<CodeAnalysis, RuntimeException> {
            val vf = LocalFileSystem.getInstance().findFileByPath(normalized)
                ?: return@compute CodeAnalysis(targetFilePath = normalized)

            val psiFile = PsiManager.getInstance(project).findFile(vf)
                ?: return@compute CodeAnalysis(targetFilePath = normalized)

            if (psiFile !is PsiJavaFile) {
                return@compute analyzeNonJava(normalized, psiFile)
            }

            val imports = extractImports(psiFile)
            val cls = PsiTargetResolver.findFirstClass(psiFile)

            val fields = if (cls != null) extractFields(cls) else emptyList()
            val methods = if (cls != null) extractMethods(cls, normalized) else emptyList()
            val similar = if (cls != null) findSimilarMethods(psiFile, cls) else emptyList()

            CodeAnalysis(
                targetFilePath = normalized,
                imports = imports,
                methods = methods,
                fields = fields,
                similarMethods = similar
            )
        }
    }

    private fun extractImports(javaFile: PsiJavaFile): List<String> {
        val importList = javaFile.importList ?: return emptyList()
        return importList.importStatements.mapNotNull { it.qualifiedName }
    }

    private fun extractFields(cls: PsiClass): List<FieldInfo> {
        return cls.fields.map { field ->
            val typeName = field.type.presentableText
            val annotations = field.annotations.mapNotNull { a ->
                a.qualifiedName?.substringAfterLast('.')
            }
            FieldInfo(
                name = field.name,
                type = typeName,
                annotations = annotations
            )
        }
    }

    private fun extractMethods(cls: PsiClass, filePath: String): List<MethodSignature> {
        return cls.methods.map { method ->
            methodToSignature(method, filePath)
        }
    }

    private fun methodToSignature(method: PsiMethod, filePath: String?): MethodSignature {
        val returnType = method.returnType?.presentableText ?: "void"
        val params = method.parameterList.parameters.map { p ->
            ParameterInfo(
                name = p.name ?: "_",
                type = p.type.presentableText
            )
        }
        return MethodSignature(
            name = method.name,
            returnType = returnType,
            parameters = params,
            filePath = filePath
        )
    }

    private fun findSimilarMethods(javaFile: PsiJavaFile, targetClass: PsiClass): List<MethodSignature> {
        val packageName = javaFile.packageName
        if (packageName.isBlank()) return emptyList()

        val targetMethodNames = targetClass.methods.map { it.name }.toSet()
        if (targetMethodNames.isEmpty()) return emptyList()

        val facade = JavaPsiFacade.getInstance(project)
        val pkg = facade.findPackage(packageName) ?: return emptyList()

        val scope = GlobalSearchScope.projectScope(project)
        val siblingClasses = pkg.getClasses(scope)

        val results = mutableListOf<MethodSignature>()

        for (sibling in siblingClasses) {
            if (sibling.qualifiedName == targetClass.qualifiedName) continue

            val siblingPath = sibling.containingFile?.virtualFile?.path

            for (method in sibling.methods) {
                if (method.name in targetMethodNames) {
                    results += methodToSignature(method, siblingPath)
                }
            }

            for (method in sibling.methods) {
                if (method.name in targetMethodNames) continue
                if (targetMethodNames.any { sharesMeaningfulPrefix(it, method.name) }) {
                    results += methodToSignature(method, siblingPath)
                }
            }

            if (results.size >= 20) break
        }

        return results.take(20)
    }

    private fun sharesMeaningfulPrefix(a: String, b: String): Boolean {
        val prefixLen = a.zip(b).takeWhile { (x, y) -> x == y }.size
        return prefixLen >= 4
    }

    private fun analyzeNonJava(filePath: String, psiFile: com.intellij.psi.PsiFile): CodeAnalysis {
        val text = psiFile.text ?: return CodeAnalysis(targetFilePath = filePath)

        val imports = mutableListOf<String>()
        val methods = mutableListOf<MethodSignature>()
        val fields = mutableListOf<FieldInfo>()

        for (line in text.lines()) {
            val trimmed = line.trim()

            if (trimmed.startsWith("import ")) {
                imports += trimmed.removePrefix("import ").removeSuffix(";").trim()
                continue
            }

            val fieldMatch = KOTLIN_FIELD_REGEX.find(trimmed)
            if (fieldMatch != null) {
                fields += FieldInfo(
                    name = fieldMatch.groupValues[2],
                    type = fieldMatch.groupValues[3].ifBlank { "Unknown" }
                )
                continue
            }

            val funMatch = KOTLIN_FUN_REGEX.find(trimmed)
            if (funMatch != null) {
                val name = funMatch.groupValues[1]
                val rawParams = funMatch.groupValues[2]
                val returnType = funMatch.groupValues[3].ifBlank { "Unit" }

                val params = parseKotlinParams(rawParams)
                methods += MethodSignature(
                    name = name,
                    returnType = returnType,
                    parameters = params,
                    filePath = filePath
                )
            }
        }

        return CodeAnalysis(
            targetFilePath = filePath,
            imports = imports,
            methods = methods,
            fields = fields,
            similarMethods = emptyList()
        )
    }

    private fun parseKotlinParams(raw: String): List<ParameterInfo> {
        if (raw.isBlank()) return emptyList()
        return raw.split(",").mapNotNull { part ->
            val trimmed = part.trim()
            val colonIdx = trimmed.indexOf(':')
            if (colonIdx < 0) return@mapNotNull null
            val name = trimmed.substring(0, colonIdx).trim()
                .removePrefix("val ").removePrefix("var ").trim()
            val type = trimmed.substring(colonIdx + 1).trim()
                .removeSuffix(",").trim()
            if (name.isNotBlank() && type.isNotBlank()) {
                ParameterInfo(name = name, type = type)
            } else null
        }
    }

    companion object {
        private val KOTLIN_FIELD_REGEX = Regex(
            """^\s*(?:private|internal|protected|public)?\s*(val|var)\s+(\w+)\s*:\s*(\S+)"""
        )
        private val KOTLIN_FUN_REGEX = Regex(
            """^\s*(?:(?:private|internal|protected|public|override)\s+)*fun\s+(\w+)\s*\(([^)]*)\)\s*(?::\s*(\S+))?"""
        )
    }
}

