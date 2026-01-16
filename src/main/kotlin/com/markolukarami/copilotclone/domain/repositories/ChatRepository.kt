package com.markolukarami.copilotclone.domain.repositories

import com.markolukarami.copilotclone.domain.entities.ChatMessage
import com.markolukarami.copilotclone.domain.entities.ModelConfig

interface ChatRepository {
    fun chat(modelConfig: ModelConfig, messages: List<ChatMessage>): String
}