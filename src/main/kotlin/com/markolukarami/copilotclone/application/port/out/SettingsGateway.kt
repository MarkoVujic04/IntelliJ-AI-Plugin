package com.markolukarami.aiplugin.application.port.out

import com.markolukarami.aiplugin.domain.model.ModelConfig

interface SettingsGateway {
    fun getModelConfig(): ModelConfig
}
