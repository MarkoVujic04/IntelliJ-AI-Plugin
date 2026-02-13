package com.markolukarami.copilotclone.domain.entities.patch

data class PatchPlan(
    val summary: String = "",
    val files: List<FilePatch> = emptyList(),
)
