package com.markolukarami.copilotclone.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import com.markolukarami.copilotclone.frameworks.chat.ChatSessionState
import com.markolukarami.copilotclone.frameworks.editor.IntelliJPatchApplier
import com.markolukarami.copilotclone.frameworks.editor.PatchEnricher
import com.markolukarami.copilotclone.frameworks.editor.UserContextState
import com.markolukarami.copilotclone.frameworks.llm.ChatWiring
import com.markolukarami.copilotclone.frameworks.llm.LMStudioModelRegistryAdapter
import com.markolukarami.copilotclone.frameworks.settings.AiSettingsState
import com.markolukarami.copilotclone.ui.components.ComposerPanel
import com.markolukarami.copilotclone.ui.components.ContextChipsPanel
import com.markolukarami.copilotclone.ui.components.InlineActionsBar
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTabbedPane

class AiToolWindowPanel(private val project: Project) {

    private val controller = ChatWiring.chatController(project)
    private val userContextState = project.service<UserContextState>()
    private val settingsState = service<AiSettingsState>()
    private val modelRegistry = LMStudioModelRegistryAdapter()
    private val chatSessions = project.service<ChatSessionState>()

    private val outputArea = JBTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
    }

    private val tracePanel = TracePanel(project)
    private val instructionsPanel = InstructionsPanel()

    private val contextChipsPanel = ContextChipsPanel(
        userContextState,
        onRemoveFile = { filePath ->
            append("Removed from context: $filePath\n\n")
        }
    )

    private lateinit var composerPanel: ComposerPanel
    private lateinit var inlineActionsBar: InlineActionsBar

    val component: JComponent = buildMainUI()

    private fun buildMainUI(): JComponent {
        val chatView = JPanel(BorderLayout()).apply {
            val scroll = JBScrollPane(outputArea).apply {
                preferredSize = Dimension(380, 500)
            }
            add(scroll, BorderLayout.CENTER)

            val bottom = JPanel(BorderLayout()).apply {
                border = JBUI.Borders.empty(8)
                add(buildComposerView(), BorderLayout.CENTER)
            }

            add(bottom, BorderLayout.SOUTH)
        }

        val tabs = JTabbedPane().apply {
            addTab("Chat", chatView)
            addTab("Trace", tracePanel.component)
            addTab("Instructions", instructionsPanel)
        }

        val newChatButton = JButton("New Chat").apply {
            isFocusable = false
            putClientProperty("JButton.buttonType", "toolbutton")
            addActionListener { onNewChat() }
        }

        val chatHistoryButton = JButton("Chat History").apply {
            isFocusable = false
            putClientProperty("JButton.buttonType", "toolbutton")
            addActionListener { onOpenChatHistory() }
        }

        tabs.setTabComponentAt(0, JBLabel("Chat"))
        tabs.setTabComponentAt(1, JBLabel("Trace"))
        tabs.setTabComponentAt(2, BorderLayoutPanel().apply {
            isOpaque = false
            addToLeft(JBLabel("Instructions"))
            addToRight(JPanel(java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, JBUI.scale(4), 0)).apply {
                isOpaque = false
                add(chatHistoryButton)
                add(newChatButton)
            })
        })

        return JPanel(BorderLayout()).apply {
            add(tabs, BorderLayout.CENTER)
        }
    }

    private fun buildComposerView(): JComponent {
        inlineActionsBar = InlineActionsBar(
            project = project,
            onAddContext = { onPickContextFiles() },
            currentInputGetter = { composerPanel.inputField.text },
            currentInputSetter = { text -> composerPanel.inputField.text = text },
            onContextFilesUpdated = {
                contextChipsPanel.refresh()
                val files = userContextState.getSelectedContextFiles().map { it.path }
                append("Context files now (${files.size}):\n")
                files.forEach { append("- $it\n") }
                append("\n")
            }
        )

        composerPanel = ComposerPanel(
            project = project,
            onSend = { text -> onSend(text) },
            inlineActionsBar = inlineActionsBar,
            contextChipsPanel = contextChipsPanel
        )

        inlineActionsBar.onPickModel = { anchor -> onPickModel(anchor) }

        contextChipsPanel.refresh()
        return composerPanel.component
    }

    private fun onNewChat() {
        chatSessions.createNewSession("New Chat")
        outputArea.text = ""
        tracePanel.setTraceLines(emptyList())
        append("New chat started.\n\n")
    }

    private fun onOpenChatHistory() {
        ChatHistoryDialog(project, chatSessions) { selectedSession ->
            loadChatSession(selectedSession.id)
        }.show()
    }

    private fun loadChatSession(sessionId: String) {
        chatSessions.setActiveSessionId(sessionId)
        outputArea.text = ""
        tracePanel.setTraceLines(emptyList())

        val messages = chatSessions.getMessages(sessionId)
        if (messages.isEmpty()) {
            append("No messages in this chat.\n\n")
        } else {
            messages.forEach { message ->
                val prefix = when (message.role) {
                    com.markolukarami.copilotclone.domain.entities.ChatRole.USER -> "You"
                    com.markolukarami.copilotclone.domain.entities.ChatRole.ASSISTANT -> "AI"
                    com.markolukarami.copilotclone.domain.entities.ChatRole.SYSTEM -> "System"
                }
                append("$prefix: ${message.content}\n\n")
            }
        }

        composerPanel.inputField.text = ""
    }

    private fun onPickContextFiles() {
        val manager = ContextManagerDialog(project) {
            contextChipsPanel.refresh()

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

    private fun onSend(text: String) {
        if (text.isBlank()) return

        composerPanel.setSendButtonEnabled(false)

        append("You: $text\n\n")
        append("AI is thinking...\n\n")

        object : Task.Backgroundable(project, "AI Chat", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Contacting LLM..."
                val result = controller.onUserMessage(text)
                append("DEBUG patch present: ${result.patch != null}\n")

                ApplicationManager.getApplication().invokeLater {
                    result.chatItems.drop(1).forEach { vm -> append(vm.displayText + "\n\n") }
                    tracePanel.setTraceLines(result.trace.lines)

                    val patch = result.patch
                    if (patch != null) {
                        val enrichedPatch = PatchEnricher(project).enrichPatch(patch)
                        PatchPreviewDialog(project, enrichedPatch) {
                            project.service<IntelliJPatchApplier>().apply(patch)
                            append("Patch applied (undo with Ctrl+Z)\n\n")
                        }.show()
                    }

                    composerPanel.setSendButtonEnabled(true)
                }
            }

            override fun onThrowable(error: Throwable) {
                ApplicationManager.getApplication().invokeLater {
                    append("Error: ${error.message ?: "Unknown error"}\n\n")
                    composerPanel.setSendButtonEnabled(true)
                }
            }
        }.queue()
    }

    private fun append(text: String) {
        outputArea.append(text)
        outputArea.caretPosition = outputArea.document.length
    }

    private fun onPickModel(anchor: JComponent) {
        object : Task.Backgroundable(project, "Fetching models from LM Studio", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Fetching models..."

                val config = settingsState.getModelConfig()
                val models = try {
                    modelRegistry.listModels(config)
                } catch (t: Throwable) {
                    ApplicationManager.getApplication().invokeLater {
                        append("Error fetching models: ${t.message}\n")
                        append("Base URL: ${config.baseUrl}\n\n")
                    }
                    return
                }

                ApplicationManager.getApplication().invokeLater {
                    if (models.isEmpty()) {
                        append("No models returned.\n")
                        append("LM Studio URL: ${config.baseUrl.trimEnd('/')}/v1/models\n\n")
                        return@invokeLater
                    }

                    ModelPickerPopup.show(
                        project = project,
                        anchor = anchor,
                        models = models,
                        currentModelId = settingsState.getSelectedModel(),
                        onPick = { picked ->
                            settingsState.setSelectedModel(picked.id)
                            append("Active model set to: ${picked.id}\n\n")
                        }
                    )
                }
            }

            override fun onThrowable(error: Throwable) {
                val config = settingsState.getModelConfig()
                ApplicationManager.getApplication().invokeLater {
                    append("Error fetching models: ${error.message ?: "Unknown error"}\n")
                    append("Base URL: ${config.baseUrl}\n\n")
                }
            }
        }.queue()
    }
}
