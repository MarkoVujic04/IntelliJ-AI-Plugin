package com.markolukarami.copilotclone.domain.entities.patch

data class TextEdit(
    val start: Int,
    val end: Int,
    val text: String
)
