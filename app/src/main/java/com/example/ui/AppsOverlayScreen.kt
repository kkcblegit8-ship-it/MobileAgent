package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.InstalledAppInfo
import com.example.ui.theme.AmberWarn
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElevatedPanel
import com.example.ui.theme.GridBorder
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeuralViolet
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TerminalEmerald
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AppsOverlayScreen(
    installedApps: List<InstalledAppInfo>,
    showFloatingBubble: Boolean,
    canDrawOverlays: Boolean,
    isAccessibilityEnabled: Boolean,
    activeForegroundPackage: String,
    screenNodes: List<String>,
    onToggleFloatingBubble: (Boolean) -> Unit,
    onExpandFloatingHud: () -> Unit,
    onOpenOverlayPermission: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onLaunchAppWithOverlay: (String, String) -> Unit,
    onExecuteAccessibilityAction: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var youtubeQuery by remember { mutableStateOf("AI Agents & Mobile Automation") }
    var uiActionTarget by remember { mutableStateOf("Search") }
    var appFilter by remember { mutableStateOf("") }

    val filteredApps = remember(installedApps, appFilter) {
        if (appFilter.isBlank()) installedApps
        else installedApps.filter {
            it.appName.contains(appFilter, ignoreCase = true) ||
                it.packageName.contains(appFilter, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .testTag("apps_overlay_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Floating Overlay Bubble & Display Over Other Apps Control Card
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = TerminalSurface,
                border = BorderStroke(1.dp, TerminalEmerald),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = TerminalEmerald
                            )
                            Column {
                                Text(
                                    text = "FLOATING AGENT OVERLAY ENGINE",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Displays draggable AI icon on top of YouTube & other apps",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        Switch(
                            checked = showFloatingBubble,
                            onCheckedChange = onToggleFloatingBubble,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ObsidianBg,
                                checkedTrackColor = TerminalEmerald
                            ),
                            modifier = Modifier.testTag("overlay_master_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onToggleFloatingBubble(true)
                                onExpandFloatingHud()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan,
                                contentColor = ObsidianBg
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_mini_hud_button")
                        ) {
                            Text(
                                text = "Expand Floating HUD",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = onOpenOverlayPermission,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElevatedPanel,
                                contentColor = if (canDrawOverlays) TerminalEmerald else AmberWarn
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("manage_overlay_permission_button")
                        ) {
                            Text(
                                text = if (canDrawOverlays) "Overlay Perm: OK" else "Grant System Overlay",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 2. Featured YouTube + Floating Agent Overlay Launcher
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = TerminalSurface,
                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircleFilled,
                            contentDescription = null,
                            tint = CyberCyan
                        )
                        Text(
                            text = "YOUTUBE + FLOATING OVERLAY LAUNCHER",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Opens YouTube directly to your topic while pinning the NexusCode floating icon over the screen so you can voice-chat or type prompts without leaving YouTube.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    OutlinedTextField(
                        value = youtubeQuery,
                        onValueChange = { youtubeQuery = it },
                        label = { Text("YouTube Search / Action Topic") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = GridBorder,
                            focusedContainerColor = ObsidianBg,
                            unfocusedContainerColor = ObsidianBg
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("youtube_query_input")
                    )

                    Button(
                        onClick = { onLaunchAppWithOverlay("YouTube", youtubeQuery) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TerminalEmerald,
                            contentColor = ObsidianBg
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("launch_youtube_with_query_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "OPEN YOUTUBE WITH FLOATING AGENT ICON",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3. Cross-App Accessibility Service & Screen Node Inspector
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = TerminalSurface,
                border = BorderStroke(1.dp, NeuralViolet.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessibilityNew,
                                contentDescription = null,
                                tint = NeuralViolet
                            )
                            Column {
                                Text(
                                    text = "ACCESSIBILITY UI AUTOMATION",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Active Window: $activeForegroundPackage",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeuralViolet
                                )
                            }
                        }

                        Button(
                            onClick = onOpenAccessibilitySettings,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElevatedPanel,
                                contentColor = if (isAccessibilityEnabled) TerminalEmerald else AmberWarn
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("configure_accessibility_button")
                        ) {
                            Text(
                                text = if (isAccessibilityEnabled) "Service Active" else "Enable Service",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    OutlinedTextField(
                        value = uiActionTarget,
                        onValueChange = { uiActionTarget = it },
                        label = { Text("Target Button Text or Input String") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeuralViolet,
                            unfocusedBorderColor = GridBorder,
                            focusedContainerColor = ObsidianBg,
                            unfocusedContainerColor = ObsidianBg
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        UiAutomationChip("Click Element") {
                            onExecuteAccessibilityAction("CLICK", uiActionTarget)
                        }
                        UiAutomationChip("Type Text") {
                            onExecuteAccessibilityAction("TYPE", uiActionTarget)
                        }
                        UiAutomationChip("Scroll Down") {
                            onExecuteAccessibilityAction("SCROLL", "")
                        }
                        UiAutomationChip("Inspect Nodes") {
                            onExecuteAccessibilityAction("INSPECT", "")
                        }
                        UiAutomationChip("Navigate Back") {
                            onExecuteAccessibilityAction("BACK", "")
                        }
                    }

                    if (screenNodes.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ObsidianBg,
                            border = BorderStroke(1.dp, GridBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "LIVE SCREEN NODES (${screenNodes.size}):",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyberCyan
                                )
                                Text(
                                    text = screenNodes.take(8).joinToString(" • "),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Installed Device Apps Catalog Header & Search
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = null,
                        tint = TerminalEmerald
                    )
                    Text(
                        text = "INSTALLED DEVICE APPS (${filteredApps.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = appFilter,
                    onValueChange = { appFilter = it },
                    placeholder = { Text("Filter apps by name or package...", color = TextMuted) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = TextSecondary
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TerminalEmerald,
                        unfocusedBorderColor = GridBorder,
                        focusedContainerColor = TerminalSurface,
                        unfocusedContainerColor = TerminalSurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("filter_apps_input")
                )
            }
        }

        // 5. App Cards
        items(filteredApps, key = { it.packageName }) { app ->
            InstalledAppRowCard(
                app = app,
                onLaunchWithOverlay = {
                    onLaunchAppWithOverlay(app.appName, "")
                }
            )
        }
    }
}

@Composable
private fun UiAutomationChip(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = ElevatedPanel,
        border = BorderStroke(1.dp, NeuralViolet),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.Default.TouchApp,
                contentDescription = null,
                tint = NeuralViolet,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun InstalledAppRowCard(
    app: InstalledAppInfo,
    onLaunchWithOverlay: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = TerminalSurface,
        border = BorderStroke(1.dp, GridBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = ElevatedPanel
                    ) {
                        Text(
                            text = app.categoryTag,
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberCyan,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }

            Button(
                onClick = onLaunchWithOverlay,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElevatedPanel,
                    contentColor = TerminalEmerald
                ),
                border = BorderStroke(1.dp, TerminalEmerald),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.testTag("launch_app_${app.packageName}")
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Open + Overlay",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
