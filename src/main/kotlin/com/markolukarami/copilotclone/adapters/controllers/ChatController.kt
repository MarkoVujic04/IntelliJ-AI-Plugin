package com.markolukarami.copilotclone.adapters.controllers

import com.markolukarami.copilotclone.adapters.presentation.ChatPresenter
import com.markolukarami.copilotclone.adapters.presentation.ChatViewModel
import com.markolukarami.copilotclone.adapters.presentation.TraceLineVM
import com.markolukarami.copilotclone.adapters.presentation.TracePresenter
import com.markolukarami.copilotclone.adapters.presentation.TraceViewModel
import com.markolukarami.copilotclone.application.usecase.ChatHandler
import com.markolukarami.copilotclone.domain.entities.ChatMessage
import com.markolukarami.copilotclone.domain.entities.ChatRole
import com.markolukarami.copilotclone.domain.repositories.ChatSessionRepository

data class ChatControllerResult(
    val chatItems: List<ChatViewModel>,
    val trace: TraceViewModel
)

class ChatController (
    private val chatHandler: ChatHandler,
    private val chatPresenter: ChatPresenter,
    private val tracePresenter: TracePresenter,
    private val chatSessionRepository: ChatSessionRepository,
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

        val result = chatHandler.execute(trimmed)

        val sessionId = chatSessionRepository.getActiveSessionId()
        chatSessionRepository.appendMessage(sessionId, ChatMessage(ChatRole.USER, trimmed))
        chatSessionRepository.appendMessage(sessionId, ChatMessage(ChatRole.ASSISTANT, result.assistantText))

        val aiVm = chatPresenter.presentAssistant(result.assistantText)
        val traceVm = tracePresenter.present(result.trace)

        return ChatControllerResult(
            chatItems = listOf(userVm, aiVm),
            trace = traceVm
        )
    }
}