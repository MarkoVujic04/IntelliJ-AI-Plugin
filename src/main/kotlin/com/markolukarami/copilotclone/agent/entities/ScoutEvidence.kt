package com.markolukarami.copilotclone.agent.entities

import com.markolukarami.copilotclone.domain.entities.FileSnippet
import com.markolukarami.copilotclone.domain.entities.TextSnippet


data class ScoutEvidence(
    val snippets: List<TextSnippet> = emptyList(),
    val files: List<FileSnippet> = emptyList(),
)

