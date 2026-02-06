package com.markolukarami.copilotclone.application.usecase

import com.markolukarami.copilotclone.domain.entities.ChatResult

interface ChatHandler {
    fun execute(userText: String): ChatResult
}