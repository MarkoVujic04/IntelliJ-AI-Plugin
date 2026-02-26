package com.markolukarami.copilotclone.ui.components

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.markolukarami.copilotclone.ui.PromptLibraryPopup
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

class InlineActionsBar(
    private val project: Project,
    private val onAddContext: () -> Unit,
    private val currentInputGetter: () -> String,
    private val currentInputSetter: (String) -> Unit,
    private val onContextFilesUpdated: () -> Unit
) {

    var onPickModel: ((JComponent) -> Unit)? = null
    val component: JComponent = createActions()

    private fun createActions(): JComponent {
        val plus = JButton(AllIcons.General.Add).apply {
            isFocusable = false
            toolTipText = "Add context"
            putClientProperty("JButton.buttonType", "toolbutton")
            addActionListener { onAddContext() }
        }

        val bookmark = JButton(BookmarkIcons.BOOKMARK).apply {
            isFocusable = false
            toolTipText = "Saved prompts"
            putClientProperty("JButton.buttonType", "toolbutton")
            addActionListener {
                PromptLibraryPopup.show(
                    project = project,
                    anchor = this,
                    currentInput = currentInputGetter,
                    onPick = { picked -> currentInputSetter(picked) }
                )
            }
        }

        val modelButton = JButton(AllIcons.General.Settings).apply {
            isFocusable = false
            toolTipText = "Select model"
            putClientProperty("JButton.buttonType", "toolbutton")
            addActionListener { onPickModel?.invoke(this) }
        }

        return JPanel(FlowLayout(FlowLayout.LEFT, com.intellij.util.ui.JBUI.scale(6), 0)).apply {
            isOpaque = false
            add(plus)
            add(bookmark)
            add(modelButton)
        }
    }
}



