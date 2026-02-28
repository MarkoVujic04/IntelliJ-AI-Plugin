package com.markolukarami.copilotclone.frameworks.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import com.markolukarami.copilotclone.domain.entities.LLMProvider
import com.markolukarami.copilotclone.domain.entities.ModelConfig
import com.markolukarami.copilotclone.domain.repositories.SettingsRepository

@State(
    name = "AiPluginSettings",
    storages = [Storage("ai-plugin.xml")]
)
@Service(Service.Level.APP)
class AiSettingsState : PersistentStateComponent<AiSettingsState>, SettingsRepository {

    var provider: String = LLMProvider.LM_STUDIO.name
    var baseUrl: String = "http://127.0.0.1:1234"
    var model: String = "mistralai/devstral-small-2-2512"

    override fun getState(): AiSettingsState = this
    override fun loadState(state: AiSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    fun getSelectedModel(): String = state.model
    fun setSelectedModel(value: String) { state.model = value }

    fun getProvider(): LLMProvider = LLMProvider.fromString(provider)
    fun setProvider(value: LLMProvider) {provider = value.name}

    override fun getModelConfig(): ModelConfig {
        val normalizedUrl = baseUrl.trim().removeSuffix("/")
        return ModelConfig(
            provider = getProvider(),
            baseUrl = normalizedUrl,
            model = model.trim()
        )
    }
}