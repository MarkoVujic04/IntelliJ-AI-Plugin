package com.markolukarami.copilotclone.domain.entities.trace

import com.markolukarami.copilotclone.domain.entities.trace.TraceType

data class TraceStep(
    val title: String,
    val details: String? = null,
    val type: TraceType = TraceType.INFO,
    val filePath: String? = null,
)