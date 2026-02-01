package com.markolukarami.copilotclone.ui.components

data class ContextListItem(
    val fullPath: String,
    val fileName: String,
    val relativePath: String? = null
)