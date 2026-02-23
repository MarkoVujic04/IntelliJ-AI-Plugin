package com.markolukarami.copilotclone.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.markolukarami.copilotclone.domain.entities.patch.PatchPlan
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextPane

class PatchPreviewDialog(
    project: Project,
    private val patch: PatchPlan,
    private val onApply: () -> Unit
): DialogWrapper(project, true) {

    init {
        title = "AI Apply Patch"
        init()
    }

    override fun createCenterPanel(): JComponent? {
        val pane = JTextPane().apply {
            isEditable = false
            document = DiffRenderer.createStyledDocument(patch.summary, patch.files)
            border = JBUI.Borders.empty(8)
        }

        val scroll = JBScrollPane(pane).apply {
            preferredSize = Dimension(800, 500)
        }

        return JPanel(BorderLayout()).apply {
            add(scroll, BorderLayout.CENTER)
        }
    }

    override fun doOKAction() {
        onApply()
        super.doOKAction()
    }
}