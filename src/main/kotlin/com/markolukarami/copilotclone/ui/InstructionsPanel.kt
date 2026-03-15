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

To apply changes to your code, use one of the following keywords in your prompt:

Examples:
add|remove|delete|insert|create|update|move|edit|change|modify|apply|rename

Example prompts:
  "add a method exampleMethod that returns hello world"
  "rename method foo to bar"
  "delete method oldMethod"

Multi-File Support:

• Select multiple context files to apply changes across several files at once
• The plugin will analyze all selected files and generate a unified patch
• Each file gets its own operations in the patch plan

Important Rules:

• Context files must be selected for changes to work
• You can select 1 or more context files for single or multi-file edits
• Only valid code will be applied
• The plugin uses semantic PSI edits (Copilot-style)
• Press Apply to confirm changes, then Ctrl+Z to undo if needed

Normal questions without action keywords will NOT modify your code.

        """.trimIndent()

        add(JBScrollPane(textArea), BorderLayout.CENTER)
    }
}
