package com.markolukarami.copilotclone.domain.entities

data class ChatMessage(
    var role: ChatRole,
    var content: String,
)