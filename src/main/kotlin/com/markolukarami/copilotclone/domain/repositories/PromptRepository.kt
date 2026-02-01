package com.markolukarami.copilotclone.domain.repositories

import com.markolukarami.copilotclone.domain.entities.prompts.SavedPrompt

interface PromptRepository {
    fun list(): List<SavedPrompt>
    fun add(title: String, text: String): SavedPrompt
    fun delete(id: String)
    fun clear()
}