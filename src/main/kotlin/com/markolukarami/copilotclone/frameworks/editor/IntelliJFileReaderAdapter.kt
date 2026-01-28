package com.markolukarami.copilotclone.frameworks.editor
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.markolukarami.copilotclone.domain.entities.FileSnippet
import com.markolukarami.copilotclone.domain.repositories.FileReaderRepository
import java.nio.charset.StandardCharsets

class IntelliJFileReaderAdapter : FileReaderRepository {

    override fun readFile(path: String, maxChars: Int): FileSnippet? {
        val p = path.trim()
        if (p.isBlank()) return null

        return ReadAction.compute<FileSnippet?, RuntimeException> {
            val vf = LocalFileSystem.getInstance().findFileByPath(p) ?: return@compute null
            if (vf.isDirectory) return@compute null

            val bytes = VfsUtilCore.loadBytes(vf)
            val text = String(bytes, StandardCharsets.UTF_8)

            val clipped = if (text.length > maxChars) text.take(maxChars) else text
            FileSnippet(filePath = vf.path, content = clipped)
        }
    }
}
