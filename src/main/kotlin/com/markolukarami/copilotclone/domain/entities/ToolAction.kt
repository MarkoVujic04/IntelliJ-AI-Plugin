package com.markolukarami.copilotclone.domain.entities

data class ToolAction(
    val type: ToolType,
    val query: String? = null,
    val filePath: String? = null,
    val limit: Int? = null
)