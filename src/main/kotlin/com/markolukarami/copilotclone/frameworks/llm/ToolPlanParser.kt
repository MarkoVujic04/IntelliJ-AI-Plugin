package com.markolukarami.copilotclone.frameworks.llm

import com.markolukarami.copilotclone.domain.entities.tool.ToolAction
import com.markolukarami.copilotclone.domain.entities.tool.ToolPlan
import com.markolukarami.copilotclone.domain.entities.tool.ToolType
import kotlinx.serialization.json.Json


object ToolPlanParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parseOrNull(raw: String): ToolPlan? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) return null

        return runCatching {
            val dto = json.decodeFromString(
                ToolPlanResponseDto.serializer(),
                raw.substring(start, end + 1)
            )

            val actions = dto.actions.mapNotNull {
                val type = when (it.type.uppercase()) {
                    "SEARCH_PROJECT_TEXT" -> ToolType.SEARCH_PROJECT_TEXT
                    "READ_FILE" -> ToolType.READ_FILE
                    else -> return@mapNotNull null
                }
                ToolAction(type, it.query, it.filePath, it.limit)
            }

            ToolPlan(dto.useTools, actions, dto.reasoning)
        }.getOrNull()
    }
}