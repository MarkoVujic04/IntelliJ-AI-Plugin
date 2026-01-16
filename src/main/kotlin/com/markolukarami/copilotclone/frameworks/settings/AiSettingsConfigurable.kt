package com.markolukarami.aiplugin.frameworks.settings

import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBTextField
import com.markolukarami.copilotclone.frameworks.settings.AiSettingsState
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class AiSettingsConfigurable : Configurable {

    private val state: AiSettingsState = service()

    private var panel: JPanel? = null
    private val baseUrlField = JBTextField()
    private val modelField = JBTextField()

    override fun getDisplayName(): String = "AI Plugin"

    override fun createComponent(): JComponent {
        baseUrlField.text = state.baseUrl
        modelField.text = state.model

        val form = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(12, 12, 12, 12)
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

    override fun isModified(): Boolean =
        baseUrlField.text.trim() != state.baseUrl.trim() ||
                modelField.text.trim() != state.model.trim()

    override fun apply() {
        state.baseUrl = baseUrlField.text.trim()
        state.model = modelField.text.trim()
    }

    override fun reset() {
        baseUrlField.text = state.baseUrl
        modelField.text = state.model
    }

    override fun disposeUIResources() {
        panel = null
    }
}
