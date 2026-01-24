package com.markolukarami.copilotclone.ui
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class TracePanel {
    private val area = JBTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        text = "Trace will appear here after you send a message."
    }

    val component: JComponent = JPanel(BorderLayout()).apply {
        add(JBScrollPane(area), BorderLayout.CENTER)
    }

    fun setTraceLines(lines: List<String>) {
        area.text = lines.joinToString("\n\n")
        area.caretPosition = 0
    }
}
