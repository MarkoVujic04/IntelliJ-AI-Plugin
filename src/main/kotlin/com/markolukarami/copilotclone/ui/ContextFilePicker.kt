package com.markolukarami.copilotclone.ui

import com.intellij.ide.util.TreeFileChooserFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile

object ContextFilePicker {

    fun pickSingleProjectFile(project: Project): VirtualFile? {
        val chooser = TreeFileChooserFactory.getInstance(project)
            .createFileChooser(
                "Select context file",
                null,
                null,
                null
            )

        chooser.showDialog()

        val psi: PsiFile = chooser.selectedFile ?: return null
        return psi.virtualFile
    }
}