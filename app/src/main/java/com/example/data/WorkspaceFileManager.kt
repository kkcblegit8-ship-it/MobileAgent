package com.example.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

class WorkspaceFileManager(private val context: Context) {

    private val workspaceRoot: File by lazy {
        File(context.filesDir, "nexus_workspace").apply {
            if (!exists()) mkdirs()
        }
    }

    private val _files = MutableStateFlow<List<WorkspaceFile>>(emptyList())
    val files: StateFlow<List<WorkspaceFile>> = _files.asStateFlow()

    suspend fun initializeAndRefresh() = withContext(Dispatchers.IO) {
        if (workspaceRoot.listFiles().isNullOrEmpty()) {
            seedDefaultWorkspaceFiles()
        }
        refreshFileList()
    }

    suspend fun refreshFileList() = withContext(Dispatchers.IO) {
        val list = workspaceRoot.walkTopDown()
            .filter { it.isFile }
            .map { file ->
                val rel = file.relativeTo(workspaceRoot).path
                val content = runCatching { file.readText() }.getOrDefault("")
                WorkspaceFile(
                    name = file.name,
                    relativePath = rel,
                    absolutePath = file.absolutePath,
                    extension = file.extension.lowercase(),
                    sizeBytes = file.length(),
                    lastModified = file.lastModified(),
                    content = content
                )
            }
            .sortedBy { it.relativePath }
            .toList()
        _files.value = list
    }

    suspend fun writeFile(relativePath: String, content: String): WorkspaceFile =
        withContext(Dispatchers.IO) {
            val cleanPath = relativePath.trimStart('/', '\\').ifBlank { "untitled.txt" }
            val targetFile = File(workspaceRoot, cleanPath)
            targetFile.parentFile?.mkdirs()
            targetFile.writeText(content)
            refreshFileList()
            WorkspaceFile(
                name = targetFile.name,
                relativePath = cleanPath,
                absolutePath = targetFile.absolutePath,
                extension = targetFile.extension.lowercase(),
                sizeBytes = targetFile.length(),
                lastModified = targetFile.lastModified(),
                content = content
            )
        }

    suspend fun readFile(relativePath: String): String? = withContext(Dispatchers.IO) {
        val cleanPath = relativePath.trimStart('/', '\\')
        val targetFile = File(workspaceRoot, cleanPath)
        if (targetFile.exists() && targetFile.isFile) {
            targetFile.readText()
        } else {
            // Try matching by file name
            workspaceRoot.walkTopDown().firstOrNull {
                it.isFile && it.name.equals(cleanPath, ignoreCase = true)
            }?.readText()
        }
    }

    suspend fun deleteFile(relativePath: String): Boolean = withContext(Dispatchers.IO) {
        val cleanPath = relativePath.trimStart('/', '\\')
        val targetFile = File(workspaceRoot, cleanPath)
        val deleted = if (targetFile.exists()) targetFile.delete() else false
        refreshFileList()
        deleted
    }

    suspend fun importDeviceUri(uri: Uri): WorkspaceFile? = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            var displayName = "imported_${System.currentTimeMillis()}.txt"
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    displayName = cursor.getString(nameIndex) ?: displayName
                }
            }
            val textContent = resolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: "// Binary or empty file imported from $uri"
            writeFile("imports/$displayName", textContent)
        }.getOrNull()
    }

    fun getWorkspaceSummaryForPrompt(): String {
        val current = _files.value
        if (current.isEmpty()) return "Workspace is empty."
        return current.joinToString("\n") { f ->
            "- ${f.relativePath} (${f.sizeBytes} bytes)"
        }
    }

    private fun seedDefaultWorkspaceFiles() {
        val youtubeScript = """
            # youtube_automation.py
            # Autonomous cross-app task script for NexusCode Agent
            
            TARGET_PACKAGE = "com.google.android.youtube"
            OVERLAY_HUD_ENABLED = True
            
            def open_youtube_and_research(topic: str):
                print(f"[NexusCode] Launching {TARGET_PACKAGE} with floating overlay...")
                print(f"[NexusCode] Executing search query: {topic}")
                return {
                    "action": "YOUTUBE_ACTION",
                    "query": topic,
                    "overlay": "PINNED_TOP_RIGHT"
                }
            
            if __name__ == "__main__":
                open_youtube_and_research("Android AI Agents & Kotlin Compose")
        """.trimIndent()

        val agentConfig = """
            # NEXUSCODE AGENT SYSTEM MANIFEST
            
            - **Agent Mode**: Autonomous Local Mobile Operator
            - **Overlay Engine**: WindowManager SYSTEM_ALERT_WINDOW + Live Mini HUD
            - **Cross-App Control**: AccessibilityService + Deep-Link Intent Router
            - **Voice Pipeline**: Low-Latency Android SpeechRecognizer + TTS
            
            ## Capabilities
            1. Open any installed app (YouTube, Chrome, Files, Social) with floating HUD pinned on top.
            2. Read & write local workspace code, notes, and automation scripts.
            3. Inspect UI hierarchy and automate taps/typing across apps.
        """.trimIndent()

        val quickMacroJson = """
            {
              "macroName": "YouTube Research & Note Sync",
              "version": "1.0.0",
              "overlayBubble": true,
              "steps": [
                {
                  "step": 1,
                  "action": "YOUTUBE_ACTION",
                  "target": "com.google.android.youtube",
                  "payload": "Kotlin Multiplatform & AI Agents"
                },
                {
                  "step": 2,
                  "action": "WRITE_FILE",
                  "target": "notes/research_log.md",
                  "payload": "# YouTube Research Session\nAutomatically logged by NexusCode Floating Agent."
                }
              ]
            }
        """.trimIndent()

        val shellCleaner = """
            #!/system/bin/sh
            # local_workspace_sync.sh
            echo "[NexusCode] Auditing local workspace directory..."
            ls -la
            echo "[NexusCode] Ready for zero-latency cross-app execution."
        """.trimIndent()

        File(workspaceRoot, "scripts/youtube_automation.py").apply {
            parentFile?.mkdirs()
            writeText(youtubeScript)
        }
        File(workspaceRoot, "README_AGENT.md").writeText(agentConfig)
        File(workspaceRoot, "macros/youtube_research.json").apply {
            parentFile?.mkdirs()
            writeText(quickMacroJson)
        }
        File(workspaceRoot, "scripts/local_workspace_sync.sh").writeText(shellCleaner)
    }
}
