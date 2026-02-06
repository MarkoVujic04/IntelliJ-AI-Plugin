package com.markolukarami.copilotclone.agent.entities

import com.markolukarami.copilotclone.domain.entities.FileSnippet
import com.markolukarami.copilotclone.domain.entities.TextSnippet


data class ScoutEvidence(
    val snippets: List<TextSnippet>,
    val files: List<FileSnippet>
)

