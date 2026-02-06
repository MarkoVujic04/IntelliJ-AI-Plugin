package com.markolukarami.copilotclone.frameworks.llm

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.markolukarami.copilotclone.adapters.controllers.ChatController
import com.markolukarami.copilotclone.adapters.presentation.ChatPresenter
import com.markolukarami.copilotclone.adapters.presentation.TracePresenter
import com.markolukarami.copilotclone.application.usecase.AgentChatUseCase
import com.markolukarami.copilotclone.application.usecase.ChatUseCase
import com.markolukarami.copilotclone.frameworks.editor.IntelliJEditorContextProvider
import com.markolukarami.copilotclone.frameworks.llm.LMStudioAdapter
import com.markolukarami.copilotclone.frameworks.settings.AiSettingsState
import com.markolukarami.copilotclone.frameworks.editor.IntelliJFileReaderAdapter
import com.markolukarami.copilotclone.frameworks.editor.IntelliJTextSearchAdapter
import com.markolukarami.copilotclone.frameworks.editor.UserContextState

object ChatWiring {

    fun chatController(project: Project): ChatController {
        val settingsRepo = service<AiSettingsState>()
        val userContextRepo = project.service<UserContextState>()
        val chatRepo = LMStudioAdapter()
        val editorRepo = IntelliJEditorContextProvider(project)

        val docSearchRepo = IntelliJTextSearchAdapter(project)
        val fileReaderRepo = IntelliJFileReaderAdapter()

        val useCase = ChatUseCase(
            chatRepository = chatRepo,
            settingsRepository = settingsRepo,
            editorContextRepository = editorRepo,
            textSearchRepository = docSearchRepo,
            fileReaderRepository = fileReaderRepo,
            userContextRepository = userContextRepo,
        )

        val agentUseCase = AgentChatUseCase(useCase)

        return ChatController(
            chatHandler = agentUseCase,
            chatPresenter = ChatPresenter(),
            tracePresenter = TracePresenter()
        )
    }
}
