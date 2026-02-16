package com.markolukarami.copilotclone.agent.application

import com.intellij.openapi.project.Project
import com.markolukarami.copilotclone.application.patch.PatchParser
import com.markolukarami.copilotclone.domain.entities.ChatMessage
import com.markolukarami.copilotclone.domain.entities.ChatResult
import com.markolukarami.copilotclone.domain.entities.ChatRole
import com.markolukarami.copilotclone.domain.entities.ModelConfig
import com.markolukarami.copilotclone.domain.entities.trace.TraceStep
import com.markolukarami.copilotclone.domain.entities.trace.TraceType
import com.markolukarami.copilotclone.domain.repositories.ChatRepository
import com.markolukarami.copilotclone.domain.repositories.EditorContextRepository

class Executor(
    private val chatRepository: ChatRepository,
    private val editorContextRepository: EditorContextRepository,
    private val project: Project,
) {

    private fun isPatchRequest(userText: String): Boolean {
        val t = userText.lowercase()
        return t.startsWith("apply") ||
                t.contains("apply this") ||
                t.contains("make this change") ||
                t.contains("rename") ||
                t.contains("refactor") ||
                t.contains("edit the code") ||
                t.contains("change the code")
    }

    private fun buildPatchPrompt(
        userText: String,
        evidence: Strategist.SelectedEvidence,
        projectBasePath: String
    ): String {
        val file = evidence.files.firstOrNull()
        val abs = (file?.filePath ?: "").replace('\\', '/')
        val base = projectBasePath.replace('\\', '/').trimEnd('/') + "/"
        val rel = abs.removePrefix(base)
        val content = file?.content ?: ""

        return """
You must reply with ONLY a single JSON object. No markdown. No extra text.

Return this schema EXACTLY:
{
  "summary": "short description",
  "files": [
    {
      "relativePath": "$rel",
      "operations": [
        {
          "type": "RENAME_METHOD",
          "oldName": "reset",
          "newName": "preset"
        }
      ]
    }
  ]
}

Rules:
- Use EXACTLY this relativePath: "$rel"
- Only include operations needed for the user's request.
- Do NOT add extra formatting edits (no random tabs/spaces).
- operations must be [] if you cannot safely apply.

Supported operation types:

1) RENAME_METHOD
   { "type":"RENAME_METHOD", "oldName":"...", "newName":"..." }

2) REMOVE_METHOD_BODY
   { "type":"REMOVE_METHOD_BODY", "methodName":"..." }

3) DELETE_METHOD
   { "type":"DELETE_METHOD", "methodName":"..." }

4) CREATE_METHOD  (Java only)
   { "type":"CREATE_METHOD", "methodSource":"public void foo() { ... }" }

5) TEXT_REPLACE  (fallback for anything)
   { "type":"TEXT_REPLACE", "search":"EXACT TEXT", "replace":"..." }

6) REWRITE_FILE  (last resort)
   { "type":"REWRITE_FILE", "newContent":"FULL FILE CONTENT" }
   
7) INSERT_STATEMENT (Java PSI)

{
  "type":"INSERT_STATEMENT",
  "methodName":"...",
  "position":"START|END|AFTER_TEXT",
  "afterText":"optional short unique snippet",
  "statement":"valid Java statement ending with ;"
}

Rules:
- If user says "at the end" -> position = "END"
- If user says "at the start" -> position = "START"
- If user says "after X" -> position = "AFTER_TEXT"
- statement must be a single valid Java statement
- Do NOT use TEXT_REPLACE if INSERT_STATEMENT can handle it

Prefer PSI operations (RENAME_METHOD / REMOVE_METHOD_BODY / DELETE_METHOD / CREATE_METHOD) when the file is Java.

USER REQUEST:
$userText

FILE PATH (relative):
$rel

FILE CONTENT (exact):
$content
""".trimIndent()
    }


    fun executeFinal(
        userText: String,
        config: ModelConfig,
        evidence: Strategist.SelectedEvidence,
        trace: MutableList<TraceStep>
    ): ChatResult {

        val patchMode = isPatchRequest(userText)

        if (patchMode) {
            trace += TraceStep("Agent: Executor", "Patch-mode JSON call", TraceType.MODEL)

            val basePath = project.basePath ?: ""
            val patchPrompt = buildPatchPrompt(userText, evidence, basePath)

            val patchMessages = listOf(
                ChatMessage(ChatRole.USER, patchPrompt)
            )

            val patchConfig = config.copy(
                temperature = 0.0,
                maxTokens = 800
            )

            val raw1 = chatRepository.chat(patchConfig, patchMessages)
            val patch1 = PatchParser.parseOrNull(raw1)

            if (patch1 != null && patch1.files.isNotEmpty()) {
                trace += TraceStep("Patch proposed", "files=${patch1.files.size}", TraceType.INFO)
                return ChatResult(
                    assistantText = "I prepared an edit patch:\n- ${patch1.summary}\nPress Apply to update your code.",
                    trace = trace,
                    patch = patch1
                )
            }

            trace += TraceStep("Patch parse failed", "Retry JSON-only", TraceType.ERROR)

            val retryMessages = listOf(
                ChatMessage(
                    ChatRole.USER,
                    "Return ONLY the JSON object. No words. No markdown. Use the schema exactly.\n\n$patchPrompt"
                )
            )

            val raw2 = chatRepository.chat(patchConfig, retryMessages)
            val patch2 = PatchParser.parseOrNull(raw2)

            if (patch2 != null && patch2.files.isNotEmpty()) {
                trace += TraceStep("Patch proposed", "files=${patch2.files.size}", TraceType.INFO)
                return ChatResult(
                    assistantText = "I prepared an edit patch:\n- ${patch2.summary}\nPress Apply to update your code.",
                    trace = trace,
                    patch = patch2
                )
            }

            return ChatResult(
                assistantText = raw2,
                trace = trace,
                patch = null
            )
        }

        trace += TraceStep("Agent: Executor", "Normal answer mode", TraceType.MODEL)

        val finalMessages = mutableListOf(
            ChatMessage(ChatRole.SYSTEM, "You are a helpful IntelliJ coding assistant.")
        )

        val editorContext = editorContextRepository.getCurrentContext()
        editorContext.selectedText?.let {
            finalMessages += ChatMessage(ChatRole.SYSTEM, "Selected code:\n$it")
        }

        if (evidence.snippets.isNotEmpty()) {
            finalMessages += ChatMessage(
                ChatRole.SYSTEM,
                "Project snippets:\n" + evidence.snippets.joinToString("\n") {
                    "- ${it.filePath}:${it.lineNumber} ${it.preview}"
                }
            )
        }

        if (evidence.files.isNotEmpty()) {
            finalMessages += ChatMessage(
                ChatRole.SYSTEM,
                "File excerpts:\n" + evidence.files.joinToString("\n\n") {
                    "FILE ${it.filePath}\n${it.content.take(1500)}"
                }
            )
        }

        finalMessages += ChatMessage(ChatRole.USER, userText)

        val answer = chatRepository.chat(config, finalMessages)
        return ChatResult(answer, trace, patch = null)
    }
}