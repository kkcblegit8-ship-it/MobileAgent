package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppsOverlayScreen
import com.example.ui.AutomationsScreen
import com.example.ui.CockpitTab
import com.example.ui.FloatingAgentHudOverlay
import com.example.ui.MainViewModel
import com.example.ui.TerminalScreen
import com.example.ui.WorkspaceScreen
import com.example.ui.theme.ElevatedPanel
import com.example.ui.theme.NexusCodeTheme
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TerminalEmerald
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.configureAppCheck

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            NexusCodeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NexusCodeCockpitApp()
                }
            }
        }
    }
}

@Composable
fun NexusCodeCockpitApp(
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current

    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val macros by viewModel.macros.collectAsStateWithLifecycle()
    val executionLogs by viewModel.executionLogs.collectAsStateWithLifecycle()
    val workspaceFiles by viewModel.workspaceFiles.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val runState by viewModel.runState.collectAsStateWithLifecycle()
    val liveOverlayStep by viewModel.liveOverlayStep.collectAsStateWithLifecycle()

    val showFloatingBubble by viewModel.showFloatingBubble.collectAsStateWithLifecycle()
    val isFloatingHudExpanded by viewModel.isFloatingHudExpanded.collectAsStateWithLifecycle()
    val currentTargetApp by viewModel.currentTargetApp.collectAsStateWithLifecycle()

    val isVoiceListening by viewModel.isVoiceListening.collectAsStateWithLifecycle()
    val liveVoiceTranscript by viewModel.liveVoiceTranscript.collectAsStateWithLifecycle()
    val voiceStatus by viewModel.voiceStatus.collectAsStateWithLifecycle()
    val isTtsEnabled by viewModel.isTtsEnabled.collectAsStateWithLifecycle()

    val isAccessibilityConnected by viewModel.isAccessibilityConnected.collectAsStateWithLifecycle()
    val activeForegroundPackage by viewModel.activeForegroundPackage.collectAsStateWithLifecycle()
    val screenNodes by viewModel.screenNodes.collectAsStateWithLifecycle()

    val selectedFilePath by viewModel.selectedFilePath.collectAsStateWithLifecycle()
    val editorText by viewModel.editorText.collectAsStateWithLifecycle()
    val editorStatus by viewModel.editorStatus.collectAsStateWithLifecycle()

    // BackHandler for secondary tabs or expanded Floating HUD
    BackHandler(enabled = isFloatingHudExpanded || currentTab != CockpitTab.TERMINAL) {
        if (isFloatingHudExpanded) {
            viewModel.setFloatingHudExpanded(false)
        } else {
            viewModel.selectTab(CockpitTab.TERMINAL)
        }
    }

    // Runtime permission launcher for Microphone / Voice recognition
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startOrStopVoiceInput(source = "VOICE")
        }
    }

    val triggerVoiceWithPermission: () -> Unit = {
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPerm) {
            viewModel.startOrStopVoiceInput(source = "VOICE")
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Storage Access Framework (SAF) file picker to mount any local file from the phone
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importExternalFile(uri)
        }
    }

    val navItems = listOf(
        Triple(CockpitTab.TERMINAL, stringResource(R.string.nav_terminal), Icons.Default.Terminal),
        Triple(CockpitTab.WORKSPACE, stringResource(R.string.nav_workspace), Icons.Default.Code),
        Triple(CockpitTab.APPS_OVERLAY, stringResource(R.string.nav_apps), Icons.Default.Apps),
        Triple(CockpitTab.AUTOMATIONS, stringResource(R.string.nav_macros), Icons.Default.AutoMode)
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = ObsidianBg,
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar(
                        containerColor = TerminalSurface,
                        tonalElevation = 0.dp
                    ) {
                        navItems.forEach { (tab, label, icon) ->
                            val selected = currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label
                                    )
                                },
                                label = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = TerminalEmerald,
                                    selectedTextColor = TerminalEmerald,
                                    indicatorColor = ElevatedPanel,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (isWideScreen) {
                        NavigationRail(
                            containerColor = TerminalSurface
                        ) {
                            navItems.forEach { (tab, label, icon) ->
                                NavigationRailItem(
                                    selected = currentTab == tab,
                                    onClick = { viewModel.selectTab(tab) },
                                    icon = { Icon(icon, contentDescription = label) },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("nav_rail_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        when (currentTab) {
                            CockpitTab.TERMINAL -> TerminalScreen(
                                messages = messages,
                                runState = runState,
                                showFloatingBubble = showFloatingBubble,
                                canDrawOverlays = viewModel.canDrawOverlays(),
                                isAccessibilityEnabled = isAccessibilityConnected || viewModel.isAccessibilityEnabled(),
                                isListening = isVoiceListening,
                                liveTranscript = liveVoiceTranscript,
                                voiceStatus = voiceStatus,
                                isTtsEnabled = isTtsEnabled,
                                onSubmitPrompt = { viewModel.submitPrompt(it, "TERMINAL") },
                                onToggleFloatingBubble = { viewModel.toggleFloatingBubble(it) },
                                onExpandFloatingHud = { viewModel.setFloatingHudExpanded(true) },
                                onOpenOverlayPermission = { viewModel.openOverlaySettings() },
                                onOpenAccessibilitySettings = { viewModel.openAccessibilitySettings() },
                                onLaunchYouTubeWithOverlay = {
                                    viewModel.launchAppWithFloatingAgent(
                                        "YouTube",
                                        "Android AI Agents & Kotlin Compose"
                                    )
                                },
                                onToggleVoice = triggerVoiceWithPermission,
                                onToggleTts = { viewModel.toggleTts() },
                                onRequestMicPermissionAndListen = triggerVoiceWithPermission,
                                onClearHistory = { viewModel.clearHistory() }
                            )

                            CockpitTab.WORKSPACE -> WorkspaceScreen(
                                files = workspaceFiles,
                                selectedFilePath = selectedFilePath,
                                editorText = editorText,
                                editorStatus = editorStatus,
                                onSelectFile = { viewModel.selectWorkspaceFile(it) },
                                onEditorTextChange = { viewModel.updateEditorText(it) },
                                onSaveFile = { viewModel.saveCurrentFile() },
                                onCreateNewFile = { path, content ->
                                    viewModel.createNewWorkspaceFile(path, content)
                                },
                                onDeleteCurrentFile = { viewModel.deleteCurrentWorkspaceFile() },
                                onImportDeviceFile = {
                                    documentPickerLauncher.launch(arrayOf("*/*"))
                                },
                                onAskAgentToRefactor = { instruction ->
                                    viewModel.askAgentToRefactorCurrentFile(instruction)
                                },
                                onRunOrExecuteFile = {
                                    viewModel.saveCurrentFile()
                                    viewModel.selectTab(CockpitTab.TERMINAL)
                                    viewModel.submitPrompt(
                                        "Execute the automation workflow in local file `$selectedFilePath`:\n```\n$editorText\n```",
                                        source = "WORKSPACE_RUNNER"
                                    )
                                },
                                onShareFile = { viewModel.shareCurrentFileToApps() }
                            )

                            CockpitTab.APPS_OVERLAY -> AppsOverlayScreen(
                                installedApps = installedApps,
                                showFloatingBubble = showFloatingBubble,
                                canDrawOverlays = viewModel.canDrawOverlays(),
                                isAccessibilityEnabled = isAccessibilityConnected || viewModel.isAccessibilityEnabled(),
                                activeForegroundPackage = activeForegroundPackage,
                                screenNodes = screenNodes,
                                onToggleFloatingBubble = { viewModel.toggleFloatingBubble(it) },
                                onExpandFloatingHud = { viewModel.setFloatingHudExpanded(true) },
                                onOpenOverlayPermission = { viewModel.openOverlaySettings() },
                                onOpenAccessibilitySettings = { viewModel.openAccessibilitySettings() },
                                onLaunchAppWithOverlay = { app, query ->
                                    viewModel.launchAppWithFloatingAgent(app, query)
                                },
                                onExecuteAccessibilityAction = { cmd, param ->
                                    viewModel.executeAccessibilityAction(cmd, param)
                                }
                            )

                            CockpitTab.AUTOMATIONS -> AutomationsScreen(
                                macros = macros,
                                executionLogs = executionLogs,
                                onRunMacro = { viewModel.runMacro(it) },
                                onCreateMacro = { name, desc, target, type, payload, mins ->
                                    viewModel.createMacro(name, desc, target, type, payload, mins)
                                },
                                onDeleteMacro = { viewModel.deleteMacro(it) }
                            )
                        }
                    }
                }

                // Interactive Draggable Floating Agent Bubble & Mini Prompt/Voice HUD
                FloatingAgentHudOverlay(
                    isVisible = showFloatingBubble,
                    isExpanded = isFloatingHudExpanded,
                    targetAppName = currentTargetApp,
                    liveStepTicker = liveOverlayStep,
                    runState = runState,
                    isListening = isVoiceListening,
                    liveTranscript = liveVoiceTranscript,
                    onToggleExpand = { viewModel.setFloatingHudExpanded(it) },
                    onSubmitPrompt = { prompt ->
                        viewModel.submitPrompt(prompt, source = "OVERLAY_HUD")
                    },
                    onToggleVoice = triggerVoiceWithPermission,
                    onQuickYouTubeAction = { topic ->
                        viewModel.launchAppWithFloatingAgent("YouTube", topic)
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 74.dp, end = 14.dp)
                )
            }
        }
    }
}
