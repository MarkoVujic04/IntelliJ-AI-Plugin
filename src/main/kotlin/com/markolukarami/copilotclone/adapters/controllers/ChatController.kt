package com.markolukarami.copilotclone.adapters.controllers

import com.markolukarami.copilotclone.adapters.presentation.ChatPresenter
import com.markolukarami.copilotclone.adapters.presentation.ChatViewModel
import com.markolukarami.copilotclone.adapters.presentation.TraceLineVM
import com.markolukarami.copilotclone.adapters.presentation.TracePresenter
import com.markolukarami.copilotclone.adapters.presentation.TraceViewModel
import com.markolukarami.copilotclone.application.dto.SendChatCommand
import com.markolukarami.copilotclone.application.dto.SendChatResult
import com.markolukarami.copilotclone.application.usecase.ChatUseCase

data class ChatControllerResult(
    val chatItems: List<ChatViewModel>,
    val trace: TraceViewModel
)

class ChatController (
    private val chatUseCase: ChatUseCase,
    private val chatPresenter: ChatPresenter,
    private val tracePresenter: TracePresenter
) {
    fun onUserMessage(text: String): ChatControllerResult {
        val trimmed = text.trim()
        if (trimmed.isBlank()) {
            return ChatControllerResult(
                chatItems = emptyList(),
                trace = TraceViewModel(lines = listOf(TraceLineVM("No Input")))
            )
        }

        val userVm = chatPresenter.presentUser(trimmed)

        val result = chatUseCase.execute(trimmed)

        val aiVm = chatPresenter.presentAssistant(result.assistantText)
        val traceVm = tracePresenter.present(result.trace)

        return ChatControllerResult(
            chatItems = listOf(userVm, aiVm),
            trace = traceVm
        )
    }
}