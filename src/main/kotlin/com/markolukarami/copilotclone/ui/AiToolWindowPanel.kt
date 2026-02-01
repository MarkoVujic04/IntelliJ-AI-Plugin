package com.markolukarami.copilotclone.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.markolukarami.copilotclone.domain.entities.ContextFile
import com.markolukarami.copilotclone.frameworks.editor.UserContextState
import com.markolukarami.copilotclone.frameworks.llm.ChatWiring
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTabbedPane
import com.intellij.openapi.ui.DialogWrapper

class AiToolWindowPanel(private val project: Project) {

    private val controller = ChatWiring.chatController(project)

    private val userContextState = project.service<UserContextState>()

    private val outputArea = JBTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
    }

    private val inputField = JBTextField()
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
                add(inputField, BorderLayout.CENTER)

                val rightButtons = JPanel().apply {
                    add(contextButton)
                    add(sendButton)
                }
                add(rightButtons, BorderLayout.EAST)
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
}