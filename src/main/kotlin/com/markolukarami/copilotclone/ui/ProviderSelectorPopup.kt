package com.markolukarami.copilotclone.ui

import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import com.markolukarami.copilotclone.domain.entities.LLMProvider
import java.awt.Point
import javax.swing.JComponent
import javax.swing.JMenuItem
import javax.swing.JPopupMenu

object ProviderSelectorPopup {
    fun show(
        anchor: JComponent,
        currentProvider: LLMProvider,
        onProviderSelected: (LLMProvider) -> Unit
    ) {
        val popupMenu = JPopupMenu()

        LLMProvider.entries.forEach { provider ->
            val item = JMenuItem(provider.displayName).apply {
                isEnabled = true
                addActionListener {
                    onProviderSelected(provider)
                }
                if (provider == currentProvider) {
                    text = "✓ ${provider.displayName}"
                }
            }
            popupMenu.add(item)
        }

        val point = RelativePoint(anchor, Point(0, anchor.height))
        popupMenu.show(anchor, 0, anchor.height)
    }
}


