package com.markolukarami.copilotclone.ui

import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.panel
import com.markolukarami.copilotclone.domain.entities.ContextFile
import com.markolukarami.copilotclone.domain.repositories.UserContextRepository
import com.markolukarami.copilotclone.frameworks.editor.UserContextState
import java.awt.BorderLayout
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.ListSelectionModel

class ContextManagerDialog(
    private val project: Project,
    private val onChanged: (() -> Unit)? = null
) {

    private val repo: UserContextRepository =
        project.service<UserContextState>()

    private val model = DefaultListModel<String>()
    private val list = JBList(model).apply {
        selectionMode = ListSelectionModel.SINGLE_SELECTION
        visibleRowCount = 10
    }

    val component: JComponent = JPanel(BorderLayout()).apply {
        add(JBScrollPane(list), BorderLayout.CENTER)
        add(buildButtons(), BorderLayout.SOUTH)
    }

    init {
        reload()
    }

    private fun buildButtons(): JComponent =
        panel {
            row {
                button("Add file…") {
                    val vf = ContextFilePicker.pickSingleProjectFile(project) ?: return@button
                    addPath(vf.path)
                }

                button("Add current file") {
                    val current =
                        FileEditorManager.getInstance(project).selectedFiles.firstOrNull()
                            ?: return@button
                    addPath(current.path)
                }

                button("Remove selected") {
                    val selected = list.selectedValue ?: return@button
                    removePath(selected)
                }

                button("Clear") {
                    repo.clear()
                    reload()
                    onChanged?.invoke()
                }
            }
        }

    private fun reload() {
        model.clear()
        repo.getSelectedContextFiles()
            .map { it.path }
            .distinct()
            .forEach { model.addElement(it) }
    }

    private fun addPath(path: String) {
        val existing = repo.getSelectedContextFiles().map { it.path }.toMutableList()
        if (path.isBlank()) return
        if (!existing.contains(path)) existing.add(path)

        repo.setSelectedContextFiles(existing.map { ContextFile(it) })
        reload()
        onChanged?.invoke()
    }

    private fun removePath(path: String) {
        val remaining = repo.getSelectedContextFiles()
            .map { it.path }
            .filter { it != path }
            .map { ContextFile(it) }

        repo.setSelectedContextFiles(remaining)
        reload()
        onChanged?.invoke()
    }
}