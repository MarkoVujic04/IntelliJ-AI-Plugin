package com.markolukarami.copilotclone.application.patch

import com.google.gson.Gson
import com.markolukarami.copilotclone.domain.entities.patch.PatchPlan

object PatchParser {

    private val gson = Gson()

    fun parseOrNull(modelText: String): PatchPlan? {
        return try {
            gson.fromJson(modelText.trim(), PatchPlan::class.java)
        } catch (e: Exception) {
            null
        }
    }
}