package com.markolukarami.copilotclone.application.usecase
import com.markolukarami.copilotclone.domain.repositories.ChatRepository
import com.markolukarami.copilotclone.domain.repositories.SettingsRepository
import com.markolukarami.copilotclone.domain.entities.ChatMessage
import com.markolukarami.copilotclone.domain.entities.ChatResult
import com.markolukarami.copilotclone.domain.entities.ChatRole
import com.markolukarami.copilotclone.domain.entities.FileSnippet
import com.markolukarami.copilotclone.domain.entities.TextSnippet
import com.markolukarami.copilotclone.domain.entities.tool.ToolAction
import com.markolukarami.copilotclone.domain.entities.tool.ToolPlan
import com.markolukarami.copilotclone.domain.entities.tool.ToolType
import com.markolukarami.copilotclone.domain.entities.trace.TraceStep
import com.markolukarami.copilotclone.domain.entities.trace.TraceType
import com.markolukarami.copilotclone.domain.repositories.EditorContextRepository
import com.markolukarami.copilotclone.domain.repositories.FileReaderRepository
import com.markolukarami.copilotclone.domain.repositories.TextSearchRepository
import com.markolukarami.copilotclone.domain.repositories.UserContextRepository
import com.markolukarami.copilotclone.frameworks.llm.ToolPlanParser

class ChatUseCase(
    private val chatRepository: ChatRepository,
    private val settingsRepository: SettingsRepository,
    private val editorContextRepository: EditorContextRepository,
    private val textSearchRepository: TextSearchRepository,
    private val fileReaderRepository: FileReaderRepository,
    private val userContextRepository: UserContextRepository
) : ChatHandler {

    override fun execute(userText: String): ChatResult {
        val trace = mutableListOf<TraceStep>()
        val config = settingsRepository.getModelConfig()

        val editorContext = editorContextRepository.getCurrentContext()

        trace += TraceStep("Ask model for tool plan", type = TraceType.MODEL)

        val planPrompt = listOf(
            ChatMessage(
                ChatRole.SYSTEM,
                """
                You are a planning module.
                Decide if tools are needed.

                Tools:
                - SEARCH_PROJECT_TEXT
                - READ_FILE

                Output STRICT JSON only:
                {
                  "useTools": true/false,
                  "reasoning": "...",
                  "actions": [
                    {"type":"SEARCH_PROJECT_TEXT","query":"...","limit":8}
                  ]
                }
                """.trimIndent()
            ),
            ChatMessage(ChatRole.USER, userText)
        )

        val planRaw = chatRepository.chat(config, planPrompt)
        val plan = ToolPlanParser.parseOrNull(planRaw)

        if (plan == null) {
            trace += TraceStep("Plan parsing failed", type = TraceType.ERROR)
        } else {
            trace += TraceStep(
                "Plan received",
                "useTools=${plan.useTools}, actions=${plan.actions.size}",
                TraceType.INFO
            )
        }

        val effectivePlan = plan ?: fallbackPlan(userText)

        val snippets = mutableListOf<TextSnippet>()
        val files = mutableListOf<FileSnippet>()

        if (effectivePlan.useTools) {
            for (action in effectivePlan.actions) {
                when (action.type) {
                    ToolType.SEARCH_PROJECT_TEXT -> {
                        trace += TraceStep("Tool: Search project", action.query, TraceType.TOOL)
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

        val finalMessages = mutableListOf(
            ChatMessage(ChatRole.SYSTEM, "You are a helpful IntelliJ coding assistant.")
        )

        editorContext.selectedText?.let {
            finalMessages += ChatMessage(ChatRole.SYSTEM, "Selected code:\n$it")
        }

        if (snippets.isNotEmpty()) {
            finalMessages += ChatMessage(
                ChatRole.SYSTEM,
                "Project snippets:\n" + snippets.joinToString("\n") {
                    "- ${it.filePath}:${it.lineNumber} ${it.preview}"
                }
            )
        }

        if (files.isNotEmpty()) {
            finalMessages += ChatMessage(
                ChatRole.SYSTEM,
                "File excerpts:\n" + files.joinToString("\n\n") {
                    "FILE ${it.filePath}\n${it.content.take(1500)}"
                }
            )
        }

        finalMessages += ChatMessage(ChatRole.USER, userText)

        trace += TraceStep("Send final request", type = TraceType.MODEL)
        val answer = chatRepository.chat(config, finalMessages)

        trace += TraceStep("Receive response", type = TraceType.MODEL)

        return ChatResult(answer, trace)
    }

    private fun fallbackPlan(text: String) =
        ToolPlan(
            useTools = true,
            actions = listOf(
                ToolAction(ToolType.SEARCH_PROJECT_TEXT, query = text, limit = 8)
            ),
            reasoning = "Fallback plan"
        )
}