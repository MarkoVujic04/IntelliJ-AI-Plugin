package com.markolukarami.copilotclone.frameworks.llm

import com.markolukarami.copilotclone.domain.entities.LLMProvider
import com.markolukarami.copilotclone.domain.repositories.ChatRepository

object ChatRepositoryFactory {

    fun create(provider: LLMProvider): ChatRepository {
        return when (provider) {
            LLMProvider.LM_STUDIO -> LMStudioAdapter()
            LLMProvider.OLLAMA -> OllamaAdapter()
        }
    }
}