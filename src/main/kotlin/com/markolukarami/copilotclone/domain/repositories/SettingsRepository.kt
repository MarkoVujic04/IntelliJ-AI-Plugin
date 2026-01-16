package com.markolukarami.copilotclone.domain.repositories

import com.markolukarami.copilotclone.domain.entities.ModelConfig

interface SettingsRepository {
    fun getModelConfig(): ModelConfig
}