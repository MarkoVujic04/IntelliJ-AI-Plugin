package com.markolukarami.copilotclone.ui

import com.intellij.ui.JBColor
import com.markolukarami.copilotclone.domain.entities.patch.FilePatch
import com.markolukarami.copilotclone.domain.entities.patch.PatchOperation
import javax.swing.text.DefaultStyledDocument
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants

object DiffRenderer {

    private val addedStyle = SimpleAttributeSet().apply {
        StyleConstants.setForeground(this, JBColor.GREEN.darker())
        StyleConstants.setBold(this, false)
    }

    private val removedStyle = SimpleAttributeSet().apply {
        StyleConstants.setForeground(this, JBColor.RED.darker())
        StyleConstants.setBold(this, false)
    }

    private val contextStyle = SimpleAttributeSet().apply {
        StyleConstants.setForeground(this, JBColor.GRAY)
    }

    private val headerStyle = SimpleAttributeSet().apply {
        StyleConstants.setForeground(this, JBColor.BLUE)
        StyleConstants.setBold(this, true)
    }

    fun createStyledDocument(summary: String, files: List<FilePatch>): DefaultStyledDocument {
        val doc = DefaultStyledDocument()

        doc.insertString(doc.length, "Summary:\n", headerStyle)
        doc.insertString(doc.length, "$summary\n\n", SimpleAttributeSet())

        files.forEach { file ->
            doc.insertString(doc.length, "File: ", headerStyle)
            doc.insertString(doc.length, "${file.relativePath}\n", SimpleAttributeSet())

            if (file.operations.isEmpty()) {
                doc.insertString(doc.length, "  (no changes)\n\n", contextStyle)
            } else {
                file.operations.forEach { operation ->
                    renderOperation(doc, operation)
                }
                doc.insertString(doc.length, "\n", SimpleAttributeSet())
            }
        }

        doc.insertString(doc.length, "Click 'OK' to apply. You can undo with Ctrl+Z.\n", contextStyle)

        return doc
    }

    private fun renderOperation(doc: DefaultStyledDocument, operation: PatchOperation) {
        when (operation.type) {
            "REMOVE_METHOD", "DELETE_METHOD" -> renderRemoveMethod(doc, operation)
            "ADD_METHOD", "CREATE_METHOD" -> renderAddMethod(doc, operation)
            "REPLACE_TEXT", "TEXT_REPLACE" -> renderReplaceText(doc, operation)
            "ADD_CODE" -> renderAddCode(doc, operation)
            "REMOVE_CODE" -> renderRemoveCode(doc, operation)
            "MODIFY_CLASS" -> renderModifyClass(doc, operation)
            "REWRITE_METHOD" -> renderRewriteMethod(doc, operation)
            "REWRITE_FILE" -> renderRewriteFile(doc, operation)
            else -> renderGenericOperation(doc, operation)
        }
    }

    private fun renderRemoveMethod(doc: DefaultStyledDocument, op: PatchOperation) {
        doc.insertString(doc.length, "  Remove method: ", SimpleAttributeSet())
        doc.insertString(doc.length, "${op.methodName}\n", removedStyle)

        op.methodSource?.let {
            renderCodeBlock(doc, it, removedStyle)
        }
    }

    private fun renderAddMethod(doc: DefaultStyledDocument, op: PatchOperation) {
        val methodName = op.methodName ?: "new method"
        doc.insertString(doc.length, "  Add method: ", SimpleAttributeSet())
        doc.insertString(doc.length, "$methodName\n", addedStyle)

        val sourceCode = op.newSource ?: op.methodSource
        sourceCode?.let {
            renderCodeBlock(doc, it, addedStyle)
        } ?: run {
            doc.insertString(doc.length, "    (no code content)\n", contextStyle)
        }
    }

    private fun renderReplaceText(doc: DefaultStyledDocument, op: PatchOperation) {
        doc.insertString(doc.length, "  Replace text:\n", SimpleAttributeSet())

        op.search?.let {
            doc.insertString(doc.length, "    --- ", SimpleAttributeSet())
            renderCodeBlock(doc, it, removedStyle)
        }

        op.replace?.let {
            doc.insertString(doc.length, "    +++ ", SimpleAttributeSet())
            renderCodeBlock(doc, it, addedStyle)
        }
    }

    private fun renderAddCode(doc: DefaultStyledDocument, op: PatchOperation) {
        doc.insertString(doc.length, "  Add code", SimpleAttributeSet())
        op.position?.let { doc.insertString(doc.length, " (at $it)", contextStyle) }
        doc.insertString(doc.length, ":\n", SimpleAttributeSet())

        op.newContent?.let {
            doc.insertString(doc.length, "    +++ ", SimpleAttributeSet())
            renderCodeBlock(doc, it, addedStyle)
        }
    }

    private fun renderRemoveCode(doc: DefaultStyledDocument, op: PatchOperation) {
        doc.insertString(doc.length, "  Remove code", SimpleAttributeSet())
        op.position?.let { doc.insertString(doc.length, " (at $it)", contextStyle) }
        doc.insertString(doc.length, ":\n", SimpleAttributeSet())

        op.statement?.let {
            doc.insertString(doc.length, "    --- ", SimpleAttributeSet())
            renderCodeBlock(doc, it, removedStyle)
        }
    }

    private fun renderModifyClass(doc: DefaultStyledDocument, op: PatchOperation) {
        doc.insertString(doc.length, "  Modify class: ", SimpleAttributeSet())
        doc.insertString(doc.length, "${op.className}\n", SimpleAttributeSet())

        op.oldName?.let {
            doc.insertString(doc.length, "    --- Name: $it\n", removedStyle)
        }

        op.newName?.let {
            doc.insertString(doc.length, "    +++ Name: $it\n", addedStyle)
        }
    }

    private fun renderRewriteMethod(doc: DefaultStyledDocument, op: PatchOperation) {
        doc.insertString(doc.length, "  Rewrite method: ", SimpleAttributeSet())
        doc.insertString(doc.length, "${op.methodName}\n", SimpleAttributeSet())

        op.methodSource?.let {
            doc.insertString(doc.length, "    --- Old code:\n", SimpleAttributeSet())
            renderCodeBlock(doc, it, removedStyle)
        }

        op.newSource?.let {
            doc.insertString(doc.length, "    +++ New code:\n", SimpleAttributeSet())
            renderCodeBlock(doc, it, addedStyle)
        }
    }

    private fun renderRewriteFile(doc: DefaultStyledDocument, op: PatchOperation) {
        doc.insertString(doc.length, "  Rewrite entire file\n", SimpleAttributeSet())

        op.newContent?.let {
            doc.insertString(doc.length, "    +++ New content:\n", SimpleAttributeSet())
            val lines = it.trim().split("\n")
            val preview = if (lines.size > 50) {
                lines.take(50).joinToString("\n") + "\n... (${lines.size - 50} more lines)"
            } else {
                it.trim()
            }
            renderCodeBlock(doc, preview, addedStyle)
        }
    }

    private fun renderGenericOperation(doc: DefaultStyledDocument, op: PatchOperation) {
        doc.insertString(doc.length, "  Operation (${op.type}):\n", SimpleAttributeSet())

        if (op.methodSource != null || op.oldName != null || op.search != null || op.statement != null) {
            doc.insertString(doc.length, "    --- ", SimpleAttributeSet())
            val oldContent = op.methodSource ?: op.oldName ?: op.search ?: op.statement ?: ""
            if (oldContent.contains("\n")) {
                renderCodeBlock(doc, oldContent, removedStyle)
            } else {
                doc.insertString(doc.length, oldContent.trim() + "\n", removedStyle)
            }
        }

        if (op.newSource != null || op.newName != null || op.replace != null || op.newContent != null) {
            doc.insertString(doc.length, "    +++ ", SimpleAttributeSet())
            val newContent = op.newSource ?: op.newName ?: op.replace ?: op.newContent ?: ""
            if (newContent.contains("\n")) {
                renderCodeBlock(doc, newContent, addedStyle)
            } else {
                doc.insertString(doc.length, newContent.trim() + "\n", addedStyle)
            }
        }
    }

    private fun renderCodeBlock(doc: DefaultStyledDocument, code: String, style: SimpleAttributeSet) {
        val lines = code.trim().split("\n")
        lines.forEachIndexed { index, line ->
            doc.insertString(doc.length, line, style)
            if (index < lines.size - 1) {
                doc.insertString(doc.length, "\n    ", SimpleAttributeSet())
            }
        }
        doc.insertString(doc.length, "\n", SimpleAttributeSet())
    }
}

