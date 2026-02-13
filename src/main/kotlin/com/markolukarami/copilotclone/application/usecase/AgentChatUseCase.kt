package com.markolukarami.copilotclone.application.usecase

import com.markolukarami.copilotclone.domain.entities.ChatResult
import com.markolukarami.copilotclone.domain.entities.trace.TraceStep
import com.markolukarami.copilotclone.domain.entities.trace.TraceType

class AgentChatUseCase(
    private val core: ChatHandler
) : ChatHandler {

    override fun execute(userText: String): ChatResult {
        val result = core.execute(userText)

        val newTrace = mutableListOf<TraceStep>()
        newTrace += TraceStep("Agent entrypoint", "AgentChatUseCase did the run", TraceType.INFO)
        newTrace += result.trace

        return result.copy(trace = newTrace)
    }
}