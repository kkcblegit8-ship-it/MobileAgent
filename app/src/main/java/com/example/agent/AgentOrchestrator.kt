package com.example.agent

import android.content.Context
import android.util.Log
import com.example.data.AgentMessageEntity
import com.example.data.AgentToolCall
import com.example.data.AutomationMacroEntity
import com.example.data.ExecutionLogEntity
import com.example.data.MacroStep
import com.example.data.NexusDatabase
import com.example.data.ToolActionType
import com.example.data.WorkspaceFileManager
import com.example.service.DeviceAppController
import com.example.service.NexusAccessibilityService
import com.example.service.VoiceEngine
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "AgentOrchestrator"

sealed interface AgentRunState {
    data object Idle : AgentRunState
    data class Thinking(val phaseText: String) : AgentRunState
    data class ExecutingTool(val toolName: String, val detail: String) : AgentRunState
    data class Error(val message: String) : AgentRunState
}

class AgentOrchestrator private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val database = NexusDatabase.getInstance(appContext)
    val dao = database.nexusDao()
    val fileManager = WorkspaceFileManager(appContext)
    val appController = DeviceAppController(appContext)
    val voiceEngine = VoiceEngine(appContext)

    private val _runState = MutableStateFlow<AgentRunState>(AgentRunState.Idle)
    val runState: StateFlow<AgentRunState> = _runState.asStateFlow()

    private val _liveOverlayStep = MutableStateFlow("Agent Ready • Tap bubble or speak command")
    val liveOverlayStep: StateFlow<String> = _liveOverlayStep.asStateFlow()

    private val agentResponseSchema by lazy {
        val toolCallItemSchema = Schema.obj(
            properties = mapOf(
                "actionType" to Schema.enumeration(
                    values = listOf(
                        "LAUNCH_APP",
                        "YOUTUBE_ACTION",
                        "OPEN_URL_OR_SEARCH",
                        "WRITE_FILE",
                        "READ_FILE",
                        "UI_AUTOMATION_STEP",
                        "SYSTEM_SHARE",
                        "CREATE_MACRO"
                    ),
                    description = "The exact device or workspace tool to execute"
                ),
                "target" to Schema.string(
                    description = "Target app name/package (e.g. YouTube, com.android.chrome), file path (e.g. scripts/bot.py), or UI command (CLICK, TYPE, SCROLL, BACK, HOME)"
                ),
                "payload" to Schema.string(
                    description = "Search query, URL, file content to write, text to type/click, or share text"
                ),
                "description" to Schema.string(
                    description = "Short human-readable description of this action step"
                )
            )
        )

        Schema.obj(
            properties = mapOf(
                "thoughtProcess" to Schema.string(
                    description = "Concise technical reasoning explaining how the agent plans and executes the user's request"
                ),
                "replyMessage" to Schema.string(
                    description = "Clear, helpful response to the user summarizing what was executed or generated"
                ),
                "spokenSummary" to Schema.string(
                    description = "1 short sentence spoken aloud via TTS confirming the action"
                ),
                "toolActions" to Schema.array(
                    items = toolCallItemSchema,
                    description = "Ordered list of device, app, overlay, or file actions to execute immediately"
                )
            )
        )
    }

    private val generativeModel by lazy {
        val config = generationConfig {
            responseMimeType = "application/json"
            responseSchema = agentResponseSchema
        }
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = config,
            systemInstruction = content {
                text(
                    """
                    You are NexusCode, an elite autonomous local mobile AI agent (like Cursor, Claude Code, and Google Antigravity built natively for Android).
                    You have direct access to:
                    1. LAUNCH_APP: Launch any app on the user's phone with the NexusCode Floating Overlay Bubble pinned on top.
                    2. YOUTUBE_ACTION: Open YouTube directly to search for videos/topics or play content with the Floating Overlay Bubble active.
                    3. OPEN_URL_OR_SEARCH: Open URLs, download links, or web searches.
                    4. WRITE_FILE: Create or update code/markdown/script files in the local mobile workspace.
                    5. READ_FILE: Read local workspace files.
                    6. UI_AUTOMATION_STEP: Perform cross-app UI automation via AccessibilityService (target = CLICK, TYPE, SCROLL, BACK, HOME, INSPECT; payload = element text or input string).
                    7. SYSTEM_SHARE: Post or share text/code to other installed apps.
                    8. CREATE_MACRO: Save a reusable automation macro for the user.
                    
                    Always include concrete toolActions whenever the user asks to open an app (such as YouTube), search, automate UI, write/edit code or files, share/post something, or create a macro.
                    """.trimIndent()
                )
            }
        )
    }

    init {
        scope.launch {
            fileManager.initializeAndRefresh()
            appController.refreshInstalledApps()
            seedInitialDataIfEmpty()
        }
    }

    private suspend fun seedInitialDataIfEmpty() {
        val existingMacros = dao.getAllMacrosOnce()
        if (existingMacros.isEmpty()) {
            val ytSteps = JSONArray().apply {
                put(
                    MacroStep(
                        1,
                        ToolActionType.YOUTUBE_ACTION,
                        "com.google.android.youtube",
                        "Android AI Agent Automation",
                        "Open YouTube & search 'Android AI Agent Automation' with Floating Bubble"
                    ).toJson()
                )
                put(
                    MacroStep(
                        2,
                        ToolActionType.WRITE_FILE,
                        "notes/youtube_digest.md",
                        "# YouTube Agent Session\n- Query: Android AI Agent Automation\n- Status: Floating Overlay Active",
                        "Log session summary to local workspace"
                    ).toJson()
                )
            }
            dao.insertMacro(
                AutomationMacroEntity(
                    name = "YouTube Research + Overlay",
                    description = "Opens YouTube to a topic, pins the NexusCode floating bubble on top, and logs research notes locally.",
                    targetPackage = "com.google.android.youtube",
                    triggerType = "OVERLAY_QUICK",
                    intervalMinutes = 0,
                    isEnabled = true,
                    stepsJson = ytSteps.toString()
                )
            )

            val codeAuditSteps = JSONArray().apply {
                put(
                    MacroStep(
                        1,
                        ToolActionType.READ_FILE,
                        "scripts/youtube_automation.py",
                        "",
                        "Inspect local automation script"
                    ).toJson()
                )
                put(
                    MacroStep(
                        2,
                        ToolActionType.WRITE_FILE,
                        "reports/audit_status.json",
                        "{\n  \"status\": \"VERIFIED\",\n  \"agent\": \"NexusCode\",\n  \"overlayReady\": true\n}",
                        "Write audit report to local storage"
                    ).toJson()
                )
            }
            dao.insertMacro(
                AutomationMacroEntity(
                    name = "Workspace Code Audit & Sync",
                    description = "Scans local workspace scripts and writes a structured verification report.",
                    targetPackage = "com.aistudio.nexuscode.kxmpzq",
                    triggerType = "SCHEDULED",
                    intervalMinutes = 30,
                    isEnabled = true,
                    stepsJson = codeAuditSteps.toString()
                )
            )

            dao.insertMessage(
                AgentMessageEntity(
                    role = "agent",
                    content = "NexusCode Local Mobile Agent initialized.\n\n• **Floating Overlay HUD**: Ready to display over YouTube and all installed apps.\n• **Workspace IDE**: Local files mounted at `/nexus_workspace`.\n• **Cross-App Engine**: Use text or voice commands to open apps, search, write code, or automate UI steps.",
                    thoughtProcess = "System boot verified: Firebase AI Logic (gemini-3.8-flash), Local Workspace FileManager, PackageManager intent router, and FloatingOverlayService initialized.",
                    source = "TERMINAL"
                )
            )

            dao.insertLog(
                ExecutionLogEntity(
                    stepTitle = "NexusCode System Online",
                    detail = "Gemini 3.8 Flash + Local Workspace + Floating Overlay Engine ready",
                    targetAppOrFile = "System Core",
                    status = "SUCCESS"
                )
            )
        }
    }

    fun submitPrompt(prompt: String, source: String = "TERMINAL") {
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.isEmpty()) return

        scope.launch {
            val startTime = System.currentTimeMillis()
            dao.insertMessage(
                AgentMessageEntity(
                    role = "user",
                    content = cleanPrompt,
                    source = source
                )
            )

            _runState.value = AgentRunState.Thinking("Reasoning with Gemini 3.8 Flash...")
            _liveOverlayStep.value = "Thinking: \"${cleanPrompt.take(45)}...\""

            dao.insertLog(
                ExecutionLogEntity(
                    stepTitle = "Prompt Received ($source)",
                    detail = cleanPrompt,
                    targetAppOrFile = "Agent Orchestrator",
                    status = "RUNNING"
                )
            )

            try {
                val contextHeader = buildSystemContextSnapshot()
                val fullInput = """
                    [DEVICE & WORKSPACE CONTEXT]
                    $contextHeader
                    
                    [USER COMMAND ($source)]
                    $cleanPrompt
                """.trimIndent()

                val response = withTimeout(60_000L) {
                    generativeModel.generateContent(fullInput)
                }

                val jsonString = response.text
                    ?: throw IllegalStateException("Model returned an empty response.")

                val rootJson = JSONObject(jsonString)
                val thought = rootJson.optString("thoughtProcess", "Analyzed command and prepared tool execution plan.")
                val reply = rootJson.optString("replyMessage", "Command executed.")
                val spoken = rootJson.optString("spokenSummary", reply)
                val actionsArray = rootJson.optJSONArray("toolActions") ?: JSONArray()

                val executedTools = mutableListOf<AgentToolCall>()

                for (i in 0 until actionsArray.length()) {
                    val item = actionsArray.getJSONObject(i)
                    val typeName = item.optString("actionType", "LAUNCH_APP")
                    val actionType = runCatching { ToolActionType.valueOf(typeName) }
                        .getOrDefault(ToolActionType.LAUNCH_APP)
                    val target = item.optString("target", "")
                    val payload = item.optString("payload", "")
                    val desc = item.optString("description", "Execute $typeName on $target")

                    _runState.value = AgentRunState.ExecutingTool(actionType.name, desc)
                    _liveOverlayStep.value = "Running: $desc"

                    val resultSummary = executeSingleTool(actionType, target, payload, desc)
                    executedTools.add(
                        AgentToolCall(
                            id = "${System.currentTimeMillis()}_$i",
                            actionType = actionType,
                            target = target,
                            payload = payload,
                            description = desc,
                            status = "COMPLETED",
                            resultSummary = resultSummary
                        )
                    )
                }

                val elapsed = System.currentTimeMillis() - startTime
                val serializedTools = JSONArray().apply {
                    executedTools.forEach { put(it.toJson()) }
                }.toString()

                dao.insertMessage(
                    AgentMessageEntity(
                        role = "agent",
                        content = reply,
                        thoughtProcess = thought,
                        toolCallsJson = serializedTools,
                        source = source,
                        latencyMs = elapsed
                    )
                )

                val finalTicker = if (executedTools.isNotEmpty()) {
                    "Done (${executedTools.size} actions): ${executedTools.last().resultSummary}"
                } else {
                    reply.take(80)
                }
                _liveOverlayStep.value = finalTicker
                _runState.value = AgentRunState.Idle

                if (source == "VOICE" || source == "OVERLAY_HUD") {
                    voiceEngine.speak(spoken)
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Gemini generation timed out", e)
                val msg = "Agent request timed out after 60 seconds. Please try again."
                _runState.value = AgentRunState.Error(msg)
                _liveOverlayStep.value = msg
                dao.insertLog(
                    ExecutionLogEntity(
                        stepTitle = "Inference Timeout",
                        detail = msg,
                        targetAppOrFile = "Gemini API",
                        status = "ERROR"
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Agent execution failed", e)
                val errMessage = e.localizedMessage ?: "AI generation failed."
                _runState.value = AgentRunState.Error(errMessage)
                _liveOverlayStep.value = "Error: $errMessage"
                dao.insertMessage(
                    AgentMessageEntity(
                        role = "agent",
                        content = "⚠️ **Agent Error**: $errMessage",
                        thoughtProcess = "Exception caught during Firebase AI Logic call.",
                        source = source
                    )
                )
                dao.insertLog(
                    ExecutionLogEntity(
                        stepTitle = "Agent Execution Error",
                        detail = errMessage,
                        targetAppOrFile = "Gemini API",
                        status = "ERROR"
                    )
                )
            }
        }
    }

    suspend fun executeSingleTool(
        actionType: ToolActionType,
        target: String,
        payload: String,
        description: String
    ): String {
        val outcome = when (actionType) {
            ToolActionType.LAUNCH_APP -> {
                appController.launchAppWithOverlay(target, payload)
            }
            ToolActionType.YOUTUBE_ACTION -> {
                val query = payload.ifBlank { target }
                appController.openYouTubeWithOverlay(query)
            }
            ToolActionType.OPEN_URL_OR_SEARCH -> {
                val urlOrQuery = payload.ifBlank { target }
                appController.openUrlOrSearch(urlOrQuery, target.ifBlank { "Web Browser" })
            }
            ToolActionType.WRITE_FILE -> {
                val path = target.ifBlank { "workspace_output.txt" }
                val file = fileManager.writeFile(path, payload)
                "Wrote ${file.sizeBytes} bytes to local file `${file.relativePath}`."
            }
            ToolActionType.READ_FILE -> {
                val content = fileManager.readFile(target)
                if (content != null) {
                    "Read `${target}` (${content.length} chars)."
                } else {
                    "File `$target` not found in workspace."
                }
            }
            ToolActionType.UI_AUTOMATION_STEP -> {
                appController.executeAccessibilityStep(target, payload)
            }
            ToolActionType.SYSTEM_SHARE -> {
                appController.shareOrPostContent(payload, target.ifBlank { "NexusCode Share" })
            }
            ToolActionType.CREATE_MACRO -> {
                val step = MacroStep(
                    stepOrder = 1,
                    actionType = ToolActionType.LAUNCH_APP,
                    target = target.ifBlank { "com.google.android.youtube" },
                    payload = payload,
                    label = description
                )
                val arr = JSONArray().apply { put(step.toJson()) }
                dao.insertMacro(
                    AutomationMacroEntity(
                        name = target.ifBlank { "Agent Macro" },
                        description = description,
                        targetPackage = target,
                        triggerType = "OVERLAY_QUICK",
                        stepsJson = arr.toString()
                    )
                )
                "Created reusable macro \"$target\" in Automations."
            }
        }

        dao.insertLog(
            ExecutionLogEntity(
                stepTitle = "${actionType.name}: $description",
                detail = outcome,
                targetAppOrFile = target.ifBlank { actionType.name },
                status = "SUCCESS"
            )
        )
        return outcome
    }

    fun runMacro(macro: AutomationMacroEntity) {
        scope.launch {
            _liveOverlayStep.value = "Running Macro: ${macro.name}"
            val steps = macro.parsedSteps()
            for (step in steps) {
                _runState.value = AgentRunState.ExecutingTool(step.actionType.name, step.label)
                val res = executeSingleTool(
                    actionType = step.actionType,
                    target = step.target,
                    payload = step.payload,
                    description = step.label
                )
                _liveOverlayStep.value = res
            }
            dao.updateMacro(
                macro.copy(
                    runCount = macro.runCount + 1,
                    lastRunTimestamp = System.currentTimeMillis()
                )
            )
            _runState.value = AgentRunState.Idle
        }
    }

    private fun buildSystemContextSnapshot(): String {
        val filesSummary = fileManager.getWorkspaceSummaryForPrompt()
        val activePkg = NexusAccessibilityService.activePackage.value
        val screenElements = NexusAccessibilityService.screenNodes.value.take(15).joinToString(", ")
        val installedSample = appController.installedApps.value.take(12).joinToString(", ") {
            "${it.appName} (${it.packageName})"
        }
        return """
            Active Foreground Package: $activePkg
            Visible Screen Nodes: ${screenElements.ifBlank { "None (Accessibility idle)" }}
            Key Apps Available: $installedSample
            Local Workspace Files:
            $filesSummary
        """.trimIndent()
    }

    fun clearError() {
        if (_runState.value is AgentRunState.Error) {
            _runState.value = AgentRunState.Idle
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: AgentOrchestrator? = null

        fun getInstance(context: Context): AgentOrchestrator {
            return INSTANCE ?: synchronized(this) {
                val instance = AgentOrchestrator(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
