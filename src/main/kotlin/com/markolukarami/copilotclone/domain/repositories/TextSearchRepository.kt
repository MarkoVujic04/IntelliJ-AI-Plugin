package com.markolukarami.copilotclone.domain.repositories

import com.markolukarami.copilotclone.domain.entities.TextSnippet

interface TextSearchRepository {
    fun search(query: String, limit: Int = 8): List<TextSnippet>
}