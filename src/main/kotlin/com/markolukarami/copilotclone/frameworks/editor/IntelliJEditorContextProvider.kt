package com.markolukarami.copilotclone.frameworks.editor

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.markolukarami.copilotclone.domain.entities.ChatContext
import com.markolukarami.copilotclone.domain.repositories.EditorContextRepository

class IntelliJEditorContextProvider(
    private val project: Project
) : EditorContextRepository {

    override fun getCurrentContext(): ChatContext {
        return ReadAction.compute<ChatContext, RuntimeException> {

            val editor = FileEditorManager.getInstance(project).selectedTextEditor
            val selectedText = editor?.selectionModel?.selectedText

            val virtualFile = editor?.document?.let { doc ->
                FileDocumentManager.getInstance().getFile(doc)
            }

            ChatContext(
                selectedText = selectedText,
                filePath = virtualFile?.path
            )
        }
    }
}
