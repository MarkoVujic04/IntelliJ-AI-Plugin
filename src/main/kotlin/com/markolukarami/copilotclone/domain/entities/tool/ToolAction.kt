package com.markolukarami.copilotclone.domain.entities.tool

data class ToolAction(
    val type: ToolType,
    val query: String? = null,
    val filePath: String? = null,
    val limit: Int? = null
)