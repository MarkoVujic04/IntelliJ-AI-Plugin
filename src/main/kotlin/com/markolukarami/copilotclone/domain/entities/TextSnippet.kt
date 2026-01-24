package com.markolukarami.copilotclone.domain.entities

data class TextSnippet(
    val filePath: String,
    val lineNumber: Int,
    val preview: String,
)
