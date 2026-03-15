package com.markolukarami.copilotclone.agent.application

object ModelTokenEstimator {

    private val contextWindows: List<Pair<Regex, Int>> = listOf(
        Regex("gpt-4o") to 128_000,
        Regex("gpt-4-turbo") to 128_000,
        Regex("gpt-4") to 8_192,
        Regex("gpt-3\\.5") to 16_385,
        Regex("claude") to 200_000,
        Regex("deepseek") to 128_000,
        Regex("codestral") to 32_000,
        Regex("devstral") to 32_000,
        Regex("mistral") to 32_000,
        Regex("mixtral") to 32_000,
        Regex("llama.*70") to 128_000,
        Regex("llama") to 8_192,
        Regex("qwen.*coder") to 32_000,
        Regex("qwen") to 32_000,
        Regex("phi") to 16_000,
        Regex("gemma") to 8_192,
        Regex("starcoder") to 16_000,
        Regex("codellama") to 16_000,
    )

    private const val DEFAULT_CONTEXT_WINDOW = 8_192

    private const val MIN_RESPONSE_TOKENS = 512

    private const val MAX_RESPONSE_TOKENS = 8_192

    private const val RESPONSE_FRACTION = 0.30

    fun estimateTokens(text: String): Int = (text.length / 4).coerceAtLeast(1)

    fun getContextWindow(modelName: String): Int {
        val lower = modelName.lowercase()
        return contextWindows
            .firstOrNull { it.first.containsMatchIn(lower) }
            ?.second
            ?: DEFAULT_CONTEXT_WINDOW
    }

    fun compute(
        userOverride: Int,
        modelName: String,
        promptText: String,
        fileCount: Int = 1
    ): Int {
        if (userOverride > 0) return userOverride

        val contextWindow = getContextWindow(modelName)
        val promptTokens = estimateTokens(promptText)
        val remaining = (contextWindow - promptTokens).coerceAtLeast(MIN_RESPONSE_TOKENS)

        val fileScale = 1.0 + (fileCount - 1) * 0.15
        val budget = (remaining * RESPONSE_FRACTION * fileScale).toInt()

        return budget.coerceIn(MIN_RESPONSE_TOKENS, MAX_RESPONSE_TOKENS)
    }
}

