package com.markolukarami.copilotclone.frameworks.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import com.markolukarami.copilotclone.domain.entities.ModelConfig
import com.markolukarami.copilotclone.domain.repositories.SettingsRepository

@State(
    name = "AiPluginSettings",
    storages = [Storage("ai-plugin.xml")]
)
@Service(Service.Level.APP)
class AiSettingsState : PersistentStateComponent<AiSettingsState>, SettingsRepository {

    var baseUrl: String = "http://127.0.0.1:1234"
    var model: String = "mistralai/devstral-small-2-2512"

    override fun getState(): AiSettingsState = this
    override fun loadState(state: AiSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    fun getSelectedModel(): String = state.model
    fun setSelectedModel(value: String) { state.model = value }

    override fun getModelConfig(): ModelConfig {
        val normalizedUrl = baseUrl.trim().removeSuffix("/")
        return ModelConfig(
            baseUrl = normalizedUrl,
            model = model.trim()
        )
    }
}