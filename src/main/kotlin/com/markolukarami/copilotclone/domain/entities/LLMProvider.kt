package com.markolukarami.copilotclone.domain.entities

enum class LLMProvider(val displayName: String, val defaultUrl: String) {
    LM_STUDIO("LM Studio", "http://127.0.0.1:1234"),
    OLLAMA("Ollama", "http://127.0.0.1:11434");

    companion object {
        fun fromString(value: String): LLMProvider {
            return entries.find { it.name == value } ?: LM_STUDIO
        }
    }
}