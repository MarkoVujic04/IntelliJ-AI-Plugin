package com.markolukarami.copilotclone.domain.model

data class ChatMessage(
    var role: ChatRole,
    var content: String,
)
