package com.markolukarami.aiplugin.frameworks.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.markolukarami.aiplugin.frameworks.ChatWiring
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

class AiToolWindowPanel(private val project: Project) {

    private val controller = ChatWiring.chatController()

    private val outputArea = JBTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
    }

    private val inputField = JBTextField()

    private val sendButton = JButton("Send").apply {
        addActionListener { onSend() }
    }

    val component: JComponent = JPanel(BorderLayout()).apply {
        val scroll = JBScrollPane(outputArea).apply {
            preferredSize = Dimension(380, 500)
        }
        add(scroll, BorderLayout.CENTER)

        val bottom = JPanel(BorderLayout(8, 8)).apply {
            add(inputField, BorderLayout.CENTER)
            add(sendButton, BorderLayout.EAST)
        }
        add(bottom, BorderLayout.SOUTH)
    }

    private fun onSend() {
        val text = inputField.text.trim()
        if (text.isBlank()) return

        inputField.text = ""
        sendButton.isEnabled = false

        append(text = "You: $text\n\n")

        object : Task.Backgroundable(project, "Asking AI", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Contacting LLM..."

                val viewModels = controller.onUserMessage(text)

                ApplicationManager.getApplication().invokeLater {
                    viewModels.drop(1).forEach { vm ->
                        append(vm.displayText + "\n")
                    }
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
