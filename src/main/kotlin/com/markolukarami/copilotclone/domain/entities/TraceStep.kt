package com.markolukarami.copilotclone.domain.entities

data class TraceStep(
    val title: String,
    val details: String? = null,
    val type: TraceType = TraceType.INFO,
    val filePath: String? = null,
)
