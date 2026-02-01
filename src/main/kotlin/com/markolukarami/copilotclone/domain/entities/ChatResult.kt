package com.markolukarami.copilotclone.domain.entities

import com.markolukarami.copilotclone.domain.entities.trace.TraceStep

data class ChatResult(
    val assistantText: String,
    val trace: List<TraceStep>,
)
