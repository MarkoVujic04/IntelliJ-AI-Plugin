package com.markolukarami.copilotclone.ui

import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.panel
import com.markolukarami.copilotclone.domain.entities.context.ContextFile
import com.markolukarami.copilotclone.domain.repositories.UserContextRepository
import com.markolukarami.copilotclone.frameworks.editor.UserContextState
import com.markolukarami.copilotclone.ui.components.ContextListItem
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

    private val model = DefaultListModel<ContextListItem>()
    private val list = JBList(model).apply {
        selectionMode = ListSelectionModel.SINGLE_SELECTION
        visibleRowCount = 10

        cellRenderer = object : ColoredListCellRenderer<ContextListItem>() {
            override fun customizeCellRenderer(
                list: javax.swing.JList<out ContextListItem>,
                value: ContextListItem?,
                index: Int,
                selected: Boolean,
                hasFocus: Boolean
            ) {
                if (value == null) return

                val vf = LocalFileSystem.getInstance().findFileByPath(value.fullPath)
                icon = vf?.fileType?.icon

                append(value.fileName, SimpleTextAttributes.REGULAR_ATTRIBUTES)

                value.relativePath?.takeIf { it.isNotBlank() }?.let {
                    append("  ", SimpleTextAttributes.REGULAR_ATTRIBUTES)
                    append(it, SimpleTextAttributes.GRAYED_ATTRIBUTES)
                }
            }
        }
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
                    val current = FileEditorManager.getInstance(project).selectedFiles.firstOrNull()
                        ?: return@button
                    addPath(current.path)
                }

                button("Remove selected") {
                    val selected = list.selectedValue ?: return@button
                    removePath(selected.fullPath)
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

        val base = project.basePath?.replace('\\', '/')

        repo.getSelectedContextFiles()
            .map { it.path }
            .distinct()
            .forEach { full ->
                val normalized = full.replace('\\', '/')
                val name = normalized.substringAfterLast('/')
                val rel = base?.let { b ->
                    if (normalized.startsWith(b)) normalized.removePrefix(b).trimStart('/')
                    else null
                }

                model.addElement(
                    ContextListItem(
                        fullPath = full,
                        fileName = name,
                        relativePath = rel
                    )
                )
            }
    }

    private fun addPath(path: String) {
        val existing = repo.getSelectedContextFiles().map { it.path }.toMutableList()
        if (path.isBlank()) return
        if (!existing.contains(path)) existing.add(path)

        repo.setSelectedContextFiles(existing.map { ContextFile(it) })
        reload()
        onChanged?.invoke()
    }

    private fun removePath(fullPath: String) {
        val remaining = repo.getSelectedContextFiles()
            .map { it.path }
            .filter { it != fullPath }
            .map { ContextFile(it) }

        repo.setSelectedContextFiles(remaining)
        reload()
        onChanged?.invoke()
    }
}