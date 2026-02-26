package com.markolukarami.copilotclone.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.markolukarami.copilotclone.domain.entities.chat.ChatSession
import com.markolukarami.copilotclone.frameworks.chat.ChatSessionState
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Cursor
import java.awt.Dimension
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.*

class ChatHistoryDialog(
    project: Project,
    private val chatSessionState: ChatSessionState,
    private val onSessionSelected: (ChatSession) -> Unit
) : DialogWrapper(project, true) {

    private var selectedSession: ChatSession? = null
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm")

    init {
        title = "Chat History"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val sessions = chatSessionState.listSessions()
        val listModel = DefaultListModel<ChatSessionDisplay>()

        sessions.forEach { session ->
            listModel.addElement(ChatSessionDisplay(session))
        }

        val sessionList = JList(listModel).apply {
            selectionMode = ListSelectionModel.SINGLE_SELECTION
            cellRenderer = ChatSessionRenderer()
            preferredSize = Dimension(400, 400)

            addListSelectionListener { event ->
                if (!event.valueIsAdjusting) {
                    selectedSession = (selectedValue as? ChatSessionDisplay)?.session
                }
            }

            addMouseListener(object : java.awt.event.MouseAdapter() {
                override fun mouseClicked(e: java.awt.event.MouseEvent) {
                    val index = locationToIndex(e.point)
                    if (index >= 0) {
                        val bounds = getCellBounds(index, index)
                        val relativeX = e.x - bounds.x
                        val cellWidth = bounds.width

                        if (relativeX > cellWidth - 35) {
                            val sessionToDelete = listModel.getElementAt(index).session

                            val result = JOptionPane.showConfirmDialog(
                                this@apply,
                                "Are you sure you want to delete this chat?",
                                "Delete Chat",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.WARNING_MESSAGE
                            )

                            if (result == JOptionPane.YES_OPTION) {
                                chatSessionState.deleteSession(sessionToDelete.id)
                                listModel.clear()
                                chatSessionState.listSessions().forEach {
                                    listModel.addElement(ChatSessionDisplay(it))
                                }
                            }
                        }
                    }
                }
            })
        }

        return JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(12)
            add(JBScrollPane(sessionList), BorderLayout.CENTER)
        }
    }

    override fun doOKAction() {
        selectedSession?.let(onSessionSelected)
        super.doOKAction()
    }

    private data class ChatSessionDisplay(val session: ChatSession) {
        override fun toString(): String {
            val date = SimpleDateFormat("MMM dd, yyyy HH:mm").format(Date(session.createdAt))
            return "${session.title} - $date"
        }
    }

    private class ChatSessionRenderer : ListCellRenderer<ChatSessionDisplay> {
        override fun getListCellRendererComponent(
            list: JList<out ChatSessionDisplay>?,
            value: ChatSessionDisplay?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            val panel = JPanel(BorderLayout()).apply {
                border = JBUI.Borders.empty(4, 8)
            }

            if (value != null) {
                val session = value.session
                val date = SimpleDateFormat("MMM dd, yyyy HH:mm").format(Date(session.createdAt))

                val textLabel = JBLabel("${session.title} - $date").apply {
                    border = JBUI.Borders.empty(0, 0, 0, 8)
                }

                val deleteButton = JLabel(AllIcons.Actions.Close).apply {
                    preferredSize = Dimension(20, 20)
                    toolTipText = "Delete chat"
                    cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                }

                panel.add(textLabel, BorderLayout.CENTER)
                panel.add(deleteButton, BorderLayout.EAST)
            }

            if (isSelected) {
                panel.background = list?.selectionBackground
                panel.components.forEach {
                    if (it is JBLabel) {
                        it.foreground = list?.selectionForeground
                    }
                }
            } else {
                panel.background = list?.background
            }

            return panel
        }
    }
}
