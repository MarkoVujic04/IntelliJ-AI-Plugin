package com.markolukarami.copilotclone.frameworks.chat

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.markolukarami.copilotclone.domain.entities.ChatMessage
import com.markolukarami.copilotclone.domain.entities.ChatRole
import com.markolukarami.copilotclone.domain.entities.chat.ChatSession
import com.markolukarami.copilotclone.domain.repositories.ChatSessionRepository
import java.util.UUID

@Service(Service.Level.PROJECT)
@State(
    name = "CopilotClone.ChatSessionState",
    storages = [Storage(StoragePathMacros.WORKSPACE_FILE)]
)
class ChatSessionState(private val project: Project) :
    PersistentStateComponent<ChatSessionState.State>,
    ChatSessionRepository {

    data class StoredMessage(
        var role: String = "USER",
        var content: String = ""
    )

    data class StoredSession(
        var id: String = "",
        var title: String = "New Chat",
        var createdAtMillis: Long = 0L,
        var messages: MutableList<StoredMessage> = mutableListOf()
    )

    data class State(
        var activeSessionId: String = "",
        var sessions: MutableList<StoredSession> = mutableListOf()
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
        ensureActiveSession()
    }

    private fun ensureActiveSession() {
        if (state.sessions.isEmpty()) {
            val s = createNewSession("New Chat")
            state.activeSessionId = s.id
            return
        }
        if (state.activeSessionId.isBlank() || state.sessions.none { it.id == state.activeSessionId }) {
            state.activeSessionId = state.sessions.first().id
        }
    }

    override fun getActiveSessionId(): String {
        ensureActiveSession()
        return state.activeSessionId
    }

    override fun setActiveSessionId(id: String) {
        state.activeSessionId = id
        ensureActiveSession()
    }

    override fun listSessions(): List<ChatSession> {
        ensureActiveSession()
        return state.sessions.map { ChatSession(it.id, it.title, it.createdAtMillis) }
    }

    override fun createNewSession(title: String): ChatSession {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val stored = StoredSession(id = id, title = title, createdAtMillis = now)
        state.sessions.add(0, stored)
        state.activeSessionId = id
        return ChatSession(id, title, now)
    }

    override fun getMessages(sessionId: String): List<ChatMessage> {
        val s = state.sessions.firstOrNull { it.id == sessionId } ?: return emptyList()
        return s.messages.map {
            val role = when (it.role) {
                "SYSTEM" -> ChatRole.SYSTEM
                "ASSISTANT" -> ChatRole.ASSISTANT
                else -> ChatRole.USER
            }
            ChatMessage(role, it.content)
        }
    }

    override fun appendMessage(sessionId: String, message: ChatMessage) {
        val s = state.sessions.firstOrNull { it.id == sessionId } ?: return
        val roleStr = when (message.role) {
            ChatRole.SYSTEM -> "SYSTEM"
            ChatRole.ASSISTANT -> "ASSISTANT"
            ChatRole.USER -> "USER"
        }
        s.messages.add(StoredMessage(role = roleStr, content = message.content))
    }

    override fun clearMessages(sessionId: String) {
        val s = state.sessions.firstOrNull { it.id == sessionId } ?: return
        s.messages.clear()
    }

    override fun deleteSession(sessionId: String) {
        state.sessions.removeIf { it.id == sessionId }
        if (state.activeSessionId == sessionId) {
            ensureActiveSession()
        }
    }

    init {
        ensureActiveSession()
    }
}