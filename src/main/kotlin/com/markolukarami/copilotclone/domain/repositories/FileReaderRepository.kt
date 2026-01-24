package com.markolukarami.copilotclone.domain.repositories

import com.markolukarami.copilotclone.domain.entities.FileSnippet

interface FileReaderRepository {
    fun readFile(path: String, maxChars: Int = 6000): FileSnippet?
}