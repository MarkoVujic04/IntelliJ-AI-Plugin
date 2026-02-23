package com.markolukarami.copilotclone.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.markolukarami.copilotclone.domain.entities.chat.ChatSession
import com.markolukarami.copilotclone.frameworks.chat.ChatSessionState
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.Dimension
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListSelectionModel

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
            preferredSize = Dimension(300, 400)

            addListSelectionListener { event ->
                if (!event.valueIsAdjusting) {
                    selectedSession = (selectedValue as? ChatSessionDisplay)?.session
                }
            }
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

    private class ChatSessionRenderer : javax.swing.DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): java.awt.Component {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)

            if (value is ChatSessionDisplay) {
                val session = value.session
                val date = SimpleDateFormat("MMM dd, yyyy HH:mm").format(Date(session.createdAt))
                text = "${session.title} - $date"
            }

            return this
        }
    }
}

