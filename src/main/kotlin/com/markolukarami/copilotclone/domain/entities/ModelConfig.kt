package com.markolukarami.copilotclone.domain.entities

data class ModelConfig(
    val provider: LLMProvider = LLMProvider.LM_STUDIO,
    val baseUrl: String,
    val model: String,
    val maxTokens: Int = 600,
    val temperature: Double = 0.2
)