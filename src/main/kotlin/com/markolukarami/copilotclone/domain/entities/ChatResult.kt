package com.markolukarami.copilotclone.domain.entities

data class ChatResult(
    val assistantText: String,
    val trace: List<TraceStep>,
)
