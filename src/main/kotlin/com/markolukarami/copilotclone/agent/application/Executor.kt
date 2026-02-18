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
import com.markolukarami.copilotclone.frameworks.instructions.AgentsMdService

class Executor(
    private val chatRepository: ChatRepository,
    private val editorContextRepository: EditorContextRepository,
    private val project: Project,
    private val agentsMdService: AgentsMdService,
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
        val agents = agentsMdService.getInstructionsOrNull()

        val methodName = guessMethodName(userText)
        val extractedMethod = if (methodName != null) extractJavaMethodBlock(content, methodName) else null

        val contextBlock = when {
            extractedMethod != null -> "METHOD SOURCE (exact):\n$extractedMethod"
            else -> "FILE CONTENT (exact):\n$content"
        }

        return """
$agents
You must reply with ONLY a single JSON object. No markdown. No extra text.

Return this schema EXACTLY:
{
  "summary": "short description",
  "files": [
    {
      "relativePath": "$rel",
      "operations": [
        {
          "type": "REWRITE_METHOD",
          "methodName": "methodNameHere",
          "newSource": "full updated method source here"
        }
      ]
    }
  ]
}

Rules:
- Use EXACTLY this relativePath: "$rel"
- operations must be an array (use [] if you cannot safely do it).
- Do NOT include unrelated formatting changes.
- Prefer REWRITE_METHOD for ANY method content changes:
  - insert statements
  - add return at end
  - remove method body contents
  - delete a method (use DELETE_METHOD)
- newSource MUST be the ENTIRE method (signature + body), valid programming language.
IMPORTANT:
- If the requested method does NOT already exist in the file, use CREATE_METHOD.
- NEVER use REWRITE_METHOD to create a new method.
- REWRITE_METHOD is only allowed if the method already exists.

Supported operation types:

1) REWRITE_METHOD
   { "type":"REWRITE_METHOD", "methodName":"...", "newSource":"public ... { ... }" }

2) RENAME_METHOD
   { "type":"RENAME_METHOD", "oldName":"...", "newName":"..." }

3) DELETE_METHOD
   { "type":"DELETE_METHOD", "methodName":"..." }

4) REWRITE_FILE (last resort)
   { "type":"REWRITE_FILE", "newContent":"FULL FILE CONTENT" }

5) TEXT_REPLACE (fallback)
   { "type":"TEXT_REPLACE", "search":"EXACT TEXT", "replace":"..." }
   
6) CREATE_METHOD 
   { "type":"CREATE_METHOD", "methodSource":"public return type foo() { ... }" }

Rules for CREATE_METHOD:
- Put the entire method code in methodSource.
- Do NOT use methodName or newSource for CREATE_METHOD.


If the request is about adding a statement "after X" or "at the end":
- still use REWRITE_METHOD and place it correctly inside the method.

USER REQUEST:
$userText

FILE PATH (relative):
$rel

$contextBlock
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
                maxTokens = 900
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

        val finalMessages = mutableListOf<ChatMessage>()

        agentsMdService.getInstructionsOrNull()
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { agents ->
                finalMessages += ChatMessage(ChatRole.SYSTEM, agents)
            }

        finalMessages += ChatMessage(ChatRole.SYSTEM, "You are a helpful IntelliJ coding assistant.")

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

    private fun guessMethodName(userText: String): String? {
        val regex = Regex("""method\s+"?([A-Za-z_][A-Za-z0-9_]*)"?""", RegexOption.IGNORE_CASE)
        return regex.find(userText)?.groupValues?.getOrNull(1)
    }

    private fun extractJavaMethodBlock(fileText: String, methodName: String): String? {
        val idx = fileText.indexOf("$methodName(")
        if (idx == -1) return null

        var start = idx
        while (start > 0 && fileText[start - 1] != '\n') start--

        val braceOpen = fileText.indexOf('{', idx)
        if (braceOpen == -1) return null

        var depth = 0
        for (i in braceOpen until fileText.length) {
            when (fileText[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) {
                        return fileText.substring(start, i + 1)
                    }
                }
            }
        }
        return null
    }

}