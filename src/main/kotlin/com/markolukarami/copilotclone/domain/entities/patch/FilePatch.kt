package com.markolukarami.copilotclone.domain.entities.patch

data class FilePatch(
    val relativePath: String,
    val operations: List<PatchOperation> = emptyList(),
)
