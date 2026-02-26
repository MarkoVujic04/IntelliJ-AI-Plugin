package com.markolukarami.copilotclone.ui.components

import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.ui.EditorTextField
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import java.awt.Dimension
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.border.EmptyBorder

class ComposerPanel(
    project: Project,
    private val onSend: (String) -> Unit,
    private val inlineActionsBar: InlineActionsBar,
    private val contextChipsPanel: ContextChipsPanel
) {

    private val inputDocument: Document = EditorFactory.getInstance().createDocument("")
    val inputField = EditorTextField(
        inputDocument,
        project,
        PlainTextFileType.INSTANCE
    ).apply {
        setOneLineMode(false)
        setPlaceholder("Ask AI Assistant… Use # or @ for mentions and / for commands")

        preferredSize = Dimension(0, 110)
        minimumSize = Dimension(0, 110)

        addSettingsProvider { editor ->
            editor.settings.isUseSoftWraps = true
            editor.settings.isCaretRowShown = false
            editor.settings.isRightMarginShown = false
            editor.settings.isLineNumbersShown = false
            editor.settings.isFoldingOutlineShown = false
            editor.setVerticalScrollbarVisible(true)
            editor.setHorizontalScrollbarVisible(false)
        }
    }

    private val sendButton = JButton(AllIcons.Actions.Execute).apply {
        isFocusable = false
        toolTipText = "Send"
        putClientProperty("JButton.buttonType", "toolbutton")
        addActionListener { handleSend() }
    }

    val component: JComponent = buildComposer()

    fun getSendButton(): JButton = sendButton

    private fun buildComposer(): JComponent {
        val composer = BorderLayoutPanel().apply {
            isOpaque = true
            background = JBColor.PanelBackground
            border = JBUI.Borders.compound(
                JBUI.Borders.customLine(JBColor.border(), 1),
                JBUI.Borders.empty(8)
            )
        }

        val chipsRow = BorderLayoutPanel().apply {
            isOpaque = false
            border = JBUI.Borders.emptyBottom(6)
            addToCenter(contextChipsPanel.component.apply {
                isOpaque = false
            })
        }

        inputField.border = JBUI.Borders.empty()
        inputField.isOpaque = false

        val editorRow = BorderLayoutPanel().apply {
            isOpaque = false
            addToCenter(inputField)
        }

        val bottomRow = BorderLayoutPanel().apply {
            isOpaque = false
            border = JBUI.Borders.emptyTop(6)
            addToLeft(inlineActionsBar.component)
            addToRight(sendButton)
        }

        composer.addToTop(chipsRow)
        composer.addToCenter(editorRow)
        composer.addToBottom(bottomRow)

        return composer
    }

    private fun handleSend() {
        val text = inputField.text.trim()
        if (text.isNotBlank()) {
            onSend(text)
            inputField.text = ""
        }
    }

    fun refreshContextChips() {
        contextChipsPanel.refresh()
    }

    fun setSendButtonEnabled(enabled: Boolean) {
        sendButton.isEnabled = enabled
    }
}


