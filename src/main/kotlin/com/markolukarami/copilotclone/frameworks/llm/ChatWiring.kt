package com.markolukarami.copilotclone.frameworks.llm

import com.intellij.openapi.project.Project
import com.intellij.openapi.components.service
import com.markolukarami.copilotclone.adapters.controllers.ChatController
import com.markolukarami.copilotclone.adapters.presentation.ChatPresenter
import com.markolukarami.copilotclone.adapters.presentation.TracePresenter
import com.markolukarami.copilotclone.agent.application.Executor
import com.markolukarami.copilotclone.agent.application.Planner
import com.markolukarami.copilotclone.agent.application.Scout
import com.markolukarami.copilotclone.agent.application.Strategist
import com.markolukarami.copilotclone.application.usecase.AgentChatUseCase
import com.markolukarami.copilotclone.application.usecase.AgentPipelineUseCase
import com.markolukarami.copilotclone.frameworks.editor.IntelliJEditorContextProvider
import com.markolukarami.copilotclone.frameworks.editor.IntelliJFileReaderAdapter
import com.markolukarami.copilotclone.frameworks.editor.IntelliJTextSearchAdapter
import com.markolukarami.copilotclone.frameworks.editor.UserContextState
import com.markolukarami.copilotclone.frameworks.settings.AiSettingsState


object ChatWiring {

    fun chatController(project: Project): ChatController {
        val settingsRepo = service<AiSettingsState>()
        val userContextRepo = project.service<UserContextState>()
        val chatRepo = LMStudioAdapter()
        val editorRepo = IntelliJEditorContextProvider(project)
        val textSearchRepo = IntelliJTextSearchAdapter(project)
        val fileReaderRepo = IntelliJFileReaderAdapter()

        val planner = Planner(chatRepo)
        val scout = Scout(
            textSearchRepository = textSearchRepo,
            fileReaderRepository = fileReaderRepo,
            userContextRepository = userContextRepo
        )
        val strategist = Strategist(
            maxSnippets = 8,
            maxFileChars = 1500,
            maxFiles = 5
        )
        val executor = Executor(
            chatRepository = chatRepo,
            editorContextRepository = editorRepo
        )

        val pipeline = AgentPipelineUseCase(
            settingsRepository = settingsRepo,
            planner = planner,
            scout = scout,
            strategist = strategist,
            executor = executor
        )

        val agentUseCase = AgentChatUseCase(pipeline)

        return ChatController(
            chatHandler = agentUseCase,
            chatPresenter = ChatPresenter(),
            tracePresenter = TracePresenter()
        )
    }
}
