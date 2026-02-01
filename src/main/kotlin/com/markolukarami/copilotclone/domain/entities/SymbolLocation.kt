package com.markolukarami.copilotclone.domain.entities

data class SymbolLocation(
    val symbol: String,
    val filePath: String,
    val lineNumber: Int? = null,
    val evidenceLine: String? = null
)