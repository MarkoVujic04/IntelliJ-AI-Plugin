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

                if(!target.path.startsWith(baseDirectory.path)) continue

                val vFile = LocalFileSystem.getInstance().findFileByIoFile(target) ?: continue
                val document = FileDocumentManager.getInstance().getDocument(vFile) ?: continue

                val edits = filePatch.edits.sortedByDescending { it.start }
                for (edit in edits) {
                    val start = edit.start.coerceAtLeast(0).coerceAtMost(document.textLength)
                    val end = edit.end.coerceAtLeast(0).coerceAtMost(document.textLength)

                    if(end < start) continue
                    document.replaceString(start, end, edit.text)
                }

                PsiDocumentManager.getInstance(project).commitDocument(document)
                FileDocumentManager.getInstance().saveDocument(document)
            }
        }
    }

}