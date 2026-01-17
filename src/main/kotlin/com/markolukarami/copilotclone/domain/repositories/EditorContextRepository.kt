package com.markolukarami.copilotclone.domain.repositories

import com.markolukarami.copilotclone.domain.entities.ChatContext

interface EditorContextRepository {
    fun getCurrentContext(): ChatContext
}
