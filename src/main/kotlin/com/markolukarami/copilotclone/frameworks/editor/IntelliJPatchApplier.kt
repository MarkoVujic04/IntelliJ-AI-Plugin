package com.markolukarami.copilotclone.frameworks.editor

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiDocumentManager
import com.markolukarami.copilotclone.domain.entities.patch.PatchPlan
import com.markolukarami.copilotclone.domain.repositories.PatchApplierRepository
import java.io.File

@Service(Service.Level.PROJECT)
class IntelliJPatchApplier(private val project: Project) : PatchApplierRepository {

    override fun apply(patch: PatchPlan) {
        val basePath = project.basePath ?: return
        val baseDirectory = File(basePath).canonicalFile

        WriteCommandAction.writeCommandAction(project)
            .withName("AI Apply Patch")
            .run<RuntimeException> {

                for (filePatch in patch.files) {
                    val target = File(baseDirectory, filePatch.relativePath).canonicalFile

                    if (!target.path.startsWith(baseDirectory.path)) {
                        println("Patch blocked (outside project): ${target.path}")
                        continue
                    }

                    val vFile = LocalFileSystem.getInstance().findFileByIoFile(target)
                    if (vFile == null) {
                        println("File not found in VFS: ${target.path}")
                        continue
                    }

                    val document = FileDocumentManager.getInstance().getDocument(vFile)
                    if (document == null) {
                        println("No document for file: ${target.path}")
                        continue
                    }

                    println("---- APPLY PATCH FILE ----")
                    println("Target: ${target.path}")
                    println("Edits: ${filePatch.edits.size}")

                    for (edit in filePatch.edits) {
                        val search = edit.search
                        val replace = edit.replace

                        if (search.isBlank()) {
                            println("Skipped edit with blank search.")
                            continue
                        }

                        val currentText = document.text
                        val index = currentText.indexOf(search)

                        println("Edit search='${short(search)}' index=$index")

                        if (index == -1) {
                            println("Search string not found. Skipping.")
                            continue
                        }

                        document.replaceString(index, index + search.length, replace)
                        println("Applied replacement at [$index, ${index + search.length})")
                    }

                    PsiDocumentManager.getInstance(project).commitDocument(document)
                    FileDocumentManager.getInstance().saveDocument(document)
                    println("---- DONE FILE ----")
                }
            }
    }

    private fun short(s: String, max: Int = 60): String =
        if (s.length <= max) s else s.take(max) + "…"
}
