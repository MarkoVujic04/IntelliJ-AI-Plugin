package com.markolukarami.copilotclone.domain.repositories

import com.markolukarami.copilotclone.domain.entities.ChatMessage
import com.markolukarami.copilotclone.domain.entities.chat.ChatSession

interface ChatSessionRepository {
    fun getActiveSessionId(): String
    fun setActiveSessionId(id: String)

    fun listSessions(): List<ChatSession>
    fun createNewSession(title: String = "New Chat"): ChatSession

    fun getMessages(sessionId: String): List<ChatMessage>
    fun appendMessage(sessionId: String, message: ChatMessage)
    fun clearMessages(sessionId: String)
}