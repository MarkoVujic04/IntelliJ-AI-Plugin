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
                        continue
                    }

                    val vFile = LocalFileSystem.getInstance().findFileByIoFile(target)
                    if (vFile == null) {
                        continue
                    }

                    val document = FileDocumentManager.getInstance().getDocument(vFile)
                    if (document == null) {
                        continue
                    }

                    for (edit in filePatch.edits) {
                        val search = edit.search
                        val replace = edit.replace

                        if (search.isBlank()) {
                            continue
                        }

                        val currentText = document.text
                        val index = currentText.indexOf(search)


                        if (index == -1) {
                            continue
                        }

                        document.replaceString(index, index + search.length, replace)
                    }

                    PsiDocumentManager.getInstance(project).commitDocument(document)
                    FileDocumentManager.getInstance().saveDocument(document)
                }
            }
    }

    private fun short(s: String, max: Int = 60): String =
        if (s.length <= max) s else s.take(max) + "…"
}
