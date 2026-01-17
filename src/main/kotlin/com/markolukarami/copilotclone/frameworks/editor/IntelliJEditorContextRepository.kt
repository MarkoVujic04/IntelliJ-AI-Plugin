package com.markolukarami.copilotclone.frameworks.editor

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.editor.Editor
import com.markolukarami.copilotclone.domain.entities.ChatContext
import com.markolukarami.copilotclone.domain.repositories.EditorContextRepository

class IntelliJEditorContextRepository(
    private val project: Project
) : EditorContextRepository {

    override fun getCurrentContext(): ChatContext {
        val fileEditorManager = FileEditorManager.getInstance(project)
        val editor: Editor? = fileEditorManager.selectedTextEditor

        val selectedText = editor?.selectionModel?.selectedText
        val filePath = fileEditorManager.selectedFiles
            .firstOrNull()
            ?.path

        return ChatContext(
            selectedText = selectedText,
            filePath = filePath
        )
    }
}
