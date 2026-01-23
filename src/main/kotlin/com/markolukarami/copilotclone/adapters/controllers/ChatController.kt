package com.markolukarami.copilotclone.adapters.controllers

import com.markolukarami.copilotclone.adapters.presentation.ChatPresenter
import com.markolukarami.copilotclone.adapters.presentation.ChatViewModel
import com.markolukarami.copilotclone.application.dto.SendChatCommand
import com.markolukarami.copilotclone.application.dto.SendChatResult
import com.markolukarami.copilotclone.application.usecase.ChatUseCase

class ChatController (
    private val chatUseCase: ChatUseCase,
    private val presenter: ChatPresenter
) {
    fun onUserMessage(userText: String): List<ChatViewModel> {
        val trimmed = userText.trim()
        if(trimmed.isBlank()) return emptyList()

        val result = chatUseCase.execute(trimmed)

        val userVm = presenter.presentUser(trimmed)
        val assistantText = chatUseCase.execute(trimmed)
        val assistantVm = presenter.presentAssistant(result.assistantText)

        return listOf(userVm, assistantVm)
    }
}