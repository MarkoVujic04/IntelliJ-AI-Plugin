package com.markolukarami.copilotclone.frameworks.settings
import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBTextField
import com.markolukarami.copilotclone.domain.entities.LLMProvider
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class AiSettingsConfigurable : Configurable {

    private val state: AiSettingsState = service()

    private var panel: JPanel? = null
    private val providerCombo = JComboBox(LLMProvider.entries.toTypedArray())
    private val baseUrlField = JBTextField()
    private val modelField = JBTextField()

    override fun getDisplayName(): String = "AI Plugin"

    override fun createComponent(): JComponent {
        providerCombo.selectedItem = state.getProvider()
        baseUrlField.text = state.baseUrl
        modelField.text = state.model

        providerCombo.addActionListener {
            val selected = providerCombo.selectedItem as? LLMProvider
            if (selected != null && baseUrlField.text.isBlank()) {
                baseUrlField.text = selected.defaultUrl
            }
        }

        val form = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(12, 12, 12, 12)
            add(JLabel("LLM Provider:"))
            add(providerCombo)
            add(JLabel("Base URL (OpenAI compatible):"))
            add(baseUrlField)
            add(JLabel("Model:"))
            add(modelField)
        }

        return JPanel(BorderLayout()).apply {
            add(form, BorderLayout.NORTH)
            panel = this
        }
    }

    override fun isModified(): Boolean {
        val selectedProvider = providerCombo.selectedItem as? LLMProvider
        return selectedProvider != state.getProvider() ||
                baseUrlField.text.trim() != state.baseUrl.trim() ||
                modelField.text.trim() != state.model.trim()
    }

    override fun apply() {
        val selectedProvider = providerCombo.selectedItem as? LLMProvider
        if (selectedProvider != null) {
            state.setProvider(selectedProvider)
        }
        state.baseUrl = baseUrlField.text.trim()
        state.model = modelField.text.trim()
    }

    override fun reset() {
        providerCombo.selectedItem = state.getProvider()
        baseUrlField.text = state.baseUrl
        modelField.text = state.model
    }

    override fun disposeUIResources() {
        panel = null
    }
}
