package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

enum class ToolActionType {
    LAUNCH_APP,
    YOUTUBE_ACTION,
    OPEN_URL_OR_SEARCH,
    WRITE_FILE,
    READ_FILE,
    UI_AUTOMATION_STEP,
    SYSTEM_SHARE,
    CREATE_MACRO
}

data class AgentToolCall(
    val id: String,
    val actionType: ToolActionType,
    val target: String,
    val payload: String,
    val description: String,
    val status: String = "COMPLETED",
    val resultSummary: String = ""
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("actionType", actionType.name)
        put("target", target)
        put("payload", payload)
        put("description", description)
        put("status", status)
        put("resultSummary", resultSummary)
    }

    companion object {
        fun fromJson(obj: JSONObject): AgentToolCall {
            val typeStr = obj.optString("actionType", "LAUNCH_APP")
            val type = runCatching { ToolActionType.valueOf(typeStr) }
                .getOrDefault(ToolActionType.LAUNCH_APP)
            return AgentToolCall(
                id = obj.optString("id", System.currentTimeMillis().toString()),
                actionType = type,
                target = obj.optString("target", ""),
                payload = obj.optString("payload", ""),
                description = obj.optString("description", ""),
                status = obj.optString("status", "COMPLETED"),
                resultSummary = obj.optString("resultSummary", "")
            )
        }
    }
}

@Entity(tableName = "agent_messages")
data class AgentMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String, // "user" or "agent"
    val content: String,
    val thoughtProcess: String = "",
    val toolCallsJson: String = "[]",
    val source: String = "TERMINAL", // "TERMINAL", "OVERLAY_HUD", "VOICE"
    val latencyMs: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun parsedToolCalls(): List<AgentToolCall> {
        if (toolCallsJson.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(toolCallsJson)
            (0 until arr.length()).map { idx ->
                AgentToolCall.fromJson(arr.getJSONObject(idx))
            }
        }.getOrDefault(emptyList())
    }
}

data class MacroStep(
    val stepOrder: Int,
    val actionType: ToolActionType,
    val target: String,
    val payload: String,
    val label: String
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("stepOrder", stepOrder)
        put("actionType", actionType.name)
        put("target", target)
        put("payload", payload)
        put("label", label)
    }

    companion object {
        fun fromJson(obj: JSONObject): MacroStep {
            val typeStr = obj.optString("actionType", "LAUNCH_APP")
            val type = runCatching { ToolActionType.valueOf(typeStr) }
                .getOrDefault(ToolActionType.LAUNCH_APP)
            return MacroStep(
                stepOrder = obj.optInt("stepOrder", 1),
                actionType = type,
                target = obj.optString("target", ""),
                payload = obj.optString("payload", ""),
                label = obj.optString("label", "")
            )
        }
    }
}

@Entity(tableName = "automation_macros")
data class AutomationMacroEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val targetPackage: String,
    val triggerType: String, // "MANUAL", "OVERLAY_QUICK", "SCHEDULED"
    val intervalMinutes: Int = 0,
    val isEnabled: Boolean = true,
    val stepsJson: String = "[]",
    val runCount: Int = 0,
    val lastRunTimestamp: Long = 0L
) {
    fun parsedSteps(): List<MacroStep> {
        if (stepsJson.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(stepsJson)
            (0 until arr.length()).map { idx ->
                MacroStep.fromJson(arr.getJSONObject(idx))
            }
        }.getOrDefault(emptyList())
    }
}

@Entity(tableName = "execution_logs")
data class ExecutionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stepTitle: String,
    val detail: String,
    val targetAppOrFile: String,
    val status: String, // "RUNNING", "SUCCESS", "WARN", "ERROR"
    val timestamp: Long = System.currentTimeMillis()
)

data class WorkspaceFile(
    val name: String,
    val relativePath: String,
    val absolutePath: String,
    val extension: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val content: String
)

data class InstalledAppInfo(
    val appName: String,
    val packageName: String,
    val categoryTag: String,
    val isInstalledOnDevice: Boolean,
    val fallbackUrlOrScheme: String = "",
    val supportedActions: List<String> = listOf("Launch + Overlay", "Search / Deep-Link", "Auto-Click / Type")
)
