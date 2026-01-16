package com.markolukarami.aiplugin.frameworks

import com.intellij.openapi.components.service
import com.markolukarami.aiplugin.application.usecase.ChatUseCase
import com.markolukarami.aiplugin.frameworks.lmstudio.LMStudioAdapter
import com.markolukarami.copilotclone.adapters.controllers.ChatController
import com.markolukarami.copilotclone.adapters.presentation.ChatPresenter
import com.markolukarami.copilotclone.frameworks.settings.AiSettingsState

object ChatWiring {

    fun chatController(): ChatController {
        val settingsRepo = service<AiSettingsState>()
        val chatRepo = LMStudioAdapter()

        val useCase = ChatUseCase(
            chatRepository = chatRepo,
            settingsRepository = settingsRepo
        )

        val presenter = ChatPresenter()

        return ChatController(
            chatUseCase = useCase,
            presenter = presenter
        )
    }
}
