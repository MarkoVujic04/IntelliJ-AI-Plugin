package com.markolukarami.copilotclone.ui.components

import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.markolukarami.copilotclone.frameworks.settings.AiSettingsState
import com.markolukarami.copilotclone.ui.PromptLibraryPopup
import com.markolukarami.copilotclone.ui.ProviderSelectorPopup
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

class InlineActionsBar(
    private val project: Project,
    private val onAddContext: () -> Unit,
    private val currentInputGetter: () -> String,
    private val currentInputSetter: (String) -> Unit,
    private val onContextFilesUpdated: () -> Unit,
    private val onProviderChanged: (() -> Unit)? = null
) {

    var onPickModel: ((JComponent) -> Unit)? = null
    private val settingsState = service<AiSettingsState>()
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

        val providerButton = JButton(AllIcons.General.ExternalTools).apply {
            isFocusable = false
            toolTipText = "Select LLM Provider"
            putClientProperty("JButton.buttonType", "toolbutton")
            addActionListener {
                ProviderSelectorPopup.show(
                    anchor = this,
                    currentProvider = settingsState.getProvider(),
                    onProviderSelected = { provider ->
                        settingsState.setProvider(provider)
                        onProviderChanged?.invoke()
                    }
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
            add(providerButton)
            add(modelButton)
        }
    }
}



