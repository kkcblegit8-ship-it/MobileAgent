package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.agent.AgentOrchestrator
import com.example.agent.AgentRunState
import com.example.data.AgentMessageEntity
import com.example.data.AutomationMacroEntity
import com.example.data.ExecutionLogEntity
import com.example.data.InstalledAppInfo
import com.example.data.MacroStep
import com.example.data.ToolActionType
import com.example.data.WorkspaceFile
import com.example.service.FloatingOverlayService
import com.example.service.NexusAccessibilityService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray

enum class CockpitTab {
    TERMINAL,
    WORKSPACE,
    APPS_OVERLAY,
    AUTOMATIONS
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val orchestrator = AgentOrchestrator.getInstance(application)

    private val _currentTab = MutableStateFlow(CockpitTab.TERMINAL)
    val currentTab: StateFlow<CockpitTab> = _currentTab.asStateFlow()

    // In-app interactive floating overlay bubble state (mirrored with system FloatingOverlayService)
    private val _showFloatingBubble = MutableStateFlow(true)
    val showFloatingBubble: StateFlow<Boolean> = _showFloatingBubble.asStateFlow()

    private val _isFloatingHudExpanded = MutableStateFlow(false)
    val isFloatingHudExpanded: StateFlow<Boolean> = _isFloatingHudExpanded.asStateFlow()

    val isSystemOverlayActive: StateFlow<Boolean> = FloatingOverlayService.isOverlayActive
    val currentTargetApp: StateFlow<String> = FloatingOverlayService.currentTargetApp

    val runState: StateFlow<AgentRunState> = orchestrator.runState
    val liveOverlayStep: StateFlow<String> = orchestrator.liveOverlayStep

    val messages: StateFlow<List<AgentMessageEntity>> = orchestrator.dao.getAllMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val macros: StateFlow<List<AutomationMacroEntity>> = orchestrator.dao.getAllMacros()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val executionLogs: StateFlow<List<ExecutionLogEntity>> = orchestrator.dao.getRecentLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val workspaceFiles: StateFlow<List<WorkspaceFile>> = orchestrator.fileManager.files
    val installedApps: StateFlow<List<InstalledAppInfo>> = orchestrator.appController.installedApps

    val isVoiceListening: StateFlow<Boolean> = orchestrator.voiceEngine.isListening
    val liveVoiceTranscript: StateFlow<String> = orchestrator.voiceEngine.liveTranscript
    val voiceStatus: StateFlow<String> = orchestrator.voiceEngine.voiceStatus
    val isTtsEnabled: StateFlow<Boolean> = orchestrator.voiceEngine.isTtsEnabled

    val isAccessibilityConnected: StateFlow<Boolean> = NexusAccessibilityService.isConnected
    val activeForegroundPackage: StateFlow<String> = NexusAccessibilityService.activePackage
    val screenNodes: StateFlow<List<String>> = NexusAccessibilityService.screenNodes

    // Workspace code editor state
    private val _selectedFilePath = MutableStateFlow("scripts/youtube_automation.py")
    val selectedFilePath: StateFlow<String> = _selectedFilePath.asStateFlow()

    private val _editorText = MutableStateFlow("")
    val editorText: StateFlow<String> = _editorText.asStateFlow()

    private val _editorStatus = MutableStateFlow("Saved on disk")
    val editorStatus: StateFlow<String> = _editorStatus.asStateFlow()

    init {
        viewModelScope.launch {
            orchestrator.fileManager.files.collect { fileList ->
                if (fileList.isNotEmpty() && _editorText.value.isEmpty()) {
                    val target = fileList.firstOrNull { it.relativePath == _selectedFilePath.value }
                        ?: fileList.first()
                    _selectedFilePath.value = target.relativePath
                    _editorText.value = target.content
                }
            }
        }
    }

    fun selectTab(tab: CockpitTab) {
        _currentTab.value = tab
    }

    fun submitPrompt(prompt: String, source: String = "TERMINAL") {
        orchestrator.submitPrompt(prompt, source)
    }

    fun toggleFloatingBubble(enabled: Boolean) {
        _showFloatingBubble.value = enabled
        if (enabled) {
            orchestrator.appController.ensureFloatingOverlayRunning(currentTargetApp.value)
        } else {
            _isFloatingHudExpanded.value = false
            orchestrator.appController.stopFloatingOverlay()
        }
    }

    fun setFloatingHudExpanded(expanded: Boolean) {
        _isFloatingHudExpanded.value = expanded
    }

    fun launchAppWithFloatingAgent(appNameOrPkg: String, searchQuery: String = "") {
        _showFloatingBubble.value = true
        viewModelScope.launch {
            orchestrator.executeSingleTool(
                actionType = if (appNameOrPkg.contains("youtube", ignoreCase = true)) {
                    ToolActionType.YOUTUBE_ACTION
                } else {
                    ToolActionType.LAUNCH_APP
                },
                target = appNameOrPkg,
                payload = searchQuery,
                description = "Launch $appNameOrPkg with Floating Agent Overlay"
            )
        }
    }

    fun startOrStopVoiceInput(source: String = "VOICE") {
        if (orchestrator.voiceEngine.isListening.value) {
            orchestrator.voiceEngine.stopListening()
        } else {
            orchestrator.voiceEngine.startListening { transcript ->
                if (transcript.isNotBlank()) {
                    orchestrator.submitPrompt(transcript, source = source)
                }
            }
        }
    }

    fun toggleTts() {
        orchestrator.voiceEngine.toggleTts()
    }

    fun selectWorkspaceFile(file: WorkspaceFile) {
        _selectedFilePath.value = file.relativePath
        _editorText.value = file.content
        _editorStatus.value = "Loaded ${file.sizeBytes}B"
    }

    fun updateEditorText(newText: String) {
        _editorText.value = newText
        _editorStatus.value = "Unsaved changes *"
    }

    fun saveCurrentFile() {
        val path = _selectedFilePath.value
        val content = _editorText.value
        viewModelScope.launch {
            val saved = orchestrator.fileManager.writeFile(path, content)
            _editorStatus.value = "Saved (${saved.sizeBytes}B)"
            orchestrator.dao.insertLog(
                ExecutionLogEntity(
                    stepTitle = "Saved Local File",
                    detail = "Wrote ${saved.sizeBytes} bytes to ${saved.relativePath}",
                    targetAppOrFile = saved.relativePath,
                    status = "SUCCESS"
                )
            )
        }
    }

    fun createNewWorkspaceFile(relativePath: String, initialContent: String) {
        viewModelScope.launch {
            val saved = orchestrator.fileManager.writeFile(relativePath, initialContent)
            _selectedFilePath.value = saved.relativePath
            _editorText.value = saved.content
            _editorStatus.value = "Created ${saved.relativePath}"
        }
    }

    fun deleteCurrentWorkspaceFile() {
        val path = _selectedFilePath.value
        viewModelScope.launch {
            orchestrator.fileManager.deleteFile(path)
            val remaining = orchestrator.fileManager.files.value
            if (remaining.isNotEmpty()) {
                selectWorkspaceFile(remaining.first())
            } else {
                _selectedFilePath.value = "notes/scratchpad.md"
                _editorText.value = ""
            }
        }
    }

    fun importExternalFile(uri: Uri) {
        viewModelScope.launch {
            val imported = orchestrator.fileManager.importDeviceUri(uri)
            if (imported != null) {
                selectWorkspaceFile(imported)
                orchestrator.dao.insertLog(
                    ExecutionLogEntity(
                        stepTitle = "Imported Device File",
                        detail = "Mounted ${imported.relativePath} (${imported.sizeBytes} bytes)",
                        targetAppOrFile = imported.relativePath,
                        status = "SUCCESS"
                    )
                )
            }
        }
    }

    fun askAgentToRefactorCurrentFile(instruction: String) {
        saveCurrentFile()
        val path = _selectedFilePath.value
        val currentCode = _editorText.value
        val prompt = """
            Update local file `$path` according to this instruction: $instruction
            Current content of `$path`:
            ```
            $currentCode
            ```
            Use WRITE_FILE with target="$path" and the complete updated code in payload.
        """.trimIndent()
        _currentTab.value = CockpitTab.TERMINAL
        orchestrator.submitPrompt(prompt, source = "WORKSPACE_IDE")
    }

    fun shareCurrentFileToApps() {
        saveCurrentFile()
        orchestrator.appController.shareOrPostContent(
            textContent = _editorText.value,
            title = _selectedFilePath.value
        )
    }

    fun createMacro(
        name: String,
        description: String,
        targetApp: String,
        actionType: ToolActionType,
        payload: String,
        intervalMinutes: Int
    ) {
        viewModelScope.launch {
            val stepsArr = JSONArray().apply {
                put(
                    MacroStep(
                        stepOrder = 1,
                        actionType = actionType,
                        target = targetApp,
                        payload = payload,
                        label = "$actionType on $targetApp"
                    ).toJson()
                )
            }
            orchestrator.dao.insertMacro(
                AutomationMacroEntity(
                    name = name,
                    description = description,
                    targetPackage = targetApp,
                    triggerType = if (intervalMinutes > 0) "SCHEDULED" else "OVERLAY_QUICK",
                    intervalMinutes = intervalMinutes,
                    isEnabled = true,
                    stepsJson = stepsArr.toString()
                )
            )
        }
    }

    fun runMacro(macro: AutomationMacroEntity) {
        orchestrator.runMacro(macro)
    }

    fun deleteMacro(id: Long) {
        viewModelScope.launch {
            orchestrator.dao.deleteMacroById(id)
        }
    }

    fun executeAccessibilityAction(cmd: String, param: String) {
        viewModelScope.launch {
            orchestrator.executeSingleTool(
                actionType = ToolActionType.UI_AUTOMATION_STEP,
                target = cmd,
                payload = param,
                description = "UI Automation: $cmd $param"
            )
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            orchestrator.dao.clearMessages()
            orchestrator.dao.clearLogs()
        }
    }

    fun canDrawOverlays(): Boolean = orchestrator.appController.canDrawOverlays()
    fun isAccessibilityEnabled(): Boolean = orchestrator.appController.isAccessibilityEnabled()
    fun openOverlaySettings() = orchestrator.appController.openOverlayPermissionSettings()
    fun openAccessibilitySettings() = orchestrator.appController.openAccessibilitySettings()
}
