package com.example.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.example.data.InstalledAppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class DeviceAppController(private val context: Context) {

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()

    private val _activeTargetApp = MutableStateFlow("NexusCode Cockpit")
    val activeTargetApp: StateFlow<String> = _activeTargetApp.asStateFlow()

    suspend fun refreshInstalledApps() = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolved = runCatching {
            pm.queryIntentActivities(mainIntent, 0)
        }.getOrDefault(emptyList())

        val discoveredMap = mutableMapOf<String, InstalledAppInfo>()

        // Core high-utility app targets with deep-link & web fallbacks
        val coreCatalog = listOf(
            InstalledAppInfo(
                appName = "YouTube",
                packageName = "com.google.android.youtube",
                categoryTag = "MEDIA & VIDEO",
                isInstalledOnDevice = isPackageInstalled("com.google.android.youtube"),
                fallbackUrlOrScheme = "https://m.youtube.com",
                supportedActions = listOf("Launch + Pin Overlay", "Search Videos", "Open Subscriptions")
            ),
            InstalledAppInfo(
                appName = "Google Chrome / Browser",
                packageName = "com.android.chrome",
                categoryTag = "WEB & DOWNLOADS",
                isInstalledOnDevice = isPackageInstalled("com.android.chrome"),
                fallbackUrlOrScheme = "https://www.google.com",
                supportedActions = listOf("Launch + Pin Overlay", "Search Web", "Download / Inspect")
            ),
            InstalledAppInfo(
                appName = "Files & Storage",
                packageName = "com.google.android.documentsui",
                categoryTag = "LOCAL FILES",
                isInstalledOnDevice = isPackageInstalled("com.google.android.documentsui") ||
                    isPackageInstalled("com.android.documentsui"),
                fallbackUrlOrScheme = "content://com.android.externalstorage.documents/root/primary",
                supportedActions = listOf("Browse Phone Files", "Upload / Share", "Sync Workspace")
            ),
            InstalledAppInfo(
                appName = "System Settings",
                packageName = "com.android.settings",
                categoryTag = "SYSTEM CONTROL",
                isInstalledOnDevice = true,
                fallbackUrlOrScheme = Settings.ACTION_SETTINGS,
                supportedActions = listOf("Open Settings", "Overlay Permission", "Accessibility Agent")
            ),
            InstalledAppInfo(
                appName = "Gmail / Mail",
                packageName = "com.google.android.gm",
                categoryTag = "COMMUNICATION",
                isInstalledOnDevice = isPackageInstalled("com.google.android.gm"),
                fallbackUrlOrScheme = "mailto:",
                supportedActions = listOf("Draft Message", "Share File", "Launch + Overlay")
            ),
            InstalledAppInfo(
                appName = "Google Maps",
                packageName = "com.google.android.apps.maps",
                categoryTag = "NAVIGATION",
                isInstalledOnDevice = isPackageInstalled("com.google.android.apps.maps"),
                fallbackUrlOrScheme = "https://maps.google.com",
                supportedActions = listOf("Search Location", "Launch + Overlay")
            )
        )

        coreCatalog.forEach { discoveredMap[it.packageName] = it }

        for (info in resolved) {
            val pkg = info.activityInfo?.packageName ?: continue
            if (pkg == context.packageName) continue
            val label = runCatching { info.loadLabel(pm).toString() }.getOrDefault(pkg)
            if (!discoveredMap.containsKey(pkg)) {
                discoveredMap[pkg] = InstalledAppInfo(
                    appName = label,
                    packageName = pkg,
                    categoryTag = "INSTALLED APP",
                    isInstalledOnDevice = true
                )
            } else {
                val existing = discoveredMap[pkg]!!
                discoveredMap[pkg] = existing.copy(isInstalledOnDevice = true)
            }
        }

        _installedApps.value = discoveredMap.values.sortedWith(
            compareByDescending<InstalledAppInfo> { it.categoryTag != "INSTALLED APP" }
                .thenBy { it.appName }
        )
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return runCatching {
            context.packageManager.getLaunchIntentForPackage(packageName) != null
        }.getOrDefault(false)
    }

    fun canDrawOverlays(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun isAccessibilityEnabled(): Boolean {
        if (NexusAccessibilityService.isConnected.value) return true
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).orEmpty()
        return enabledServices.contains(context.packageName, ignoreCase = true)
    }

    fun openOverlayPermissionSettings() {
        runCatching {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun openAccessibilitySettings() {
        runCatching {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun ensureFloatingOverlayRunning(targetContextLabel: String = "") {
        if (targetContextLabel.isNotBlank()) {
            _activeTargetApp.value = targetContextLabel
        }
        FloatingOverlayService.setOverlayActiveState(true, _activeTargetApp.value)
        if (canDrawOverlays()) {
            runCatching {
                val serviceIntent = Intent(context, FloatingOverlayService::class.java).apply {
                    putExtra(FloatingOverlayService.EXTRA_TARGET_APP, _activeTargetApp.value)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
        }
    }

    fun stopFloatingOverlay() {
        FloatingOverlayService.setOverlayActiveState(false, "NexusCode Cockpit")
        runCatching {
            context.stopService(Intent(context, FloatingOverlayService::class.java))
        }
    }

    fun launchAppWithOverlay(targetNameOrPackage: String, optionalQuery: String = ""): String {
        val clean = targetNameOrPackage.trim()
        val match = _installedApps.value.firstOrNull {
            it.packageName.equals(clean, ignoreCase = true) ||
                it.appName.contains(clean, ignoreCase = true)
        }

        val displayLabel = match?.appName ?: clean.ifBlank { "External App" }
        ensureFloatingOverlayRunning(displayLabel)

        // Check if YouTube is requested or matched
        if (clean.contains("youtube", ignoreCase = true) ||
            match?.packageName == "com.google.android.youtube"
        ) {
            return openYouTubeWithOverlay(optionalQuery)
        }

        // Try direct package launch intent
        val pkgToTry = match?.packageName ?: clean
        val launchIntent = context.packageManager.getLaunchIntentForPackage(pkgToTry)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            return "Launched $displayLabel ($pkgToTry) with NexusCode Floating Agent Bubble pinned on screen."
        }

        // Try fallback URL or Settings action
        val fallback = match?.fallbackUrlOrScheme.orEmpty()
        if (fallback.startsWith("http")) {
            return openUrlOrSearch(fallback, displayLabel)
        } else if (fallback == Settings.ACTION_SETTINGS) {
            val settingsIntent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(settingsIntent)
            return "Opened System Settings with NexusCode Floating Agent Bubble active."
        }

        // Fallback to web search for the requested app/action
        val query = if (optionalQuery.isNotBlank()) "$clean $optionalQuery" else clean
        return openUrlOrSearch(query, displayLabel)
    }

    fun openYouTubeWithOverlay(searchQuery: String = ""): String {
        ensureFloatingOverlayRunning("YouTube")
        return runCatching {
            val youtubePkg = "com.google.android.youtube"
            if (isPackageInstalled(youtubePkg)) {
                if (searchQuery.isNotBlank()) {
                    val searchIntent = Intent(Intent.ACTION_SEARCH).apply {
                        setPackage(youtubePkg)
                        putExtra("query", searchQuery)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(searchIntent)
                } else {
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(youtubePkg)?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    if (launchIntent != null) {
                        context.startActivity(launchIntent)
                    }
                }
            } else {
                val url = if (searchQuery.isNotBlank()) {
                    "https://m.youtube.com/results?search_query=${Uri.encode(searchQuery)}"
                } else {
                    "https://m.youtube.com"
                }
                val ytIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(ytIntent)
            }
            if (searchQuery.isNotBlank()) {
                "Opened YouTube searching for \"$searchQuery\" with NexusCode Floating Bubble pinned on top."
            } else {
                "Opened YouTube with NexusCode Floating Bubble pinned on top."
            }
        }.getOrElse {
            val url = if (searchQuery.isNotBlank()) {
                "https://m.youtube.com/results?search_query=${Uri.encode(searchQuery)}"
            } else {
                "https://m.youtube.com"
            }
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            "Opened YouTube session for \"$searchQuery\" (Floating HUD active)."
        }
    }

    fun openUrlOrSearch(urlOrQuery: String, label: String = "Browser"): String {
        ensureFloatingOverlayRunning(label)
        val uri = if (urlOrQuery.startsWith("http://") || urlOrQuery.startsWith("https://")) {
            Uri.parse(urlOrQuery)
        } else {
            Uri.parse("https://www.google.com/search?q=${Uri.encode(urlOrQuery)}")
        }
        return runCatching {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Opened \"$urlOrQuery\" with Floating Agent Overlay active."
        }.getOrElse { e ->
            "Action dispatched ($urlOrQuery): ${e.localizedMessage ?: "Ready"}"
        }
    }

    fun shareOrPostContent(textContent: String, title: String = "NexusCode Post"): String {
        ensureFloatingOverlayRunning("System Share / Post")
        return runCatching {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, textContent)
            }
            val chooser = Intent.createChooser(sendIntent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            "Opened Android Share/Post sheet with payload (${textContent.length} chars)."
        }.getOrElse {
            "Prepared share payload: ${textContent.take(60)}..."
        }
    }

    fun executeAccessibilityStep(command: String, parameter: String): String {
        val service = NexusAccessibilityService.instance
            ?: return "Queued UI automation ($command -> \"$parameter\"). Enable NexusCode Accessibility Service in Settings for live cross-app tap/type injection."

        return when (command.uppercase()) {
            "CLICK" -> {
                val ok = service.clickNodeByText(parameter)
                if (ok) "Clicked UI element matching \"$parameter\"."
                else "Inspected window for \"$parameter\" (element not currently clickable)."
            }
            "TYPE", "INPUT" -> {
                val ok = service.inputText(parameter)
                if (ok) "Typed \"$parameter\" into active input field."
                else "No focused editable input field found in current window."
            }
            "SCROLL" -> {
                val ok = service.scrollForward()
                if (ok) "Scrolled active app window forward."
                else "Active window is not scrollable."
            }
            "BACK" -> {
                service.performBack()
                "Executed global Back action."
            }
            "HOME" -> {
                service.performHome()
                "Executed global Home action."
            }
            else -> {
                val nodes = service.refreshScreenNodes()
                "Captured ${nodes.size} UI nodes from ${NexusAccessibilityService.activePackage.value}."
            }
        }
    }
}
