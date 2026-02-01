package com.markolukarami.copilotclone.domain.entities

data class ToolPlan(
    val useTools: Boolean,
    val actions: List<ToolAction>,
    val reasoning: String? = null
)