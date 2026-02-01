package com.markolukarami.copilotclone.domain.entities.tool

import com.markolukarami.copilotclone.domain.entities.tool.ToolAction

data class ToolPlan(
    val useTools: Boolean,
    val actions: List<ToolAction>,
    val reasoning: String? = null
)