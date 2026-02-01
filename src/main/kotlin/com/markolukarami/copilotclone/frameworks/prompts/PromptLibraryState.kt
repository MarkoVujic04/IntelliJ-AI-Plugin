package com.markolukarami.copilotclone.frameworks.prompts

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.markolukarami.copilotclone.domain.entities.prompts.SavedPrompt
import com.markolukarami.copilotclone.domain.repositories.PromptRepository
import java.util.UUID

@State(
    name = "CopilotClonePromptLibrary",
    storages = [Storage("copilotclone-prompts.xml")]
)
@Service(Service.Level.PROJECT)
class PromptLibraryState : PersistentStateComponent<PromptLibraryState.StateDto>, PromptRepository {

    data class PromptDto(
        var id: String = "",
        var title: String = "",
        var text: String = "",
        var createdAtEpochMs: Long = 0L
    )

    data class StateDto(
        var prompts: MutableList<PromptDto> = mutableListOf()
    )

    private var state = StateDto()

    override fun getState(): StateDto = state

    override fun loadState(state: StateDto) {
        this.state = state
    }

    override fun list(): List<SavedPrompt> =
        state.prompts
            .sortedByDescending { it.createdAtEpochMs }
            .map { SavedPrompt(it.id, it.title, it.text, it.createdAtEpochMs) }

    override fun add(title: String, text: String): SavedPrompt {
        val p = PromptDto(
            id = UUID.randomUUID().toString(),
            title = title.trim().ifBlank { "Saved prompt" },
            text = text,
            createdAtEpochMs = System.currentTimeMillis()
        )
        state.prompts.add(0, p)
        return SavedPrompt(p.id, p.title, p.text, p.createdAtEpochMs)
    }

    override fun delete(id: String) {
        state.prompts.removeIf { it.id == id }
    }

    override fun clear() {
        state.prompts.clear()
    }
}
