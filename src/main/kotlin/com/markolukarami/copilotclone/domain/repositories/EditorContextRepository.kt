package com.markolukarami.copilotclone.domain.repositories

import com.markolukarami.copilotclone.domain.entities.context.ChatContext

interface EditorContextRepository {
    fun getCurrentContext(): ChatContext
}
