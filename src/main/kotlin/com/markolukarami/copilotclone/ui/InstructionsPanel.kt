package com.markolukarami.copilotclone.ui

import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import java.awt.BorderLayout
import javax.swing.JPanel

class InstructionsPanel : JPanel(BorderLayout()) {

    init {
        val textArea = JBTextArea()
        textArea.isEditable = false
        textArea.lineWrap = true
        textArea.wrapStyleWord = true

        textArea.text = """
Welcome to Copilot Clone 🚀

To apply changes to your code, you MUST start your prompt with:

Apply this change: <your instruction>

Examples:

Apply this change: Rename method reset to initialize.

Apply this change: Add return true; at the end of isGameComplete method.

Apply this change: Write a method called exampleMethod that does this

Important Rules:

• Changes only work when the prompt starts with "Apply this change:"
• The context file must be selected
• Only valid code will be applied
• The plugin uses semantic PSI edits (Copilot-style)

Normal questions without "Apply this change:" will NOT modify your code.

        """.trimIndent()

        add(JBScrollPane(textArea), BorderLayout.CENTER)
    }
}
