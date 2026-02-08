package com.markolukarami.copilotclone.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.EditorTextField
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import com.intellij.ui.JBColor
import com.markolukarami.copilotclone.frameworks.editor.UserContextState
import com.markolukarami.copilotclone.frameworks.llm.ChatWiring
import com.markolukarami.copilotclone.frameworks.llm.LMStudioModelRegistryAdapter
import com.markolukarami.copilotclone.frameworks.settings.AiSettingsState
import com.markolukarami.copilotclone.ui.components.BookmarkIcons
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTabbedPane
import javax.swing.SwingConstants
import javax.swing.border.EmptyBorder

class AiToolWindowPanel(private val project: Project) {

    private val controller = ChatWiring.chatController(project)
    private val userContextState = project.service<UserContextState>()
    private val settingsState = service<AiSettingsState>()
    private val modelRegistry = LMStudioModelRegistryAdapter()


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
        addActionListener { onSend() }
    }

    private val tracePanel = TracePanel(project)

    private val contextChipsRow = JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(6), 0)).apply {
        isOpaque = false
        border = EmptyBorder(0, 0, 0, 0)
    }

    private val composerToolbar: JComponent = createComposerToolbar()

    val component: JComponent = JPanel(BorderLayout()).apply {
        val chatView = JPanel(BorderLayout()).apply {
            val scroll = JBScrollPane(outputArea).apply {
                preferredSize = Dimension(380, 500)
            }
            add(scroll, BorderLayout.CENTER)

            val bottom = JPanel(BorderLayout()).apply {
                border = JBUI.Borders.empty(8)
                add(buildComposer(), BorderLayout.CENTER)
            }

            add(bottom, BorderLayout.SOUTH)
        }

        val tabs = JTabbedPane().apply {
            addTab("Chat", chatView)
            addTab("Trace", tracePanel.component)
        }

        add(tabs, BorderLayout.CENTER)
    }

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
            addToCenter(contextChipsRow.apply {
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
            addToLeft(createInlineActions())
            addToRight(sendButton)
        }

        composer.addToTop(chipsRow)
        composer.addToCenter(editorRow)
        composer.addToBottom(bottomRow)

        refreshContextChips()
        return composer
    }

    private fun createInlineActions(): JComponent {
        val plus = JButton(AllIcons.General.Add).apply {
            isFocusable = false
            toolTipText = "Add context"
            putClientProperty("JButton.buttonType", "toolbutton")
            addActionListener { onPickContextFiles() }
        }

        val bookmark = JButton(BookmarkIcons.BOOKMARK).apply {
            isFocusable = false
            toolTipText = "Saved prompts"
            putClientProperty("JButton.buttonType", "toolbutton")
            addActionListener {
                PromptLibraryPopup.show(
                    project = project,
                    anchor = this,
                    currentInput = { inputField.text },
                    onPick = { picked -> inputField.text = picked }
                )
            }
        }

        val modelButton = JButton(AllIcons.General.Settings).apply {
            isFocusable = false
            toolTipText = "Select model"
            putClientProperty("JButton.buttonType", "toolbutton")
            addActionListener { onPickModel(this) }
        }

        return JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(6), 0)).apply {
            isOpaque = false
            add(plus)
            add(bookmark)
            add(modelButton)
        }
    }


    private fun createComposerToolbar(): JComponent {
        val group = DefaultActionGroup().apply {
            add(AddContextAction())
            add(BookmarkAction())
        }

        val toolbar = ActionManager.getInstance()
            .createActionToolbar("CopilotCloneComposerToolbar", group, true)

        toolbar.setTargetComponent(inputField)
        toolbar.setMiniMode(true)

        toolbar.component.apply {
            isOpaque = false
            border = JBUI.Borders.emptyRight(8)
        }

        return toolbar.component
    }

    private inner class AddContextAction : DumbAwareAction("Add Context", "Add files to context", AllIcons.General.Add) {
        override fun actionPerformed(e: AnActionEvent) {
            onPickContextFiles()
        }
    }

    private inner class BookmarkAction : DumbAwareAction("Saved prompts", "Open saved prompts", BookmarkIcons.BOOKMARK) {
        override fun actionPerformed(e: AnActionEvent) {
            PromptLibraryPopup.show(
                project = project,
                anchor = composerToolbar,
                currentInput = { inputField.text },
                onPick = { picked -> inputField.text = picked }
            )
        }
    }

    private fun onPickContextFiles() {
        val manager = ContextManagerDialog(project) {
            refreshContextChips()

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

    private fun refreshContextChips() {
        contextChipsRow.removeAll()

        val selected = userContextState.getSelectedContextFiles()
        if (selected.isEmpty()) {
            contextChipsRow.add(makeStateChip("No context"))
        } else {
            contextChipsRow.add(makeStateChip("Context"))
            selected.take(8).forEach { cf ->
                contextChipsRow.add(makeFileChip(cf))
            }
            if (selected.size > 8) {
                contextChipsRow.add(makeStateChip("+${selected.size - 8} more"))
            }
        }

        contextChipsRow.revalidate()
        contextChipsRow.repaint()
    }

    private fun makeStateChip(text: String): JComponent {
        return JBLabel(text, SwingConstants.CENTER).apply {
            border = JBUI.Borders.compound(
                JBUI.Borders.customLine(JBColor.border(), 1),
                JBUI.Borders.empty(2, 8)
            )
            isOpaque = true
            background = JBColor.namedColor("Editor.SearchField.background", JBColor.PanelBackground)
        }
    }

    private fun makeFileChip(cf: com.markolukarami.copilotclone.domain.entities.context.ContextFile): JComponent {
        val name = cf.path.substringAfterLast('/').substringAfterLast('\\')
        val chip = BorderLayoutPanel().apply {
            isOpaque = true
            background = JBColor.namedColor("Editor.SearchField.background", JBColor.PanelBackground)
            border = JBUI.Borders.compound(
                JBUI.Borders.customLine(JBColor.border(), 1),
                JBUI.Borders.empty(2, 8)
            )
        }

        val label = JBLabel(name).apply {
            toolTipText = cf.path
        }

        val close = JBLabel(AllIcons.Actions.Close).apply {
            toolTipText = "Remove from context"
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            border = JBUI.Borders.emptyLeft(6)
            addMouseListener(object : java.awt.event.MouseAdapter() {
                override fun mouseClicked(e: java.awt.event.MouseEvent) {
                    //TODO Remove Functionality
                    append("Remove chip clicked (wire up remove in UserContextState): ${cf.path}\n")
                }
            })
        }

        chip.addToCenter(label)
        chip.addToRight(close)
        return chip
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
                            append("✅ Active model set to: ${picked.id}\n\n")
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
