package com.markolukarami.copilotclone.domain.entities.prompts

data class SavedPrompt(
    val id: String,
    val title: String,
    val text: String,
    val createdAtEpochMs: Long
)