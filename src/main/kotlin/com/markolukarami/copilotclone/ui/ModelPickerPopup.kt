package com.markolukarami.copilotclone.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.components.JBList
import com.markolukarami.copilotclone.domain.entities.model.ModelInfo
import java.awt.Component
import javax.swing.ListSelectionModel

object ModelPickerPopup {

    fun show(
        project: Project,
        anchor: Component,
        models: List<ModelInfo>,
        currentModelId: String?,
        onPick: (ModelInfo) -> Unit
    ) {
        val items = models.sortedBy { it.id }

        val list = JBList(items).apply {
            selectionMode = ListSelectionModel.SINGLE_SELECTION
            setCellRenderer { _, value, _, _, _ ->
                val label = com.intellij.ui.components.JBLabel(value.id)
                label
            }

            selectedIndex = items.indexOfFirst { it.id == currentModelId }.takeIf { it >= 0 } ?: 0

            addListSelectionListener {
            }
        }

        val popup = JBPopupFactory.getInstance()
            .createListPopupBuilder(list)
            .setTitle("Select LM Studio Model")
            .setItemChoosenCallback {
                val picked = list.selectedValue ?: return@setItemChoosenCallback
                onPick(picked)
            }
            .createPopup()

        popup.showUnderneathOf(anchor)
    }
}