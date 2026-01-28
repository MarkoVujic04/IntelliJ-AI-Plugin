package com.markolukarami.copilotclone.frameworks.editor
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.openapi.vfs.VfsUtilCore
import com.markolukarami.copilotclone.domain.entities.TextSnippet
import com.markolukarami.copilotclone.domain.repositories.TextSearchRepository
import java.nio.charset.StandardCharsets
import kotlin.math.max
import kotlin.math.min

class IntelliJTextSearchAdapter(
    private val project: Project
) : TextSearchRepository {

    override fun search(query: String, limit: Int): List<TextSnippet> {
        val q = query.trim()
        if (q.isBlank() || limit <= 0) return emptyList()

        return ReadAction.compute<List<TextSnippet>, RuntimeException> {
            val scope = GlobalSearchScope.projectScope(project)

            val files: List<VirtualFile> =
                FilenameIndex.getAllFilesByExt(project, "kt", scope).toList() +
                        FilenameIndex.getAllFilesByExt(project, "java", scope).toList() +
                        FilenameIndex.getAllFilesByExt(project, "md", scope).toList() +
                        FilenameIndex.getAllFilesByExt(project, "txt", scope).toList() +
                        FilenameIndex.getAllFilesByExt(project, "xml", scope).toList() +
                        FilenameIndex.getAllFilesByExt(project, "yml", scope).toList() +
                        FilenameIndex.getAllFilesByExt(project, "yaml", scope).toList()

            val results = ArrayList<TextSnippet>(limit)

            for (file in files) {
                if (results.size >= limit) break
                if (file.isDirectory) continue

                val text = runCatching {
                    val bytes = VfsUtilCore.loadBytes(file)
                    String(bytes, StandardCharsets.UTF_8)
                }.getOrNull() ?: continue

                val idx = text.indexOf(q, ignoreCase = true)
                if (idx < 0) continue

                val lineNumber = text.substring(0, idx).count { it == '\n' } + 1

                val start = max(0, idx - 80)
                val end = min(text.length, idx + 120)
                val preview = text.substring(start, end)
                    .replace("\n", " ")
                    .replace("\r", " ")
                    .trim()

                results += TextSnippet(
                    filePath = file.path,
                    lineNumber = lineNumber,
                    preview = preview
                )
            }
            results
        }
    }
}
