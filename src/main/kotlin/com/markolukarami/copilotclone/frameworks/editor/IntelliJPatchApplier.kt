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
                val factory = JavaPsiFacade.getElementFactory(project)

                var appliedOps = 0
                var missedOps = 0

                for (filePatch in patch.files) {
                    val rel = filePatch.relativePath.trim()
                    if (rel.isBlank()) continue
                    if (filePatch.operations.isEmpty()) continue

                    val targetIo = File(baseDir, rel).canonicalFile
                    if (!targetIo.path.startsWith(baseDir.path)) continue

                    val vFile = LocalFileSystem.getInstance().findFileByIoFile(targetIo) ?: continue
                    val psiFile = psiManager.findFile(vFile) ?: continue

                    FileDocumentManager.getInstance().getDocument(vFile)?.let { doc ->
                        docManager.commitDocument(doc)
                    }

                    for (op in filePatch.operations) {
                        val ok = applyOperation(factory, psiFile, vFile, op)
                        if (ok) appliedOps++ else missedOps++
                    }

                    try {
                        CodeStyleManager.getInstance(project).reformat(psiFile)
                    } catch (_: Throwable) {
                    }

                    docManager.commitAllDocuments()

                    FileDocumentManager.getInstance().getDocument(vFile)?.let { doc ->
                        FileDocumentManager.getInstance().saveDocument(doc)
                    }
                }

                println("PATCH RESULT: appliedOps=$appliedOps missedOps=$missedOps")
            }
    }

    private fun applyOperation(
        factory: PsiElementFactory,
        psiFile: PsiFile,
        vFile: VirtualFile,
        op: PatchOperation
    ): Boolean {
        return when (op.type) {

            "RENAME_METHOD" -> {
                val oldName = op.oldName?.trim().orEmpty()
                val newName = op.newName?.trim().orEmpty()
                if (oldName.isBlank() || newName.isBlank()) return false

                val m = PsiTargetResolver.findMethodByName(psiFile, oldName) ?: return false
                m.setName(newName)
                true
            }

            "REMOVE_METHOD_BODY" -> {
                val name = op.methodName?.trim().orEmpty()
                if (name.isBlank()) return false

                val m = PsiTargetResolver.findMethodByName(psiFile, name) ?: return false
                val body = m.body ?: return false
                val empty = factory.createCodeBlockFromText("{\n}", m)
                body.replace(empty)
                true
            }

            "DELETE_METHOD" -> {
                val name = op.methodName?.trim().orEmpty()
                if (name.isBlank()) return false

                val m = PsiTargetResolver.findMethodByName(psiFile, name) ?: return false
                m.delete()
                true
            }

            "CREATE_METHOD" -> {
                val src = listOf(
                    op.methodSource,
                    op.newSource,
                    op.newContent
                ).firstOrNull { !it.isNullOrBlank() }?.trim().orEmpty()

                if (src.isBlank()) {
                    println("CREATE_METHOD missing method source (methodSource/newSource/newContent all blank)")
                    return false
                }

                val cls = PsiTargetResolver.findFirstClass(psiFile) ?: return false

                val newMethod = try {
                    factory.createMethodFromText(src, cls)
                } catch (t: Throwable) {
                    println("CREATE_METHOD parse failed: ${t.message}")
                    return false
                }

                cls.add(newMethod)
                true
            }

            "REWRITE_METHOD" -> {
                val name = op.methodName?.trim().orEmpty()
                val src = op.newSource?.trim().orEmpty()
                if (name.isBlank() || src.isBlank()) return false

                val oldMethod = PsiTargetResolver.findMethodByName(psiFile, name) ?: return false
                val cls = PsiTargetResolver.findFirstClass(psiFile) ?: return false

                val newMethod = try {
                    factory.createMethodFromText(src, cls)
                } catch (t: Throwable) {
                    println("REWRITE_METHOD parse failed: ${t.message}")
                    return false
                }

                oldMethod.replace(newMethod)
                true
            }

            "INSERT_STATEMENT" -> {
                val methodName = op.methodName?.trim().orEmpty()
                val position = op.position?.trim().orEmpty()
                val stmtText = op.statement?.trim().orEmpty()
                if (methodName.isBlank() || position.isBlank() || stmtText.isBlank()) return false

                val m = PsiTargetResolver.findMethodByName(psiFile, methodName) ?: return false
                val body = m.body ?: return false

                val stmt = try {
                    factory.createStatementFromText(stmtText, m)
                } catch (t: Throwable) {
                    println("INSERT_STATEMENT bad statement: ${t.message}")
                    return false
                }

                when (position) {
                    "START" -> {
                        val first = body.statements.firstOrNull()
                        if (first != null) body.addBefore(stmt, first) else body.add(stmt)
                        true
                    }

                    "END" -> {
                        val rBrace = body.rBrace ?: return false
                        body.addBefore(stmt, rBrace)
                        true
                    }

                    "AFTER_TEXT" -> {
                        val needle = op.afterText?.trim().orEmpty()
                        if (needle.isBlank()) return false

                        val anchor = body.statements.firstOrNull { it.text.contains(needle) }
                            ?: return false

                        body.addAfter(stmt, anchor)
                        true
                    }

                    else -> false
                }
            }

            "TEXT_REPLACE" -> {
                val search = op.search ?: return false
                val replace = op.replace ?: ""
                val doc = FileDocumentManager.getInstance().getDocument(vFile) ?: return false
                val idx = doc.text.indexOf(search)
                if (idx == -1) return false
                doc.replaceString(idx, idx + search.length, replace)
                true
            }

            "REWRITE_FILE" -> {
                val newContent = op.newContent ?: return false
                val doc = FileDocumentManager.getInstance().getDocument(vFile) ?: return false
                doc.setText(newContent)
                true
            }

            else -> false
        }
    }
}

