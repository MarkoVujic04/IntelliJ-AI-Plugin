package com.markolukarami.copilotclone.ui

import com.markolukarami.copilotclone.adapters.presentation.TraceLineVM
import java.awt.Component
import javax.swing.DefaultListCellRenderer
import javax.swing.JList

class TraceRenderer : DefaultListCellRenderer() {
    override fun getListCellRendererComponent(
        list: JList<*>,
        value: Any?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        val vm = value as? TraceLineVM
        val text = vm?.text ?: value?.toString().orEmpty()
        return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus)
    }
}
