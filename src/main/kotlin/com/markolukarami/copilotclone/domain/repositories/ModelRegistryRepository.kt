package com.markolukarami.copilotclone.domain.repositories

import com.markolukarami.copilotclone.domain.entities.ModelConfig
import com.markolukarami.copilotclone.domain.entities.model.ModelInfo

interface ModelRegistryRepository {
    fun listModels(config: ModelConfig): List<ModelInfo>
}