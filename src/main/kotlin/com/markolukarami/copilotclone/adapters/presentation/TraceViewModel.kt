package com.markolukarami.copilotclone.adapters.presentation

data class TraceLineVM(
    val text: String,
    val filePath: String? = null
)


data class TraceViewModel(
    val lines: List<TraceLineVM>,
)
