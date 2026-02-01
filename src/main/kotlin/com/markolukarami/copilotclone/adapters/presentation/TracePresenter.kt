package com.markolukarami.copilotclone.adapters.presentation
import com.markolukarami.copilotclone.domain.entities.TraceStep
import com.markolukarami.copilotclone.domain.entities.TraceType

class TracePresenter {

    fun present(steps: List<TraceStep>): TraceViewModel {
        val lines = steps.mapIndexed { i, s ->
            val icon = when (s.type) {
                TraceType.INFO -> "ℹ"
                TraceType.IO -> "📄"
                TraceType.MODEL -> "🤖"
                TraceType.TOOL -> "🛠"
                TraceType.ERROR -> "❌"
            }

            TraceLineVM(
                text = "${i + 1}. $icon ${s.title}${s.details?.let { "\n   $it" } ?: ""}",
                filePath = s.filePath
            )
        }
        return TraceViewModel(lines)
    }
}
