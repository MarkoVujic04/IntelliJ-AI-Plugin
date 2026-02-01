package com.markolukarami.copilotclone.frameworks.llm
import kotlinx.serialization.Serializable

@Serializable
data class ToolPlanResponseDto(
    val useTools: Boolean = false,
    val reasoning: String? = null,
    val actions: List<ToolActionDto> = emptyList()
)

@Serializable
data class ToolActionDto(
    val type: String,
    val query: String? = null,
    val filePath: String? = null,
    val limit: Int? = null
)