package com.markolukarami.copilotclone.ui

import com.markolukarami.copilotclone.domain.entities.LLMProvider
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

        popupMenu.show(anchor, 0, anchor.height)
    }
}
