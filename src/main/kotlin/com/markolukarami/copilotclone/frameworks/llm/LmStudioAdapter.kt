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

class LMStudioAdapter : ChatRepository {

    private val log = Logger.getInstance(LMStudioAdapter::class.java)

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
        val url = modelConfig.baseUrl.removeSuffix("/") + "/v1/chat/completions"

        val req = OpenAiChatRequest(
            model = modelConfig.model,
            messages = messages.map { it.toOpenAi() },
            temperature = modelConfig.temperature,
            maxTokens = modelConfig.maxTokens,
        )

        val bodyStr = json.encodeToString(OpenAiChatRequest.serializer(), req)
        val body = bodyStr.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        try {
            http.newCall(request).execute().use { resp ->
                val text = resp.body?.string().orEmpty()

                if (!resp.isSuccessful) {
                    log.warn("LLM HTTP ${resp.code}: $text")
                    return "LLM request failed (HTTP ${resp.code}). Check Base URL/model."
                }

                val parsed = json.decodeFromString(OpenAiChatResponse.serializer(), text)
                val content = parsed.choices.firstOrNull()?.message?.content?.trim()

                return content?.takeIf { it.isNotBlank() }
                    ?: "Error: Empty response from LLM."
            }
        } catch (e: IOException) {
            log.warn("LLM connection error", e)
            return "Could not reach LLM at $url. Is LM Studio/Ollama running?"
        } catch (e: Exception) {
            log.warn("LLM unexpected error", e)
            return "Unexpected error: ${e.message ?: e::class.java.simpleName}"
        }
    }

    private fun ChatMessage.toOpenAi(): OpenAiMessage {
        val roleString = when (role) {
            ChatRole.SYSTEM -> "user"
            ChatRole.USER -> "user"
            ChatRole.ASSISTANT -> "assistant"
        }

        val contentString = when (role) {
            ChatRole.SYSTEM -> {
                "### SYSTEM INSTRUCTIONS (obey strictly)\n$content"
            }
            else -> content
        }

        return OpenAiMessage(role = roleString, content = contentString)
    }

}
