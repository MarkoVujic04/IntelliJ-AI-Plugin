package com.markolukarami.copilotclone.frameworks.llm

import com.intellij.openapi.diagnostic.Logger
import com.markolukarami.copilotclone.domain.entities.ChatMessage
import com.markolukarami.copilotclone.domain.entities.ChatRole
import com.markolukarami.copilotclone.domain.entities.ModelConfig
import com.markolukarami.copilotclone.domain.repositories.ChatRepository
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class OllamaAdapter : ChatRepository {

    private val log = Logger.getInstance(OllamaAdapter::class.java)

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(150, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override fun chat(modelConfig: ModelConfig, messages: List<ChatMessage>): String {
        val url = modelConfig.baseUrl.removeSuffix("/") + "/api/chat"

        val req = OllamaChatRequest(
            model = modelConfig.model,
            messages = messages.map { it.toOllama() },
            stream = false,
            options = OllamaOptions(
                temperature = modelConfig.temperature,
                num_predict = modelConfig.maxTokens
            )
        )

        val bodyStr = json.encodeToString(OllamaChatRequest.serializer(), req)
        val body = bodyStr.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        try {
            http.newCall(request).execute().use { resp ->
                val text = resp.body?.string().orEmpty()

                if (!resp.isSuccessful) {
                    log.warn("Ollama HTTP ${resp.code}: $text")
                    return "Ollama request failed (HTTP ${resp.code}). Check Base URL/model."
                }

                val lines = text.lines().filter { it.isNotBlank() }
                val contentBuilder = StringBuilder()

                for (line in lines) {
                    try {
                        val parsed = json.decodeFromString(OllamaChatResponse.serializer(), line)
                        parsed.message?.content?.let { contentBuilder.append(it) }
                    } catch (e: Exception) {
                        log.warn("Failed to parse Ollama response line: $line", e)
                    }
                }

                val content = contentBuilder.toString().trim()
                return content.takeIf { it.isNotBlank() }
                    ?: "Error: Empty response from Ollama."
            }
        } catch (e: IOException) {
            log.warn("Ollama connection error", e)
            return "Could not reach Ollama at $url. Is Ollama running?"
        } catch (e: Exception) {
            log.warn("Ollama unexpected error", e)
            return "Unexpected error: ${e.message ?: e::class.java.simpleName}"
        }
    }

    private fun ChatMessage.toOllama(): OllamaMessage {
        val roleString = when (role) {
            ChatRole.SYSTEM -> "system"
            ChatRole.USER -> "user"
            ChatRole.ASSISTANT -> "assistant"
        }

        val contentString = when (role) {
            ChatRole.SYSTEM -> {
                "### SYSTEM INSTRUCTIONS (obey strictly)\n$content"
            }
            else -> content
        }

        return OllamaMessage(role = roleString, content = contentString)
    }
}
