package com.markolukarami.copilotclone.frameworks.llm

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.intellij.util.io.HttpRequests
import com.markolukarami.copilotclone.domain.entities.ModelConfig
import com.markolukarami.copilotclone.domain.entities.model.ModelInfo
import com.markolukarami.copilotclone.domain.repositories.ModelRegistryRepository

class LMStudioModelRegistryAdapter : ModelRegistryRepository {

    private val gson = Gson()

    override fun listModels(config: ModelConfig): List<ModelInfo> {
        val base = config.baseUrl.trimEnd('/')
        val url = "$base/v1/models"

        return try {
            val body = HttpRequests.request(url)
                .tuner { connection ->
                    connection.connectTimeout = 3000
                    connection.readTimeout = 5000
                    connection.setRequestProperty("Accept", "application/json")
                }
                .readString()

            parseOpenAiModels(body)
        } catch (t: Throwable) {
            println("LMStudioModelRegistryAdapter error: ${t.message}")
            emptyList()
        }
    }

    private fun parseOpenAiModels(body: String): List<ModelInfo> {
        val root = try {
            gson.fromJson(body, JsonObject::class.java)
        } catch (_: Throwable) {
            return emptyList()
        } ?: return emptyList()

        val data = root.getAsJsonArray("data") ?: return emptyList()

        return data.mapNotNull { el ->
            val obj = el.asJsonObject
            val id = obj.get("id")?.asString ?: return@mapNotNull null
            ModelInfo(id = id)
        }
    }
}
