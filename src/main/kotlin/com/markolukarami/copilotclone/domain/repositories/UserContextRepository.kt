package com.markolukarami.copilotclone.domain.repositories

import com.markolukarami.copilotclone.domain.entities.ContextFile

interface UserContextRepository {

    fun getSelectedContextFiles(): List<ContextFile>
    fun setSelectedContextFiles(files: List<ContextFile>)
    fun clear()
}