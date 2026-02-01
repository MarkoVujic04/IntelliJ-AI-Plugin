package com.markolukarami.copilotclone.ui

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.markolukarami.copilotclone.domain.entities.prompts.SavedPrompt
import com.markolukarami.copilotclone.domain.repositories.PromptRepository
import com.markolukarami.copilotclone.frameworks.prompts.PromptLibraryState
import java.awt.BorderLayout
import java.awt.Component
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JPopupMenu

object PromptLibraryPopup {

    fun show(
        project: Project,
        anchor: Component,
        currentInput: () -> String,
        onPick: (String) -> Unit
    ) {
        val repo: PromptRepository = project.service<PromptLibraryState>()

        val model = DefaultListModel<SavedPrompt>()
        repo.list().forEach { model.addElement(it) }

        val list = JBList(model).apply {
            visibleRowCount = 8
            cellRenderer = object : ColoredListCellRenderer<SavedPrompt>() {
                override fun customizeCellRenderer(
                    list: JList<out SavedPrompt>,
                    value: SavedPrompt?,
                    index: Int,
                    selected: Boolean,
                    hasFocus: Boolean
                ) {
                    if (value == null) return
                    append(value.title, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
                    val preview = value.text.replace("\n", " ").take(80)
                    if (preview.isNotBlank()) {
                        append("  ", SimpleTextAttributes.REGULAR_ATTRIBUTES)
                        append(preview, SimpleTextAttributes.GRAYED_ATTRIBUTES)
                    }
                }
            }
        }

        list.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    val selected = list.selectedValue ?: return
                    onPick(selected.text)
                }
            }
        })

        list.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (e.button != MouseEvent.BUTTON3) return
                val idx = list.locationToIndex(e.point)
                if (idx < 0) return
                list.selectedIndex = idx
                val selected = list.selectedValue ?: return

                val menu = JPopupMenu()
                menu.add("Delete").addActionListener {
                    repo.delete(selected.id)
                    model.removeElement(selected)
                }
                menu.show(list, e.x, e.y)
            }
        })

        val saveButton = JButton("Save current").apply {
            addActionListener {
                val text = currentInput().trim()
                if (text.isBlank()) {
                    Messages.showInfoMessage(project, "Nothing to save.", "Saved Prompts")
                    return@addActionListener
                }

                val title = Messages.showInputDialog(
                    project,
                    "Name this prompt:",
                    "Save Prompt",
                    null,
                    text.lines().firstOrNull()?.take(40) ?: "Saved prompt",
                    null
                ) ?: return@addActionListener

                val saved = repo.add(title, text)
                model.add(0, saved)
                list.selectedIndex = 0
            }
        }

        val clearButton = JButton("Clear all").apply {
            addActionListener {
                repo.clear()
                model.clear()
            }
        }

        val panel = JPanel(BorderLayout(8, 8)).apply {
            add(JBScrollPane(list), BorderLayout.CENTER)
            add(
                JPanel().apply {
                    add(saveButton)
                    add(clearButton)
                },
                BorderLayout.SOUTH
            )
        }

        JBPopupFactory.getInstance()
            .createComponentPopupBuilder(panel, list)
            .setTitle("Saved prompts")
            .setResizable(true)
            .setMovable(true)
            .createPopup()
            .showUnderneathOf(anchor)
    }
}