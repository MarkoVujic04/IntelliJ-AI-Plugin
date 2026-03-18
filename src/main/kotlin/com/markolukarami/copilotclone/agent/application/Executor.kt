package com.markolukarami.copilotclone.agent.application

import com.intellij.openapi.project.Project
import com.markolukarami.copilotclone.application.patch.PatchParser
import com.markolukarami.copilotclone.domain.entities.ChatMessage
import com.markolukarami.copilotclone.domain.entities.ChatResult
import com.markolukarami.copilotclone.domain.entities.ChatRole
import com.markolukarami.copilotclone.domain.entities.ModelConfig
import com.markolukarami.copilotclone.domain.entities.code.CodeAnalysis
import com.markolukarami.copilotclone.domain.entities.code.ConsistencyIssue
import com.markolukarami.copilotclone.domain.entities.trace.TraceStep
import com.markolukarami.copilotclone.domain.entities.trace.TraceType
import com.markolukarami.copilotclone.domain.repositories.ChatRepository
import com.markolukarami.copilotclone.domain.repositories.ChatSessionRepository
import com.markolukarami.copilotclone.domain.repositories.EditorContextRepository
import com.markolukarami.copilotclone.frameworks.editor.CodeInspector
import com.markolukarami.copilotclone.frameworks.editor.ConsistencyChecker
import com.markolukarami.copilotclone.frameworks.instructions.AgentsMdService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentLinkedQueue

class Executor(
    private val chatRepository: ChatRepository,
    private val editorContextRepository: EditorContextRepository,
    private val project: Project,
    private val agentsMdService: AgentsMdService,
    private val codeInspector: CodeInspector,
    private val chatSessionRepository: ChatSessionRepository,
) {

    companion object {
        private const val MAX_HISTORY_CHARS = 4000
        private const val MAX_HISTORY_TURNS = 10
    }

    private fun isPatchRequest(userText: String): Boolean {
        val t = userText.lowercase()
        val patchPatterns = listOf(
            Regex("""(add|remove|delete|insert|create|update|move)\s+(a\s+)?(method|field|class|function|import|line|statement)"""),
            Regex("""(rename|refactor|extract|inline)\s+"""),
            Regex("""(apply|make)\s+(this\s+)?(change|edit|fix|patch)"""),
            Regex("""(edit|change|modify|fix)\s+(the\s+)?(code|method|class|file)"""),
            Regex("""add\s+.*\s+to\s+"""),
            Regex("""remove\s+.*\s+from\s+"""),
        )
        return patchPatterns.any { it.containsMatchIn(t) }
    }


    private data class FileAnalysisEntry(
        val relativePath: String,
        val content: String,
        val analysis: CodeAnalysis
    )

    private fun buildPatchPrompt(
        userText: String,
        projectBasePath: String,
        fileAnalyses: List<FileAnalysisEntry>,
        conversationHistory: String? = null
    ): String {
        val agents = agentsMdService.getInstructionsOrNull()

        val methodName = guessMethodName(userText)

        val fileBlocks = fileAnalyses.mapIndexed { idx, entry ->
            val extractedMethod = if (methodName != null) extractJavaMethodBlock(entry.content, methodName) else null

            val contextBlock = when {
                extractedMethod != null -> "METHOD SOURCE (exact):\n$extractedMethod"
                else -> "FILE CONTENT (exact):\n${entry.content}"
            }

            val analysisBlock = entry.analysis.formatForPrompt()

            val targetMethodAnalysis = if (methodName != null) {
                val sig = entry.analysis.methods.find { it.name == methodName }
                if (sig != null) {
                    val params = sig.parameters.joinToString(", ") { "${it.type} ${it.name}" }
                    """
TARGET METHOD ANALYSIS:
  Method: ${sig.name}
  Returns: ${sig.returnType}
  Parameters: $params
  The generated code MUST return a value of type '${sig.returnType}' (unless void/Unit).
  The generated code MUST accept exactly these parameter types.
"""
                } else ""
            } else ""

            """
--- FILE ${idx + 1}: ${entry.relativePath} ---
$analysisBlock
$targetMethodAnalysis
$contextBlock
"""
        }

        val allSimilar = fileAnalyses.flatMap { it.analysis.similarMethods }.distinctBy { it.name }
        val similarBlock = if (allSimilar.isNotEmpty()) {
            val lines = allSimilar.joinToString("\n") { m ->
                val p = m.parameters.joinToString(", ") { "${it.type} ${it.name}" }
                "  ${m.returnType} ${m.name}($p) [${m.filePath ?: "unknown"}]"
            }
            """
SIMILAR METHODS ALREADY IN CODEBASE (reuse patterns, do not duplicate):
$lines
"""
        } else ""

        val filesSchemaExample = fileAnalyses.joinToString(",\n    ") { entry ->
            """{ "relativePath": "${entry.relativePath}", "operations": [ { "type": "REWRITE_METHOD", "methodName": "...", "newSource": "..." } ] }"""
        }

        val relPathRules = fileAnalyses.joinToString("\n") { entry ->
            "- \"${entry.relativePath}\""
        }

        return """
$agents
You must reply with ONLY a single JSON object. No markdown. No extra text.

Return this schema EXACTLY:
{
  "summary": "short description",
  "files": [
    $filesSchemaExample
  ]
}

Rules:
- Use ONLY these relativePaths:
$relPathRules
- You may include operations for one or more files. Only include files that need changes.
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

$similarBlock
${conversationHistory ?: ""}
USER REQUEST:
$userText

${fileBlocks.joinToString("\n")}
""".trimIndent()
    }

    fun executeFinal(
        userText: String,
        config: ModelConfig,
        evidence: Strategist.SelectedEvidence,
        trace: MutableList<TraceStep>
    ): ChatResult {

        if (config.model.isBlank()) {
            trace += TraceStep(
                "Executor",
                "No model configured. Please set a model in Settings > Tools > AI Plugin or in bottom UI.",
                TraceType.ERROR
            )
            return ChatResult(
                assistantText = "⚠ No model configured. Please go to **Settings > Tools > AI Plugin** and set a model.",
                trace = trace,
                patch = null
            )
        }

        val patchMode = isPatchRequest(userText)

        if (patchMode) {
            trace += TraceStep("Agent: Executor", "Patch-mode JSON call", TraceType.MODEL)

            val basePath = project.basePath ?: ""
            val baseNorm = basePath.replace('\\', '/').trimEnd('/') + "/"

            val fileAnalyses: List<FileAnalysisEntry> = run {
                val startTime = System.currentTimeMillis()
                val parallelTraces = ConcurrentLinkedQueue<TraceStep>()

                val results = runBlocking {
                    evidence.files.map { file ->
                        async(Dispatchers.Default) {
                            val absPath = file.filePath.replace('\\', '/').trim()
                            if (absPath.isBlank()) return@async null
                            val rel = absPath.removePrefix(baseNorm)

                            parallelTraces += TraceStep("Pre-gen analysis", "Inspecting $rel", TraceType.TOOL)

                            val analysis = codeInspector.analyze(absPath)

                            parallelTraces += TraceStep(
                                "Analysis: $rel",
                                "methods=${analysis.methods.size}, fields=${analysis.fields.size}, " +
                                        "imports=${analysis.imports.size}",
                                TraceType.INFO
                            )

                            FileAnalysisEntry(
                                relativePath = rel,
                                content = file.content,
                                analysis = analysis
                            )
                        }
                    }.awaitAll().filterNotNull()
                }

                val elapsed = System.currentTimeMillis() - startTime
                trace += parallelTraces
                trace += TraceStep(
                    "Parallel analysis",
                    "Analyzed ${results.size} file(s) in ${elapsed}ms",
                    TraceType.INFO
                )
                results
            }

            if (fileAnalyses.isEmpty()) {
                trace += TraceStep("Pre-gen analysis", "No files available for analysis", TraceType.ERROR)
            }


            val history = getRecentHistory()
            val historySummary = buildHistorySummary(history)

            if (history.isNotEmpty()) {
                trace += TraceStep(
                    "Conversation memory",
                    "Included ${history.size} message(s) from history",
                    TraceType.INFO
                )
            }

            val patchPrompt = buildPatchPrompt(userText, basePath, fileAnalyses, historySummary)

            val patchMessages = listOf(
                ChatMessage(ChatRole.USER, patchPrompt)
            )

            val computedMaxTokens = ModelTokenEstimator.compute(
                userOverride = config.maxResponseTokens,
                modelName = config.model,
                promptText = patchPrompt,
                fileCount = fileAnalyses.size
            )

            trace += TraceStep(
                "Token budget",
                "maxTokens=$computedMaxTokens (override=${config.maxResponseTokens}, model=${config.model})",
                TraceType.INFO
            )

            val patchConfig = config.copy(
                temperature = 0.0,
                maxTokens = computedMaxTokens
            )

            val mergedAnalysis = mergeAnalyses(fileAnalyses)

            val raw1 = chatRepository.chat(patchConfig, patchMessages)
            val patch1 = PatchParser.parseOrNull(raw1)

            if (patch1 != null && patch1.files.isNotEmpty()) {
                val issues = ConsistencyChecker.check(mergedAnalysis, raw1)
                appendConsistencyTrace(issues, trace)

                if (issues.any { it.kind == com.markolukarami.copilotclone.domain.entities.code.IssueKind.RETURN_MISMATCH }) {
                    trace += TraceStep("Consistency fix", "Re-prompting for return type mismatch", TraceType.MODEL)
                    val fixResult = attemptConsistencyFix(patchConfig, patchPrompt, issues, mergedAnalysis, trace)
                    if (fixResult != null) return fixResult
                }

                trace += TraceStep("Patch proposed", "files=${patch1.files.size}", TraceType.INFO)
                return ChatResult(
                    assistantText = buildPatchResponse(patch1.summary, issues),
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
                val issues = ConsistencyChecker.check(mergedAnalysis, raw2)
                appendConsistencyTrace(issues, trace)

                if (issues.any { it.kind == com.markolukarami.copilotclone.domain.entities.code.IssueKind.RETURN_MISMATCH }) {
                    trace += TraceStep("Consistency fix", "Re-prompting for return type mismatch", TraceType.MODEL)
                    val fixResult = attemptConsistencyFix(patchConfig, patchPrompt, issues, mergedAnalysis, trace)
                    if (fixResult != null) return fixResult
                }

                trace += TraceStep("Patch proposed", "files=${patch2.files.size}", TraceType.INFO)
                return ChatResult(
                    assistantText = buildPatchResponse(patch2.summary, issues),
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

        val normalHistory = getRecentHistory()
        if (normalHistory.isNotEmpty()) {
            finalMessages += normalHistory
            trace += TraceStep(
                "Conversation memory",
                "Included ${normalHistory.size} message(s) from history",
                TraceType.INFO
            )
        }

        finalMessages += ChatMessage(ChatRole.USER, userText)

        val answer = chatRepository.chat(config, finalMessages)
        return ChatResult(answer, trace, patch = null)
    }

    private fun getRecentHistory(): List<ChatMessage> {
        val sessionId = chatSessionRepository.getActiveSessionId()
        val all = chatSessionRepository.getMessages(sessionId)
        if (all.isEmpty()) return emptyList()

        val recentMessages = all.takeLast(MAX_HISTORY_TURNS * 2)

        val trimmed = mutableListOf<ChatMessage>()
        var totalChars = 0
        for (msg in recentMessages.reversed()) {
            if (totalChars + msg.content.length > MAX_HISTORY_CHARS) break
            trimmed.add(0, msg)
            totalChars += msg.content.length
        }
        return trimmed
    }

    private fun buildHistorySummary(history: List<ChatMessage>): String? {
        if (history.isEmpty()) return null
        val lines = history.joinToString("\n") { msg ->
            val role = when (msg.role) {
                ChatRole.USER -> "User"
                ChatRole.ASSISTANT -> "Assistant"
                ChatRole.SYSTEM -> "System"
            }
            "$role: ${msg.content.take(300)}"
        }
        return """
CONVERSATION HISTORY (recent):
$lines
"""
    }

    private fun mergeAnalyses(entries: List<FileAnalysisEntry>): CodeAnalysis {
        if (entries.isEmpty()) return CodeAnalysis(targetFilePath = "")
        if (entries.size == 1) return entries.first().analysis

        return CodeAnalysis(
            targetFilePath = entries.first().analysis.targetFilePath,
            imports = entries.flatMap { it.analysis.imports }.distinct(),
            methods = entries.flatMap { it.analysis.methods }.distinctBy { it.name },
            fields = entries.flatMap { it.analysis.fields }.distinctBy { it.name },
            similarMethods = entries.flatMap { it.analysis.similarMethods }.distinctBy { it.name }
        )
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

    private fun appendConsistencyTrace(issues: List<ConsistencyIssue>, trace: MutableList<TraceStep>) {
        if (issues.isEmpty()) {
            trace += TraceStep("Consistency check", "No issues found", TraceType.INFO)
        } else {
            val summary = issues.joinToString("; ") { "[${it.kind}] ${it.description}" }
            trace += TraceStep(
                "Consistency check",
                "${issues.size} issue(s): $summary",
                TraceType.ERROR
            )
        }
    }

    private fun attemptConsistencyFix(
        config: ModelConfig,
        originalPrompt: String,
        issues: List<ConsistencyIssue>,
        analysis: CodeAnalysis,
        trace: MutableList<TraceStep>
    ): ChatResult? {
        val issueDescriptions = issues.joinToString("\n") { "- [${it.kind}] ${it.description}" }

        val fixPrompt = """
The previous response had consistency issues:
$issueDescriptions

Fix these issues. Use the correct return type and parameter types as specified.

$originalPrompt
""".trimIndent()

        val fixMessages = listOf(
            ChatMessage(ChatRole.USER, fixPrompt)
        )

        val fixConfig = config.copy(
            temperature = 0.0,
            maxTokens = ModelTokenEstimator.compute(
                userOverride = config.maxResponseTokens,
                modelName = config.model,
                promptText = fixPrompt
            )
        )
        val fixRaw = chatRepository.chat(fixConfig, fixMessages)
        val fixPatch = PatchParser.parseOrNull(fixRaw)

        if (fixPatch != null && fixPatch.files.isNotEmpty()) {
            val fixIssues = ConsistencyChecker.check(analysis, fixRaw)
            appendConsistencyTrace(fixIssues, trace)
            trace += TraceStep("Consistency fix", "Fix succeeded", TraceType.INFO)
            return ChatResult(
                assistantText = buildPatchResponse(fixPatch.summary, fixIssues),
                trace = trace,
                patch = fixPatch
            )
        }

        trace += TraceStep("Consistency fix", "Fix attempt failed, using original", TraceType.ERROR)
        return null
    }

    private fun buildPatchResponse(summary: String, issues: List<ConsistencyIssue>): String {
        val base = "I prepared an edit patch:\n- $summary\nPress Apply to update your code."
        if (issues.isEmpty()) return base

        val warnings = issues.joinToString("\n") { "⚠ [${it.kind.name}] ${it.description}" }
        return "$base\n\nConsistency warnings:\n$warnings"
    }

}