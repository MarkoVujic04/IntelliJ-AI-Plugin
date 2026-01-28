package com.markolukarami.copilotclone.application.usecase
import com.markolukarami.copilotclone.domain.repositories.ChatRepository
import com.markolukarami.copilotclone.domain.repositories.SettingsRepository
import com.markolukarami.copilotclone.domain.entities.ChatMessage
import com.markolukarami.copilotclone.domain.entities.ChatResult
import com.markolukarami.copilotclone.domain.entities.ChatRole
import com.markolukarami.copilotclone.domain.entities.FileSnippet
import com.markolukarami.copilotclone.domain.entities.TraceStep
import com.markolukarami.copilotclone.domain.entities.TraceType
import com.markolukarami.copilotclone.domain.repositories.EditorContextRepository
import com.markolukarami.copilotclone.domain.repositories.FileReaderRepository
import com.markolukarami.copilotclone.domain.repositories.TextSearchRepository

class ChatUseCase(
    private val chatRepository: ChatRepository,
    private val settingsRepository: SettingsRepository,
    private val editorContextRepository: EditorContextRepository,
    private val textSearchRepository: TextSearchRepository,
    private val fileReaderRepository: FileReaderRepository
) {
    fun execute(userText: String): ChatResult {
        val trace = mutableListOf<TraceStep>()

        trace += TraceStep("Load settings", "Read Base URL + Model", TraceType.INFO)
        val config = settingsRepository.getModelConfig()
        trace += TraceStep("Model config", "baseUrl=${config.baseUrl}, model=${config.model}", TraceType.INFO)

        trace += TraceStep("Read editor context", "Selection + active file", TraceType.IO)
        val context = editorContextRepository.getCurrentContext()

        val messages = mutableListOf(
            ChatMessage(
                role = ChatRole.SYSTEM,
                content = "You are a helpful coding assistant inside IntelliJ IDEA. Be direct and practical."
            )
        )

        context.selectedText?.takeIf { it.isNotBlank() }?.let { sel ->
            trace += TraceStep(
                title = "Selection detected",
                details = "chars=${sel.length}, file=${context.filePath ?: "unknown"}",
                type = TraceType.INFO,
                filePath = context.filePath
            )
            messages += ChatMessage(
                role = ChatRole.SYSTEM,
                content = "The user selected this code:\n$sel"
            )
        } ?: run {
            trace += TraceStep("No selection", "No selected text in editor", TraceType.INFO)
        }

        val query = userText.trim()
        trace += TraceStep("Search project text", "query=\"$query\"", TraceType.IO)

        val snippets = textSearchRepository.search(query, limit = 8)

        if (snippets.isEmpty()) {
            trace += TraceStep("Search result", "No matches found", TraceType.INFO)
        } else {
            trace += TraceStep("Search result", "matches=${snippets.size}", TraceType.INFO)

            val snippetText = snippets.joinToString("\n") {
                "- ${it.filePath}:${it.lineNumber}  ${it.preview}"
            }
            messages += ChatMessage(
                role = ChatRole.SYSTEM,
                content = "Project text search snippets (for context):\n$snippetText"
            )
        }

        val topFiles = snippets.map { it.filePath }.distinct().take(2)
        val readFiles = mutableListOf<FileSnippet>()

        for (path in topFiles) {
            trace += TraceStep("Read file", path, TraceType.IO, filePath = path)

            val fs = fileReaderRepository.readFile(path, maxChars = 6000)
            if (fs == null) {
                trace += TraceStep("File read failed", "Could not read file", TraceType.ERROR, filePath = path)
                continue
            }

            readFiles += fs
            trace += TraceStep("File read OK", "chars=${fs.content.length}", TraceType.INFO, filePath = path)
        }

        if (readFiles.isNotEmpty()) {
            val block = readFiles.joinToString("\n\n") { fs ->
                "FILE: ${fs.filePath}\n---\n${fs.content}\n---"
            }
            messages += ChatMessage(
                role = ChatRole.SYSTEM,
                content = "Relevant file content excerpts:\n$block"
            )
        }

        messages += ChatMessage(role = ChatRole.USER, content = userText)

        trace += TraceStep(
            "Send request to LM Studio",
            "POST /v1/chat/completions (messages=${messages.size})",
            TraceType.MODEL
        )

        val assistantText = chatRepository.chat(config, messages)

        trace += TraceStep("Receive response", "chars=${assistantText.length}", TraceType.MODEL)

        if (assistantText.startsWith("Error:", ignoreCase = true)) {
            trace += TraceStep("Model error", assistantText, TraceType.ERROR)
        }

        return ChatResult(assistantText = assistantText, trace = trace)
    }
}