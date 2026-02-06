package com.markolukarami.copilotclone.agent.entities

import com.markolukarami.copilotclone.domain.entities.FileSnippet
import com.markolukarami.copilotclone.domain.entities.TextSnippet

data class AgentContext(
    val selectedText: String?,
    val userContextFiles: List<FileSnippet>,
    val specSnippets: List<TextSnippet>
)