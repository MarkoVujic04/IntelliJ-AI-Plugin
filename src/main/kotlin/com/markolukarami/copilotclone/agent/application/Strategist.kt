package com.markolukarami.copilotclone.agent.application

import com.markolukarami.copilotclone.agent.entities.ScoutEvidence
import com.markolukarami.copilotclone.domain.entities.FileSnippet
import com.markolukarami.copilotclone.domain.entities.TextSnippet
import com.markolukarami.copilotclone.domain.entities.trace.TraceStep
import com.markolukarami.copilotclone.domain.entities.trace.TraceType

class Strategist(
    private val maxSnippets: Int = 8,
    private val maxFileChars: Int = 1500,
    private val maxFiles: Int = 5
) {
    data class SelectedEvidence(
        val snippets: List<TextSnippet>,
        val files: List<FileSnippet>
    )
    fun select(evidence: ScoutEvidence, trace: MutableList<TraceStep>): SelectedEvidence {
        trace += TraceStep("Agent: Strategist", "Trim evidence to limits", TraceType.INFO)

        val chosenSnippets = evidence.snippets.take(maxSnippets)
        val chosenFiles = evidence.files
            .distinctBy { it.filePath }
            .take(maxFiles)
            .map { it.copy(content = it.content.take(maxFileChars)) }

        trace += TraceStep(
            "Strategist: evidence selected",
            "snippets=${chosenSnippets.size}, files=${chosenFiles.size}",
            TraceType.INFO
        )

        return SelectedEvidence(chosenSnippets, chosenFiles)
    }
}