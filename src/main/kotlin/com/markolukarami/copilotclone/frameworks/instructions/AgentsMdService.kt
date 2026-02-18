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
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore

@Service(Service.Level.PROJECT)
class AgentsMdService(private val project: Project) : Disposable {

    private val cachedText = AtomicReference<String?>(null)
    private val cachedPath = AtomicReference<String?>(null)

    private var connection: MessageBusConnection? = null

    private val log = Logger.getInstance(AgentsMdService::class.java)

    init {
        reloadNow()

        connection = project.messageBus.connect(this).also { conn ->
            conn.subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
                override fun after(events: List<VFileEvent>) {
                    val known = cachedPath.get()
                    val touched = events.any { e ->
                        val p = e.path
                        (known != null && p == known) ||
                                p.endsWith("/AGENTS.md") ||
                                p.endsWith("\\AGENTS.md")
                    }
                    if (touched) reloadNow()
                }
            })
        }
    }

    fun getInstructionsOrNull(): String? = cachedText.get()

    fun getAgentsPath(): String? = cachedPath.get()

    fun reloadNow() {
        project.basePath?.let { base ->
            com.intellij.openapi.vfs.LocalFileSystem.getInstance()
                .refreshAndFindFileByPath(base)
                ?.let { VfsUtil.markDirtyAndRefresh(false, true, true, it) }
        }

        val vf = locateAgentsMd()
        if (vf == null) {
            cachedPath.set(null)
            cachedText.set(null)
            return
        }

        cachedPath.set(vf.path)

        val text = try {
            vf.charset = Charsets.UTF_8
            vf.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
        } catch (t: Throwable) {
            null
        }

        val finalText = text?.trim()?.takeIf { it.isNotBlank() }
        cachedText.set(finalText)
    }

    private fun locateAgentsMd(): VirtualFile? {
        val fs = com.intellij.openapi.vfs.LocalFileSystem.getInstance()

        project.basePath?.let { base ->
            val direct = fs.refreshAndFindFileByPath(base.trimEnd('/', '\\') + "/AGENTS.md")
            if (direct != null && direct.exists()) return direct
        }

        project.basePath?.let { base ->
            val root = fs.refreshAndFindFileByPath(base)
            if (root != null) {
                val found = VfsUtilCore.iterateChildrenRecursively(
                    root,
                    { true }
                ) { file ->
                    if (!file.isDirectory && file.name.equals("AGENTS.md", ignoreCase = true)) {
                        return@iterateChildrenRecursively false
                    }
                    true
                }

            }
        }

        project.basePath?.let { base ->
            val known = fs.refreshAndFindFileByPath(
                base.trimEnd('/', '\\') + "/src/main/java/marko/luka/sudokunew/AGENTS.md"
            )
            if (known != null && known.exists()) return known
        }

        return null
    }

    override fun dispose() {
        connection?.disconnect()
        connection = null
    }
}