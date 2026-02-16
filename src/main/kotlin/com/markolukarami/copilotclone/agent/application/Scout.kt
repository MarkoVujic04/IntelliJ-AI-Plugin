package com.markolukarami.copilotclone.agent.application

import com.markolukarami.copilotclone.agent.entities.ScoutEvidence
import com.markolukarami.copilotclone.domain.entities.tool.ToolPlan
import com.markolukarami.copilotclone.domain.entities.tool.ToolType
import com.markolukarami.copilotclone.domain.entities.trace.TraceStep
import com.markolukarami.copilotclone.domain.entities.trace.TraceType
import com.markolukarami.copilotclone.domain.repositories.EditorContextRepository
import com.markolukarami.copilotclone.domain.repositories.FileReaderRepository
import com.markolukarami.copilotclone.domain.repositories.TextSearchRepository
import com.markolukarami.copilotclone.domain.repositories.UserContextRepository

class Scout(
    private val textSearchRepository: TextSearchRepository,
    private val fileReaderRepository: FileReaderRepository,
    private val userContextRepository: UserContextRepository,
) {
    fun gather(plan: ToolPlan?, userText: String, trace: MutableList<TraceStep>): ScoutEvidence {
        trace += TraceStep("Agent: Scout", "Run tools from plan", TraceType.TOOL)

        val snippets = mutableListOf<com.markolukarami.copilotclone.domain.entities.TextSnippet>()
        val files = mutableListOf<com.markolukarami.copilotclone.domain.entities.FileSnippet>()

        val selectedContext = userContextRepository.getSelectedContextFiles().map { it.path }.distinct()
        if (selectedContext.isNotEmpty()) {
            trace += TraceStep("Tool: User context files", "files=${selectedContext.size}", TraceType.TOOL)
            selectedContext.take(5).forEach { path ->
                fileReaderRepository.readFile(path)?.let { files += it }
            }
        }

        if (plan == null) {
            val contextPaths = userContextRepository
                .getSelectedContextFiles()
                .map { it.path }
                .distinct()

            val contextFileSnippets = mutableListOf<com.markolukarami.copilotclone.domain.entities.FileSnippet>()
            contextPaths.take(5).forEach { path ->
                fileReaderRepository.readFile(path)?.let { contextFileSnippets += it }
            }

            return ScoutEvidence(
                files = contextFileSnippets,
                snippets = emptyList()
            )
        }

        if (plan.useTools) {
            for (action in plan.actions) {
                when (action.type) {
                    ToolType.SEARCH_PROJECT_TEXT -> {
                        trace += TraceStep("Tool: Search project", action.query ?: userText, TraceType.TOOL)
                        snippets += textSearchRepository.search(
                            action.query ?: userText,
                            action.limit ?: 8
                        )
                    }
                    ToolType.READ_FILE -> {
                        val path = action.filePath ?: continue
                        trace += TraceStep("Tool: Read file", path, TraceType.TOOL, path)
                        fileReaderRepository.readFile(path)?.let { files += it }
                    }
                }
            }
        }

        return ScoutEvidence(snippets = snippets, files = files)
    }
}