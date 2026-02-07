package com.markolukarami.copilotclone.domain.entities.model

data class ModelInfo(
    val id: String,
    val type: String? = null,
    val publisher: String? = null,
    val quantization: String? = null,
)
