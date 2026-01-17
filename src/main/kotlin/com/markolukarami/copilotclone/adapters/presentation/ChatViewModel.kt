package com.markolukarami.copilotclone.adapters.presentation

enum class ChatType { USER, ASSISTANT, SYSTEM }

data class ChatViewModel(
    val displayText: String,
    val type: ChatType
)
