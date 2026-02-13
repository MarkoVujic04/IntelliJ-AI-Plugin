package com.markolukarami.copilotclone.domain.repositories

import com.markolukarami.copilotclone.domain.entities.context.ContextFile

interface UserContextRepository {

    fun getSelectedContextFiles(): List<ContextFile>
    fun setSelectedContextFiles(files: List<ContextFile>)
    fun clear()
    fun remove(path: String)
}