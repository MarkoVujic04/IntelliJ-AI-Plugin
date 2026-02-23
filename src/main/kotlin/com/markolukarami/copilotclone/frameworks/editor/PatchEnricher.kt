package com.markolukarami.copilotclone.frameworks.editor

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiFile
import com.markolukarami.copilotclone.domain.entities.patch.FilePatch
import com.markolukarami.copilotclone.domain.entities.patch.PatchOperation
import com.markolukarami.copilotclone.domain.entities.patch.PatchPlan
import java.io.File

/**
 * Enriches PatchPlan with actual code content from files for display in the preview dialog.
 * Extracts method source code for DELETE_METHOD and other operations.
 */
class PatchEnricher(private val project: Project) {

    fun enrichPatch(patch: PatchPlan): PatchPlan {
        val enrichedFiles = patch.files.map { filePatch ->
            enrichFilePatch(filePatch)
        }
        return patch.copy(files = enrichedFiles)
    }

    private fun enrichFilePatch(filePatch: FilePatch): FilePatch {
        val basePath = project.basePath ?: return filePatch
        val fullPath = "$basePath/${filePatch.relativePath}"
        val file = File(fullPath)

        if (!file.exists()) return filePatch

        val enrichedOps = filePatch.operations.map { operation ->
            enrichOperation(operation, file)
        }

        return filePatch.copy(operations = enrichedOps)
    }

    private fun enrichOperation(operation: PatchOperation, file: File): PatchOperation {
        return when (operation.type) {
            "DELETE_METHOD" -> enrichDeleteMethod(operation, file)
            "REWRITE_METHOD" -> enrichRewriteMethod(operation, file)
            "REMOVE_METHOD_BODY" -> enrichRemoveMethodBody(operation, file)
            "CREATE_METHOD" -> operation  // CREATE_METHOD already has the source in methodSource or newSource
            else -> operation
        }
    }

    private fun enrichDeleteMethod(operation: PatchOperation, file: File): PatchOperation {
        val methodName = operation.methodName ?: return operation

        // If methodSource is already present, don't re-extract
        if (!operation.methodSource.isNullOrBlank()) return operation

        val methodSource = extractMethodByName(file, methodName)
        return if (methodSource != null) {
            operation.copy(methodSource = methodSource)
        } else {
            operation
        }
    }

    private fun enrichRewriteMethod(operation: PatchOperation, file: File): PatchOperation {
        val methodName = operation.methodName ?: return operation

        // If oldSource is not present, extract it to show what's being replaced
        if (operation.methodSource.isNullOrBlank()) {
            val methodSource = extractMethodByName(file, methodName)
            if (methodSource != null) {
                return operation.copy(methodSource = methodSource)
            }
        }

        return operation
    }

    private fun enrichRemoveMethodBody(operation: PatchOperation, file: File): PatchOperation {
        val methodName = operation.methodName ?: return operation

        if (operation.methodSource.isNullOrBlank()) {
            val methodSource = extractMethodByName(file, methodName)
            if (methodSource != null) {
                return operation.copy(methodSource = methodSource)
            }
        }

        return operation
    }

    private fun extractMethodByName(file: File, methodName: String): String? {
        return try {
            val content = file.readText()
            extractJavaMethod(content, methodName)
        } catch (e: Exception) {
            null
        }
    }

    private fun extractJavaMethod(content: String, methodName: String): String? {
        // More robust extraction that properly handles nested braces
        val methodPattern = """(?:public|private|protected)?\s*(?:static)?\s*(?:final)?\s*(?:\w+(?:<[^>]+>)?)\s+$methodName\s*\([^)]*\)\s*\{""".toRegex()

        val match = methodPattern.find(content) ?: return null

        val startIdx = match.range.first
        var braceCount = 0
        var idx = startIdx + match.value.length - 1

        // Find the opening brace
        while (idx < content.length && content[idx] != '{') {
            idx++
        }

        if (idx >= content.length) return null

        braceCount = 1
        idx++

        // Find the matching closing brace
        while (idx < content.length && braceCount > 0) {
            when (content[idx]) {
                '{' -> braceCount++
                '}' -> braceCount--
            }
            idx++
        }

        return if (braceCount == 0) {
            content.substring(startIdx, idx).trim()
        } else {
            null
        }
    }
}


