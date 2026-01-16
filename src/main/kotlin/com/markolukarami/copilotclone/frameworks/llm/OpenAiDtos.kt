package com.markolukarami.aiplugin.frameworks.lmstudio

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenAiChatRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val temperature: Double? = 0.2,
    @SerialName("max_tokens") val maxTokens: Int? = null,
    val stream: Boolean? = false
)

@Serializable
data class OpenAiMessage(
    val role: String,
    val content: String
)

@Serializable
data class OpenAiChatResponse(
    val choices: List<Choice> = emptyList()
) {
    @Serializable
    data class Choice(val message: Message? = null)

    @Serializable
    data class Message(
        val role: String? = null,
        val content: String? = null
    )
}
