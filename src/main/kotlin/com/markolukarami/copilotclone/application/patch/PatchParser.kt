package com.markolukarami.copilotclone.application.patch

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.markolukarami.copilotclone.domain.entities.patch.PatchPlan

object PatchParser {

    private val gson = Gson()

    fun parseOrNull(modelText: String): PatchPlan? {
        val candidates = listOfNotNull(
            modelText.trim(),
            extractJsonObject(modelText)
        )

        for (candidate in candidates) {
            try {
                val parsed = gson.fromJson(candidate, PatchPlan::class.java)
                if (!parsed?.files.isNullOrEmpty()) return parsed

                val hasAnyEdits = parsed.files.any { it.edits.isNotEmpty() }
                if (hasAnyEdits) return parsed
            } catch (_: JsonSyntaxException) {
            }
        }

        return null
    }

    private fun extractJsonObject(text: String): String? {
        val start = text.indexOf('{')
        if (start == -1) return null

        var depth = 0
        for (i in start until text.length) {
            when (text[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) {
                        return text.substring(start, i + 1)
                    }
                }
            }
        }
        return null
    }
}