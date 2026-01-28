package com.markolukarami.copilotclone.ui

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.markolukarami.copilotclone.adapters.presentation.TraceLineVM
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.JPanel

class TracePanel(private val project: Project) {

    private val model = DefaultListModel<TraceLineVM>()
    private val list = JBList(model).apply {
        cellRenderer = TraceRenderer()
    }

    val component: JComponent = JPanel(BorderLayout()).apply {
        add(JBScrollPane(list), BorderLayout.CENTER)
    }

    init {
        list.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount != 2) return
                val idx = list.selectedIndex
                if (idx < 0) return

                val item = model.get(idx)
                val path = item.filePath ?: return

                val vf = LocalFileSystem.getInstance().findFileByPath(path) ?: return
                FileEditorManager.getInstance(project).openFile(vf, true)
            }
        })
    }

    fun setTraceLines(lines: List<TraceLineVM>) {
        model.clear()
        if (lines.isEmpty()) {
            model.addElement(TraceLineVM("Trace will appear here after you send a message."))
            return
        }
        lines.forEach { model.addElement(it) }
    }
}
