package com.markolukarami.copilotclone.application.usecase

import com.markolukarami.copilotclone.agent.application.Executor
import com.markolukarami.copilotclone.agent.application.Planner
import com.markolukarami.copilotclone.agent.application.Scout
import com.markolukarami.copilotclone.agent.application.Strategist
import com.markolukarami.copilotclone.domain.entities.ChatResult
import com.markolukarami.copilotclone.domain.entities.trace.TraceStep
import com.markolukarami.copilotclone.domain.repositories.SettingsRepository

class AgentPipelineUseCase(
    private val settingsRepository: SettingsRepository,
    private val planner: Planner,
    private val scout: Scout,
    private val strategist: Strategist,
    private val executor: Executor
) : ChatHandler {

    override fun execute(userText: String): ChatResult {

        val trace = mutableListOf<TraceStep>()
        val config = settingsRepository.getModelConfig()

        val patchMode = isPatchRequestAgent(userText)

        if (patchMode) {
            val evidence = scout.gather(
                plan = null,
                userText = userText,
                trace = trace
            )

            val selected = strategist.select(evidence, trace)

            return executor.executeFinal(
                userText = userText,
                config = config,
                evidence = selected,
                trace = trace
            )
        }

        val plan = planner.plan(userText, config, trace)
        val evidence = scout.gather(plan, userText, trace)
        val selected = strategist.select(evidence, trace)

        return executor.executeFinal(userText, config, selected, trace)
    }

    private fun isPatchRequestAgent(text: String): Boolean {
        val t = text.lowercase()
        return t.startsWith("apply") || t.contains("apply this") || t.contains("make this change")
    }
}