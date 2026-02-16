package com.markolukarami.copilotclone.application.patch

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.markolukarami.copilotclone.domain.entities.patch.PatchPlan

object PatchParser {

    private val gson = Gson()

    fun parseOrNull(modelText: String): PatchPlan? {
        val json = extractFirstJsonObject(modelText) ?: return null
        return try {
            JsonParser.parseString(json)
            gson.fromJson(json, PatchPlan::class.java)
        } catch (_: Exception) {
            null
        }
    }

    private fun extractFirstJsonObject(text: String): String? {
        val s = text.trim()
        val start = s.indexOf('{')
        if (start == -1) return null

        var depth = 0
        var inString = false
        var escape = false

        for (i in start until s.length) {
            val c = s[i]

            if (escape) {
                escape = false
                continue
            }
            if (c == '\\') {
                if (inString) escape = true
                continue
            }
            if (c == '"') inString = !inString
            if (inString) continue

            if (c == '{') depth++
            if (c == '}') depth--

            if (depth == 0) {
                return s.substring(start, i + 1)
            }
        }
        return null
    }
}
