package com.markolukarami.copilotclone.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.markolukarami.copilotclone.domain.entities.context.ContextFile
import com.markolukarami.copilotclone.frameworks.editor.UserContextState
import com.markolukarami.copilotclone.frameworks.llm.ChatWiring
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTabbedPane
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.ui.EditorTextField
import com.intellij.util.ui.JBUI
import com.markolukarami.copilotclone.ui.PromptLibraryPopup
import com.markolukarami.copilotclone.ui.components.BookmarkIcons
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.OverlayLayout
import javax.swing.border.EmptyBorder

class AiToolWindowPanel(private val project: Project) {

    private val controller = ChatWiring.chatController(project)

    private val userContextState = project.service<UserContextState>()

    private val outputArea = JBTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
    }

    private val inputDocument: Document = EditorFactory.getInstance().createDocument("")
    private val inputField = EditorTextField(
        inputDocument,
        project,
        PlainTextFileType.INSTANCE
    ).apply {
        setOneLineMode(false)
        setPlaceholder("Ask AI Assistant…")

        preferredSize = Dimension(0, 120)
        minimumSize = Dimension(0, 120)
    }

    private val bookmarkButton = JButton(BookmarkIcons.BOOKMARK).apply {
        isFocusable = false
        toolTipText = "Saved prompts"
        addActionListener {
            PromptLibraryPopup.show(
                project = project,
                anchor = this,
                currentInput = { inputField.text },
                onPick = { picked ->
                    inputField.text = picked
                }
            )
        }
    }

    private val sendButton = JButton("Send").apply { addActionListener { onSend() } }

    private val contextButton = JButton("Context").apply {
        isFocusable = false
        toolTipText = "Choose file(s) to include as context"
        addActionListener { onPickContextFiles() }
    }

    private val tracePanel = TracePanel(project)

    val component: JComponent = JPanel(BorderLayout()).apply {
        val chatView = JPanel(BorderLayout()).apply {
            val scroll = JBScrollPane(outputArea).apply {
                preferredSize = Dimension(380, 500)
            }
            add(scroll, BorderLayout.CENTER)

            val bottom = JPanel(BorderLayout(8, 8)).apply {
                add(buildInputWithOverlayButtons(), BorderLayout.CENTER)
                add(sendButton, BorderLayout.EAST)
            }

            add(bottom, BorderLayout.SOUTH)
        }

        val tabs = JTabbedPane().apply {
            addTab("Chat", chatView)
            addTab("Trace", tracePanel.component)
        }

        add(tabs, BorderLayout.CENTER)
    }

    private fun onPickContextFiles() {
        val manager = ContextManagerDialog(project) {
            val files = userContextState.getSelectedContextFiles().map { it.path }
            append("Context files now (${files.size}):\n")
            files.forEach { append("- $it\n") }
            append("\n")
        }

        object : DialogWrapper(project, true) {
            init {
                title = "Context"
                init()
            }

            override fun createCenterPanel(): JComponent = manager.component
        }.show()
    }

    private fun onSend() {
        val text = inputField.text.trim()
        if (text.isBlank()) return

        inputField.text = ""
        sendButton.isEnabled = false

        append("You: $text\n\n")
        append("AI is thinking...\n\n")

        object : Task.Backgroundable(project, "AI Chat", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Contacting LLM..."
                val result = controller.onUserMessage(text)

                ApplicationManager.getApplication().invokeLater {
                    result.chatItems.drop(1).forEach { vm -> append(vm.displayText + "\n\n") }
                    tracePanel.setTraceLines(result.trace.lines)
                    sendButton.isEnabled = true
                }
            }

            override fun onThrowable(error: Throwable) {
                ApplicationManager.getApplication().invokeLater {
                    append("Error: ${error.message ?: "Unknown error"}\n\n")
                    sendButton.isEnabled = true
                }
            }
        }.queue()
    }

    private fun append(text: String) {
        outputArea.append(text)
        outputArea.caretPosition = outputArea.document.length
    }

    private fun buildInputWithOverlayButtons(): JComponent {
        val container = JPanel().apply {
            layout = OverlayLayout(this)
            border = JBUI.Borders.empty()
            preferredSize = Dimension(0, 120)
            minimumSize = Dimension(0, 120)
        }

        inputField.alignmentX = 0f
        inputField.alignmentY = 0f

        val overlayRow = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            isOpaque = false
            border = EmptyBorder(0, 8, 8, 0)

            bookmarkButton.putClientProperty("JButton.buttonType", "toolbutton")
            contextButton.putClientProperty("JButton.buttonType", "toolbutton")

            bookmarkButton.margin = JBUI.insets(2, 6)
            contextButton.margin = JBUI.insets(2, 10)

            add(bookmarkButton)
            add(Box.createHorizontalStrut(6))
            add(contextButton)
        }

        overlayRow.alignmentX = 0f
        overlayRow.alignmentY = 1f

        container.add(overlayRow)
        container.add(inputField)

        return container
    }
}