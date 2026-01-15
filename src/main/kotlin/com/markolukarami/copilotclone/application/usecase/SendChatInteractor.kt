package com.markolukarami.aiplugin.application.usecase

import com.markolukarami.aiplugin.application.dto.SendChatCommand
import com.markolukarami.aiplugin.application.dto.SendChatResult
import com.markolukarami.aiplugin.application.port.out.LlmGateway
import com.markolukarami.aiplugin.application.port.out.SettingsGateway
import com.markolukarami.aiplugin.domain.model.ChatMessage
import com.markolukarami.aiplugin.domain.model.ChatRole

class SendChatInteractor(
    private val llmGateway: LlmGateway,
    private val settingsGateway: SettingsGateway
) {
    fun execute(command: SendChatCommand): SendChatResult {
        val config = settingsGateway.getModelConfig()

        val messages = listOf(
            ChatMessage(
                role = ChatRole.SYSTEM,
                content = "You are a helpful coding assistant inside IntelliJ. Answer clearly and concisely."
            ),
            ChatMessage(
                role = ChatRole.USER,
                content = command.userText
            )
        )

        val assistantText = llmGateway.chat(config, messages)
        return SendChatResult(assistantText = assistantText)
    }
}
