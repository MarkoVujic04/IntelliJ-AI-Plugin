package com.markolukarami.copilotclone.frameworks.editor

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.codeStyle.CodeStyleManager
import com.markolukarami.copilotclone.domain.entities.patch.PatchOperation
import com.markolukarami.copilotclone.domain.entities.patch.PatchPlan
import com.markolukarami.copilotclone.domain.repositories.PatchApplierRepository
import java.io.File

@Service(Service.Level.PROJECT)
class IntelliJPatchApplier(private val project: Project) : PatchApplierRepository {

    override fun apply(patch: PatchPlan) {
        val basePath = project.basePath ?: return
        val baseDir = File(basePath).canonicalFile

        WriteCommandAction.writeCommandAction(project)
            .withName("AI Apply Patch")
            .run<RuntimeException> {

                val psiManager = PsiManager.getInstance(project)
                val docManager = PsiDocumentManager.getInstance(project)

                var appliedOps = 0
                var missedOps = 0

                for (filePatch in patch.files) {
                    val target = File(baseDir, filePatch.relativePath).canonicalFile
                    if (!target.path.startsWith(baseDir.path)) continue

                    val vFile = LocalFileSystem.getInstance().findFileByIoFile(target) ?: continue
                    val psiFile = psiManager.findFile(vFile) ?: continue

                    val document = FileDocumentManager.getInstance().getDocument(vFile)
                    if (document != null) docManager.commitDocument(document)

                    for (op in filePatch.operations) {
                        val ok = applyOperation(psiFile, vFile, op)
                        if (ok) appliedOps++ else missedOps++
                    }

                    CodeStyleManager.getInstance(project).reformat(psiFile)
                    docManager.commitAllDocuments()

                    val docToSave = FileDocumentManager.getInstance().getDocument(vFile)
                    if (docToSave != null) FileDocumentManager.getInstance().saveDocument(docToSave)
                }

                println("PATCH RESULT: appliedOps=$appliedOps missedOps=$missedOps")
            }
    }

    private fun applyOperation(psiFile: PsiFile, vFile: VirtualFile, op: PatchOperation): Boolean {
        return when (op.type) {
            "RENAME_METHOD" -> renameMethod(psiFile, op)
            "REMOVE_METHOD_BODY" -> removeMethodBody(psiFile, op)
            "DELETE_METHOD" -> deleteMethod(psiFile, op)
            "CREATE_METHOD" -> createMethod(psiFile, op)
            "TEXT_REPLACE" -> textReplace(vFile, op)
            "REWRITE_FILE" -> rewriteFile(vFile, op)
            else -> false
        }
    }

    private fun renameMethod(psiFile: PsiFile, op: PatchOperation): Boolean {
        val oldName = op.oldName?.trim().orEmpty()
        val newName = op.newName?.trim().orEmpty()
        if (oldName.isBlank() || newName.isBlank()) return false

        val cls = findFirstJavaClass(psiFile) ?: return false
        val method = cls.methods.firstOrNull { it.name == oldName } ?: return false
        method.setName(newName)
        return true
    }

    private fun removeMethodBody(psiFile: PsiFile, op: PatchOperation): Boolean {
        val name = op.methodName?.trim().orEmpty()
        if (name.isBlank()) return false

        val cls = findFirstJavaClass(psiFile) ?: return false
        val method = cls.methods.firstOrNull { it.name == name } ?: return false

        val factory = JavaPsiFacade.getElementFactory(project)
        val emptyBody = factory.createCodeBlockFromText("{\n}", method)

        val body = method.body ?: return false
        body.replace(emptyBody)
        return true
    }

    private fun deleteMethod(psiFile: PsiFile, op: PatchOperation): Boolean {
        val name = op.methodName?.trim().orEmpty()
        if (name.isBlank()) return false

        val cls = findFirstJavaClass(psiFile) ?: return false
        val method = cls.methods.firstOrNull { it.name == name } ?: return false
        method.delete()
        return true
    }

    private fun createMethod(psiFile: PsiFile, op: PatchOperation): Boolean {
        val src = op.methodSource?.trim().orEmpty()
        if (src.isBlank()) return false

        val cls = findFirstJavaClass(psiFile) ?: return false
        val factory = JavaPsiFacade.getElementFactory(project)

        val newMethod = try {
            factory.createMethodFromText(src, cls)
        } catch (t: Throwable) {
            println("CREATE_METHOD failed: ${t.message}")
            return false
        }

        cls.add(newMethod)
        return true
    }

    private fun findFirstJavaClass(psiFile: PsiFile): PsiClass? {
        return when (psiFile) {
            is PsiJavaFile -> psiFile.classes.firstOrNull()
            else -> psiFile.children.filterIsInstance<PsiClass>().firstOrNull()
        }
    }

    private fun textReplace(vFile: VirtualFile, op: PatchOperation): Boolean {
        val search = op.search ?: return false
        val replace = op.replace ?: ""

        val document = FileDocumentManager.getInstance().getDocument(vFile) ?: return false
        val idx = document.text.indexOf(search)
        if (idx == -1) {
            println("TEXT_REPLACE MISS: " + short(search))
            return false
        }

        document.replaceString(idx, idx + search.length, replace)
        return true
    }

    private fun rewriteFile(vFile: VirtualFile, op: PatchOperation): Boolean {
        val newContent = op.newContent ?: return false
        val document = FileDocumentManager.getInstance().getDocument(vFile) ?: return false
        document.setText(newContent)
        return true
    }

    private fun short(s: String, max: Int = 80): String =
        if (s.length <= max) s else s.take(max) + "…"
}
