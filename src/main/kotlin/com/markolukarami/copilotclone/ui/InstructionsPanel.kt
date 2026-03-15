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
Welcome to LLM Plugin 🚀

First of all make sure your LM Studio server is running.

Go to File > Settings > Tools > AI Plugin to configure:
- Select your LLM Provider (LM Studio or Ollama) or choose it directly in the bottom UI
- Set the Base URL (default: LM Studio: http://127.0.0.1:1234, Ollama: http://127.0.0.1:11434)
- Set your model name or choose it directly in the bottom UI

To apply changes to your code, you MUST start your prompt with one of the following keywords:

Apply this change: <your instruction>

Examples:
add|remove|delete|insert|create|update|move|edit|change|modify|apply|rename
      
Apply this change: generate a method "exampleMethod"

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
