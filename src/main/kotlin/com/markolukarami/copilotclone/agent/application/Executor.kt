package com.markolukarami.copilotclone.agent.application

import com.markolukarami.copilotclone.domain.entities.ChatMessage
import com.markolukarami.copilotclone.domain.entities.ChatRole
import com.markolukarami.copilotclone.domain.entities.ModelConfig
import com.markolukarami.copilotclone.domain.entities.trace.TraceStep
import com.markolukarami.copilotclone.domain.entities.trace.TraceType
import com.markolukarami.copilotclone.domain.repositories.ChatRepository
import com.markolukarami.copilotclone.domain.repositories.EditorContextRepository

class Executor(
    private val chatRepository: ChatRepository,
    private val editorContextRepository: EditorContextRepository
) {

    fun executeFinal(
        userText: String,
        config: ModelConfig,
        evidence: Strategist.SelectedEvidence,
        trace: MutableList<TraceStep>
    ): String {
        trace += TraceStep("Agent: Executor", "Compose final prompt", TraceType.MODEL)

        val finalMessages = mutableListOf(
            ChatMessage(ChatRole.SYSTEM, "You are a helpful IntelliJ coding assistant.")
        )

        val editorContext = editorContextRepository.getCurrentContext()
        editorContext.selectedText?.let {
            finalMessages += ChatMessage(ChatRole.SYSTEM, "Selected code:\n$it")
        }

        if (evidence.snippets.isNotEmpty()) {
            finalMessages += ChatMessage(
                ChatRole.SYSTEM,
                "Project snippets:\n" + evidence.snippets.joinToString("\n") {
                    "- ${it.filePath}:${it.lineNumber} ${it.preview}"
                }
            )
        }

        if (evidence.files.isNotEmpty()) {
            finalMessages += ChatMessage(
                ChatRole.SYSTEM,
                "File excerpts:\n" + evidence.files.joinToString("\n\n") {
                    "FILE ${it.filePath}\n${it.content}"
                }
            )
        }

        finalMessages += ChatMessage(ChatRole.USER, userText)

        trace += TraceStep("Model: send final request", type = TraceType.MODEL)
        val answer = chatRepository.chat(config, finalMessages)
        trace += TraceStep("Model: receive response", type = TraceType.MODEL)

        return answer
    }
}