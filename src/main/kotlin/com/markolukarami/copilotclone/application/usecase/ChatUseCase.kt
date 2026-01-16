package com.markolukarami.aiplugin.application.usecase

import com.markolukarami.copilotclone.domain.repositories.ChatRepository
import com.markolukarami.copilotclone.domain.repositories.SettingsRepository
import com.markolukarami.copilotclone.application.dto.SendChatCommand
import com.markolukarami.copilotclone.application.dto.SendChatResult
import com.markolukarami.copilotclone.domain.entities.ChatMessage
import com.markolukarami.copilotclone.domain.entities.ChatRole

class ChatUseCase(
    private val chatRepository: ChatRepository,
    private val settingsRepository: SettingsRepository
) {
    fun execute(userText: String): String {
        val config = settingsRepository.getModelConfig()

        val messages = listOf(
            ChatMessage(
                role = ChatRole.SYSTEM,
                content = "You are a helpful coding assistant inside IntelliJ. Answer clearly and concisely."
            ),
            ChatMessage(
                role = ChatRole.USER,
                content = userText
            )
        )

        return chatRepository.chat(config, messages)
    }
}
