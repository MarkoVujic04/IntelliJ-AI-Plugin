package com.markolukarami.copilotclone.frameworks.llm

import com.markolukarami.copilotclone.domain.entities.LLMProvider
import com.markolukarami.copilotclone.domain.repositories.ModelRegistryRepository

object ModelRegistryFactory {
    fun create(provider: LLMProvider): ModelRegistryRepository {
        return when (provider) {
            LLMProvider.LM_STUDIO -> LMStudioModelRegistryAdapter()
            LLMProvider.OLLAMA -> OllamaModelRegistryAdapter()
        }
    }
}