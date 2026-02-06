package com.markolukarami.copilotclone.agent.entities

data class AgentDecision(
    val shouldUseSpecSearch: Boolean,
    val maxSnippets: Int = 6,
    val maxCharsPerFile: Int = 2500
)
