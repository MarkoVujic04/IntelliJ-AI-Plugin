package com.markolukarami.copilotclone.application.usecase
import com.markolukarami.copilotclone.domain.repositories.ChatRepository
import com.markolukarami.copilotclone.domain.repositories.SettingsRepository
import com.markolukarami.copilotclone.domain.entities.ChatMessage
import com.markolukarami.copilotclone.domain.entities.ChatResult
import com.markolukarami.copilotclone.domain.entities.ChatRole
import com.markolukarami.copilotclone.domain.entities.TraceStep
import com.markolukarami.copilotclone.domain.entities.TraceType
import com.markolukarami.copilotclone.domain.repositories.EditorContextRepository

class ChatUseCase(
    private val chatRepository: ChatRepository,
    private val settingsRepository: SettingsRepository,
    private val editorContextRepository: EditorContextRepository
) {

    fun execute(userText: String): ChatResult {
        val trace = mutableListOf<TraceStep>()
        val context = editorContextRepository.getCurrentContext()
        val config = settingsRepository.getModelConfig()


        trace += TraceStep(
            title = "Load settings",
            details = "Reading Base URL + Model from plugin settings",
            type = TraceType.INFO
        )
        trace += TraceStep(
            title = "Model config",
            details = "baseUrl=${config.baseUrl}, model=${config.model}",
            type = TraceType.INFO
        )

        trace += TraceStep(
            title = "Read editor context",
            details = "Getting selected text + active file path",
            type = TraceType.IO
        )

        if (!context.selectedText.isNullOrBlank()) {
            trace += TraceStep(
                title = "Selection detected",
                details = "Selected chars=${context.selectedText.length}, file=${context.filePath ?: "unknown"}",
                type = TraceType.INFO
            )
        } else {
            trace += TraceStep(
                title = "No selection",
                details = "No selected text in editor",
                type = TraceType.INFO
            )
        }

        val messages = mutableListOf(
            ChatMessage(
                role = ChatRole.SYSTEM,
                content = "You are a helpful coding assistant inside IntelliJ IDEA. Be direct and practical."
            )
        )

        context.selectedText?.takeIf { it.isNotBlank() }?.let { sel ->
            messages += ChatMessage(
                role = ChatRole.SYSTEM,
                content = "The user selected this code:\n$sel"
            )
        }

        messages += ChatMessage(role = ChatRole.USER, content = userText)

        trace += TraceStep(
            title = "Send request to local LLM",
            details = "POST /v1/chat/completions with ${messages.size} messages",
            type = TraceType.MODEL
        )

        val assistantText = chatRepository.chat(config, messages)

        trace += TraceStep(
            title = "Receive response",
            details = "Received ${assistantText.length} chars",
            type = TraceType.MODEL
        )

        if (assistantText.startsWith("Error:", ignoreCase = true) ||
            assistantText.contains("failed", ignoreCase = true) ||
            assistantText.contains("could not reach", ignoreCase = true)
        ) {
            trace += TraceStep(
                title = "Model error",
                details = assistantText,
                type = TraceType.ERROR
            )
        }

        return ChatResult(
            assistantText = assistantText,
            trace = trace
        )
    }
}