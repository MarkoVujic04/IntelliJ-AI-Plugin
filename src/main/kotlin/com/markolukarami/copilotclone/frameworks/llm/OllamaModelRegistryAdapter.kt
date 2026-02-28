package com.markolukarami.copilotclone.frameworks.llm

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.intellij.util.io.HttpRequests
import com.markolukarami.copilotclone.domain.entities.ModelConfig
import com.markolukarami.copilotclone.domain.entities.model.ModelInfo
import com.markolukarami.copilotclone.domain.repositories.ModelRegistryRepository

class OllamaModelRegistryAdapter : ModelRegistryRepository {

    private val gson = Gson()

    override fun listModels(config: ModelConfig): List<ModelInfo> {
        val base = config.baseUrl.trimEnd('/')
        val url = "$base/api/tags"

        return try {
            val body = HttpRequests.request(url)
                .tuner { connection ->
                    connection.connectTimeout = 3000
                    connection.readTimeout = 5000
                    connection.setRequestProperty("Accept", "application/json")
                }
                .readString()

            parseOllamaModels(body)
        } catch (t: Throwable) {
            println("OllamaModelRegistryAdapter error: ${t.message}")
            emptyList()
        }
    }

    private fun parseOllamaModels(body: String): List<ModelInfo> {
        val root = try {
            gson.fromJson(body, JsonObject::class.java)
        } catch (_: Throwable) {
            return emptyList()
        } ?: return emptyList()

        val models = root.getAsJsonArray("models") ?: return emptyList()

        return models.mapNotNull { el ->
            val obj = el.asJsonObject
            val name = obj.get("name")?.asString ?: return@mapNotNull null
            ModelInfo(id = name)
        }
    }
}
