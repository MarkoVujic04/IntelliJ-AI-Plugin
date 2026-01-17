package com.markolukarami.copilotclone.frameworks.llm
import com.intellij.openapi.components.service
import com.markolukarami.copilotclone.adapters.controllers.ChatController
import com.markolukarami.copilotclone.adapters.presentation.ChatPresenter
import com.markolukarami.copilotclone.application.usecase.ChatUseCase
import com.markolukarami.copilotclone.frameworks.editor.IntelliJEditorContextProvider
import com.markolukarami.copilotclone.frameworks.settings.AiSettingsState

object ChatWiring {

    fun chatController(project: com.intellij.openapi.project.Project): ChatController {
        val settingsRepo = service<AiSettingsState>()
        val chatRepo = LMStudioAdapter()
        val editorProvider = IntelliJEditorContextProvider(project)

        val useCase = ChatUseCase(
            chatRepository = chatRepo,
            settingsRepository = settingsRepo,
            editorProvider
        )

        return ChatController(useCase, ChatPresenter())
    }
}
