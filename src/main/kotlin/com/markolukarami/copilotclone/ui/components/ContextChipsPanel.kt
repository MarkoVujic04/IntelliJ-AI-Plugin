package com.markolukarami.copilotclone.ui.components

import com.intellij.icons.AllIcons
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.markolukarami.copilotclone.frameworks.editor.UserContextState
import java.awt.Cursor
import java.awt.FlowLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.border.EmptyBorder

class ContextChipsPanel(
    private val userContextState: UserContextState,
    private val onRemoveFile: (String) -> Unit
) {

    private val contextChipsRow = JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(6), 0)).apply {
        isOpaque = false
        border = EmptyBorder(0, 0, 0, 0)
    }

    val component: JComponent
        get() = contextChipsRow

    fun refresh() {
        contextChipsRow.removeAll()

        val selected = userContextState.getSelectedContextFiles()
        if (selected.isEmpty()) {
            contextChipsRow.add(makeStateChip("No context"))
        } else {
            contextChipsRow.add(makeStateChip("Context"))
            selected.take(8).forEach { cf ->
                contextChipsRow.add(makeFileChip(cf))
            }
            if (selected.size > 8) {
                contextChipsRow.add(makeStateChip("+${selected.size - 8} more"))
            }
        }

        contextChipsRow.revalidate()
        contextChipsRow.repaint()
    }

    private fun makeStateChip(text: String): JComponent {
        return JBLabel(text, SwingConstants.CENTER).apply {
            border = JBUI.Borders.compound(
                JBUI.Borders.customLine(JBColor.border(), 1),
                JBUI.Borders.empty(2, 8)
            )
            isOpaque = true
            background = JBColor.namedColor("Editor.SearchField.background", JBColor.PanelBackground)
        }
    }

    private fun makeFileChip(cf: com.markolukarami.copilotclone.domain.entities.context.ContextFile): JComponent {
        val name = cf.path.substringAfterLast('/').substringAfterLast('\\')
        val chip = com.intellij.util.ui.components.BorderLayoutPanel().apply {
            isOpaque = true
            background = JBColor.namedColor("Editor.SearchField.background", JBColor.PanelBackground)
            border = JBUI.Borders.compound(
                JBUI.Borders.customLine(JBColor.border(), 1),
                JBUI.Borders.empty(2, 8)
            )
        }

        val label = JBLabel(name).apply {
            toolTipText = cf.path
        }

        val close = JBLabel(AllIcons.Actions.Close).apply {
            toolTipText = "Remove from context"
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            border = JBUI.Borders.emptyLeft(6)
            addMouseListener(object : java.awt.event.MouseAdapter() {
                override fun mouseClicked(e: java.awt.event.MouseEvent) {
                    userContextState.remove(cf.path)
                    refresh()
                    onRemoveFile(cf.path)
                }
            })
        }

        chip.addToCenter(label)
        chip.addToRight(close)
        return chip
    }
}

