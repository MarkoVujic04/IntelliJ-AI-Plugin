package com.markolukarami.aiplugin.application.port.out

import com.markolukarami.aiplugin.domain.model.ChatMessage
import com.markolukarami.aiplugin.domain.model.ModelConfig

interface LlmGateway {
    fun chat(modelConfig: ModelConfig, messages: List<ChatMessage>): String
}
