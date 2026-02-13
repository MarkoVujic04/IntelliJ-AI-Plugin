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

        val plan = planner.plan(userText, config, trace)
        val evidence = scout.gather(plan, userText, trace)
        val selected = strategist.select(evidence, trace)

        val result = executor.executeFinal(userText, config, selected, trace)
        return result
    }
}