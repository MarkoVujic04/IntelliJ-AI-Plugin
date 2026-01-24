package com.markolukarami.copilotclone.adapters.presenters

import com.markolukarami.copilotclone.adapters.presentation.TraceViewModel
import com.markolukarami.copilotclone.domain.entities.TraceStep
import com.markolukarami.copilotclone.domain.entities.TraceType

class TracePresenter {

    fun present(steps: List<TraceStep>): TraceViewModel {
        val lines = steps.mapIndexed { index, step ->
            val prefix = when (step.type) {
                TraceType.INFO -> "ℹ"
                TraceType.IO -> "📄"
                TraceType.MODEL -> "🤖"
                TraceType.ERROR -> "❌"
            }

            val detail = step.details?.takeIf { it.isNotBlank() }
            if (detail == null) {
                "${index + 1}. $prefix ${step.title}"
            } else {
                "${index + 1}. $prefix ${step.title}\n    $detail"
            }
        }

        return TraceViewModel(lines = lines)
    }
}
