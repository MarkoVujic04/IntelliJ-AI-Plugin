package com.markolukarami.copilotclone.application.usecase
import com.markolukarami.copilotclone.domain.repositories.ChatRepository
import com.markolukarami.copilotclone.domain.repositories.SettingsRepository
import com.markolukarami.copilotclone.domain.entities.ChatMessage
import com.markolukarami.copilotclone.domain.entities.ChatRole
import com.markolukarami.copilotclone.domain.repositories.EditorContextRepository

class ChatUseCase(
    private val chatRepository: ChatRepository,
    private val settingsRepository: SettingsRepository,
    private val editorContextRepository: EditorContextRepository
) {
    fun execute(userText: String): String {
        val config = settingsRepository.getModelConfig()
        val context = editorContextRepository.getCurrentContext()

        val messages = mutableListOf(
            ChatMessage(
                role = ChatRole.SYSTEM,
                content = "You are a helpful coding assistant inside IntelliJ. Answer clearly and concisely."
            ),
        )

        context.selectedText?.let {
            messages += ChatMessage(
                role = ChatRole.SYSTEM,
                content = "The user has selected the following code: \n$it"
            )
        }

        messages += ChatMessage(
            role = ChatRole.USER,
            content = userText
        )

        return chatRepository.chat(config, messages)
    }
}
