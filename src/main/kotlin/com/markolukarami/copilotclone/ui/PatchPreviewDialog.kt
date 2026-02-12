package com.markolukarami.copilotclone.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import com.markolukarami.copilotclone.domain.entities.patch.PatchPlan
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JPanel

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
        val area = JBTextArea().apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
            text = buildPreview(patch)
            border = JBUI.Borders.empty(8)
        }

        val scroll = JBScrollPane(area).apply {
            preferredSize = Dimension(720, 420)
        }

        return JPanel(BorderLayout()).apply {
            add(scroll, BorderLayout.CENTER)
        }
    }

    private fun buildPreview(p: PatchPlan): String {
        val sb = StringBuilder()
        sb.append("Summary:\n")
        sb.append(p.summary).append("\n\n")
        sb.append("Files:\n")
        p.files.forEach { fp ->
            sb.append("- ").append(fp.relativePath).append(" (edits=").append(fp.edits.size).append(")\n")
        }
        sb.append("\nClick 'OK' to apply. You can undo with Ctrl+Z.\n")
        return sb.toString()
    }
}