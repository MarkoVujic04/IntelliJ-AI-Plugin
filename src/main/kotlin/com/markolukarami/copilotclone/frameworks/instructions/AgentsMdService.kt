package com.markolukarami.copilotclone.frameworks.instructions

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.util.messages.MessageBusConnection
import java.util.concurrent.atomic.AtomicReference

@Service(Service.Level.PROJECT)
class AgentsMdService(private val project: Project) : Disposable {

    private val cachedText = AtomicReference<String?>(null)
    private val cachedPath = AtomicReference<String?>(null)

    private var connection: MessageBusConnection? = null

    init {
        reloadNow()

        connection = project.messageBus.connect(this).also { conn ->
            conn.subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
                override fun after(events: List<VFileEvent>) {
                    val known = cachedPath.get()
                    val touched = events.any { e ->
                        val p = e.path
                        known != null && (p == known) ||
                                p.endsWith("/AGENTS.md") || p.endsWith("\\AGENTS.md")
                    }
                    if (touched) reloadNow()
                }
            })
        }
    }

    fun getInstructionsOrNull(): String? = cachedText.get()

    fun getAgentsPath(): String? = cachedPath.get()

    fun reloadNow() {
        val vf = locateAgentsMd()
        if (vf == null) {
            cachedPath.set(null)
            cachedText.set(null)
            return
        }

        cachedPath.set(vf.path)

        val text = try {
            String(vf.contentsToByteArray())
        } catch (_: Throwable) {
            ""
        }.trim()

        cachedText.set(text.takeIf { it.isNotBlank() })
    }

    private fun locateAgentsMd(): VirtualFile? {
        val base = project.baseDir ?: return null
        return base.findChild("AGENTS.md")
    }

    override fun dispose() {
        connection?.disconnect()
        connection = null
    }
}