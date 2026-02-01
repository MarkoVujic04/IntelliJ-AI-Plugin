package com.markolukarami.copilotclone.frameworks.editor

import com.intellij.openapi.components.Service
import com.markolukarami.copilotclone.domain.entities.ContextFile
import com.markolukarami.copilotclone.domain.repositories.UserContextRepository

@Service(Service.Level.PROJECT)
class UserContextState : UserContextRepository {

    private var selected: List<ContextFile> = emptyList()

    override fun getSelectedContextFiles(): List<ContextFile> = selected

    override fun setSelectedContextFiles(files: List<ContextFile>) {
        selected = files
            .map { ContextFile(it.path.trim()) }
            .filter { it.path.isNotBlank() }
            .distinctBy { it.path }
    }

    override fun clear() {
        selected = emptyList()
    }
}