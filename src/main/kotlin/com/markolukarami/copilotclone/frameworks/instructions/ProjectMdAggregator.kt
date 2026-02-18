package com.markolukarami.copilotclone.frameworks.instructions

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import java.nio.charset.Charset

object ProjectMdAggregator {
    fun buildAgentsMdFromAllMarkdown(project: Project) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Building AGENTS.md from Markdown files", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = false
                indicator.text = "Scanning project..."

                val basePath = project.basePath ?: return
                val root = LocalFileSystem.getInstance().findFileByPath(basePath) ?: return

                val mdFiles = ReadAction.compute<List<VirtualFile>, Throwable> {
                    collectMarkdownFiles(root)
                }

                if (mdFiles.isEmpty()) return

                indicator.text = "Reading Markdown files..."
                val combined = StringBuilder()
                combined.append("# AGENTS.md\n\n")
                combined.append("<!-- Auto-generated. You can edit this file manually. -->\n\n")

                mdFiles.forEachIndexed { idx, vf ->
                    if (indicator.isCanceled) return
                    indicator.fraction = (idx.toDouble() / mdFiles.size.toDouble()).coerceIn(0.0, 1.0)
                    indicator.text2 = vf.path

                    val content = safeRead(vf)
                    if (content.isBlank()) return@forEachIndexed

                    val rel = VfsUtilCore.getRelativePath(vf, root) ?: vf.name
                    combined.append("\n---\n\n")
                    combined.append("## ").append(rel).append("\n\n")
                    combined.append(content.trim()).append("\n")
                }

                indicator.text = "Writing AGENTS.md..."

                WriteAction.run<Throwable> {
                    val agentsFile = root.findOrCreateChildData(this, "AGENTS.md")
                    val doc = FileDocumentManager.getInstance().getDocument(agentsFile)
                    if (doc != null) {
                        doc.setText(combined.toString())
                        PsiDocumentManager.getInstance(project).commitDocument(doc)
                        FileDocumentManager.getInstance().saveDocument(doc)
                    } else {
                        agentsFile.setBinaryContent(combined.toString().toByteArray(Charsets.UTF_8))
                    }
                }
            }
        })
    }

    private fun collectMarkdownFiles(root: VirtualFile): List<VirtualFile> {
        val out = mutableListOf<VirtualFile>()
        VfsUtilCore.iterateChildrenRecursively(root, null) { vf ->
            if (vf.isDirectory) return@iterateChildrenRecursively true
            val name = vf.name.lowercase()
            if (!name.endsWith(".md")) return@iterateChildrenRecursively true
            if (name == "agents.md") return@iterateChildrenRecursively true
            out += vf
            true
        }
        return out
    }

    private fun safeRead(vf: VirtualFile): String {
        return try {
            val bytes = vf.contentsToByteArray()
            String(bytes, Charsets.UTF_8)
        } catch (_: Throwable) {
            ""
        }
    }
}