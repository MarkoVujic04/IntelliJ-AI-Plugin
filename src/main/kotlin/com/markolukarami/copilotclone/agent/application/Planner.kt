package com.markolukarami.copilotclone.agent.application

import com.markolukarami.copilotclone.domain.entities.ChatMessage
import com.markolukarami.copilotclone.domain.entities.ChatRole
import com.markolukarami.copilotclone.domain.entities.ModelConfig
import com.markolukarami.copilotclone.domain.entities.tool.ToolAction
import com.markolukarami.copilotclone.domain.entities.tool.ToolPlan
import com.markolukarami.copilotclone.domain.entities.tool.ToolType
import com.markolukarami.copilotclone.domain.entities.trace.TraceStep
import com.markolukarami.copilotclone.domain.entities.trace.TraceType
import com.markolukarami.copilotclone.domain.repositories.ChatRepository
import com.markolukarami.copilotclone.frameworks.llm.ToolPlanParser

class Planner(
    private val chatRepository: ChatRepository
) {
    fun plan(
        userText: String,
        config: ModelConfig,
        trace: MutableList<TraceStep>
    ): ToolPlan
    {
        trace += TraceStep("Agent: Planner", "Ask model for tool plan", TraceType.MODEL)

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
                    {"type":"SEARCH_PROJECT_TEXT","query":"...","limit":8},
                    {"type":"READ_FILE","filePath":"..."}
                  ]
                }
                """.trimIndent()
            ),
            ChatMessage(ChatRole.USER, userText)
        )

        val planRaw = chatRepository.chat(config, planPrompt)
        val plan = ToolPlanParser.parseOrNull(planRaw)

        return if (plan == null) {
            trace += TraceStep("Planner: parse failed", "Fallback plan", TraceType.ERROR)
            ToolPlan(
                useTools = true,
                actions = listOf(ToolAction(ToolType.SEARCH_PROJECT_TEXT, query = userText, limit = 8)),
                reasoning = "Fallback plan"
            )
        } else {
            trace += TraceStep(
                "Planner: plan received",
                "useTools=${plan.useTools}, actions=${plan.actions.size}",
                TraceType.INFO
            )
            plan
        }
    }
}