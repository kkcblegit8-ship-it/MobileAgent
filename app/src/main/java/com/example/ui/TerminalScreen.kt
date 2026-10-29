package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.agent.AgentRunState
import com.example.data.AgentMessageEntity
import com.example.data.AgentToolCall
import com.example.ui.theme.AmberWarn
import com.example.ui.theme.CrimsonAlert
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
fun TerminalScreen(
    messages: List<AgentMessageEntity>,
    runState: AgentRunState,
    showFloatingBubble: Boolean,
    canDrawOverlays: Boolean,
    isAccessibilityEnabled: Boolean,
    isListening: Boolean,
    liveTranscript: String,
    voiceStatus: String,
    isTtsEnabled: Boolean,
    onSubmitPrompt: (String) -> Unit,
    onToggleFloatingBubble: (Boolean) -> Unit,
    onExpandFloatingHud: () -> Unit,
    onOpenOverlayPermission: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onLaunchYouTubeWithOverlay: () -> Unit,
    onToggleVoice: () -> Unit,
    onToggleTts: () -> Unit,
    onRequestMicPermissionAndListen: () -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inputPrompt by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, runState) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        // 1. Cyber-Terminal System Readiness & Permissions Cockpit Header
        SystemReadinessHeader(
            showFloatingBubble = showFloatingBubble,
            canDrawOverlays = canDrawOverlays,
            isAccessibilityEnabled = isAccessibilityEnabled,
            isTtsEnabled = isTtsEnabled,
            onToggleFloatingBubble = onToggleFloatingBubble,
            onExpandFloatingHud = onExpandFloatingHud,
            onOpenOverlayPermission = onOpenOverlayPermission,
            onOpenAccessibilitySettings = onOpenAccessibilitySettings,
            onLaunchYouTubeWithOverlay = onLaunchYouTubeWithOverlay,
            onToggleTts = onToggleTts,
            onClearHistory = onClearHistory
        )

        // 2. Agent Conversation & Tool Execution Stream
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("agent_message_list")
        ) {
            items(messages, key = { it.id }) { msg ->
                AgentMessageCard(
                    message = msg,
                    onReRunTool = { tool ->
                        onSubmitPrompt("Execute ${tool.actionType.name} on ${tool.target}: ${tool.payload}")
                    }
                )
            }

            // Active Thinking / Tool Execution Banner
            if (runState !is AgentRunState.Idle) {
                item {
                    LiveRunStateBanner(runState = runState)
                }
            }
        }

        // 3. Quick Agent Command Chips
        val quickPrompts = listOf(
            "▶ Open YouTube & search 'AI Coding Agents'" to "Open YouTube and search for AI Coding Agents with the floating overlay bubble active",
            "🐍 Create Python YouTube Downloader Script" to "Write a clean Python script in scripts/yt_sync.py that organizes local media and logs metadata",
            "⚡ Automate Screen Click & Search" to "Use UI_AUTOMATION_STEP to inspect the current screen and click the Search button",
            "📤 Post Workspace Note to Apps" to "Share a status update from my local workspace using SYSTEM_SHARE"
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickPrompts.forEachIndexed { idx, (label, fullPrompt) ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = TerminalSurface,
                    border = BorderStroke(1.dp, GridBorder),
                    modifier = Modifier
                        .clickable { onSubmitPrompt(fullPrompt) }
                        .testTag("quick_prompt_chip_$idx")
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = CyberCyan,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // 4. Voice Recognition Status Strip (when active)
        AnimatedVisibility(visible = isListening || liveTranscript.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(NeuralViolet.copy(alpha = 0.16f))
                    .border(1.dp, NeuralViolet, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(NeuralViolet)
                    )
                    Text(
                        text = if (liveTranscript.isNotBlank()) "🎤 \"$liveTranscript\"" else voiceStatus,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary
                    )
                }
                if (isListening) {
                    Text(
                        text = "TAP MIC TO SEND",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 5. Bottom Prompt & Voice Command Bar
        Surface(
            color = TerminalSurface,
            border = BorderStroke(1.dp, GridBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputPrompt,
                    onValueChange = { inputPrompt = it },
                    placeholder = {
                        Text(
                            text = "Ask NexusCode to open apps, code, or automate...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        color = TextPrimary
                    ),
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputPrompt.isNotBlank()) {
                                onSubmitPrompt(inputPrompt)
                                inputPrompt = ""
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TerminalEmerald,
                        unfocusedBorderColor = GridBorder,
                        focusedContainerColor = ObsidianBg,
                        unfocusedContainerColor = ObsidianBg
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("terminal_prompt_input")
                )

                // Voice Input Button
                IconButton(
                    onClick = onRequestMicPermissionAndListen,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isListening) NeuralViolet else ElevatedPanel)
                        .border(
                            1.dp,
                            if (isListening) Color.White else NeuralViolet,
                            RoundedCornerShape(12.dp)
                        )
                        .testTag("terminal_voice_button")
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "Voice Command Input",
                        tint = Color.White
                    )
                }

                // Execute Command Button
                IconButton(
                    onClick = {
                        if (inputPrompt.isNotBlank()) {
                            onSubmitPrompt(inputPrompt)
                            inputPrompt = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(TerminalEmerald)
                        .testTag("terminal_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Command to Agent",
                        tint = ObsidianBg
                    )
                }
            }
        }
    }
}

@Composable
private fun SystemReadinessHeader(
    showFloatingBubble: Boolean,
    canDrawOverlays: Boolean,
    isAccessibilityEnabled: Boolean,
    isTtsEnabled: Boolean,
    onToggleFloatingBubble: (Boolean) -> Unit,
    onExpandFloatingHud: () -> Unit,
    onOpenOverlayPermission: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onLaunchYouTubeWithOverlay: () -> Unit,
    onToggleTts: () -> Unit,
    onClearHistory: () -> Unit
) {
    Surface(
        color = TerminalSurface,
        border = BorderStroke(1.dp, GridBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF0D1520),
                            TerminalSurface,
                            Color(0xFF131124)
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(TerminalEmerald)
                    )
                    Column {
                        Text(
                            text = "NEXUSCODE // MOBILE AGENT",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "GEMINI 3.8 FLASH • OVERLAY & LOCAL FS ACCESS",
                            style = MaterialTheme.typography.labelSmall,
                            color = TerminalEmerald
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onToggleTts,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("toggle_tts_button")
                    ) {
                        Icon(
                            imageVector = if (isTtsEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Toggle Spoken Agent Feedback",
                            tint = if (isTtsEnabled) CyberCyan else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onClearHistory,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("clear_terminal_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear Terminal History",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Interactive Capability & Permission Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. YouTube + Overlay Quick Launch
                Button(
                    onClick = onLaunchYouTubeWithOverlay,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TerminalEmerald,
                        contentColor = ObsidianBg
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("launch_youtube_overlay_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircleFilled,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Open YouTube + Overlay Icon",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 2. Floating Bubble Toggle / Expand
                PermissionPill(
                    label = if (showFloatingBubble) "Floating Bubble: ON (Tap HUD)" else "Floating Bubble: OFF",
                    isReady = showFloatingBubble,
                    onClick = {
                        if (!showFloatingBubble) {
                            onToggleFloatingBubble(true)
                        } else {
                            onExpandFloatingHud()
                        }
                    },
                    testTag = "toggle_floating_bubble_pill"
                )

                // 3. System Display Over Other Apps Permission
                PermissionPill(
                    label = if (canDrawOverlays) "System Overlay: Granted" else "Grant Overlay Perm",
                    isReady = canDrawOverlays,
                    onClick = onOpenOverlayPermission,
                    testTag = "system_overlay_permission_pill"
                )

                // 4. Accessibility Cross-App Automation Service
                PermissionPill(
                    label = if (isAccessibilityEnabled) "Accessibility: Active" else "Enable Accessibility Agent",
                    isReady = isAccessibilityEnabled,
                    onClick = onOpenAccessibilitySettings,
                    testTag = "accessibility_permission_pill"
                )
            }
        }
    }
}

@Composable
private fun PermissionPill(
    label: String,
    isReady: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isReady) TerminalEmerald.copy(alpha = 0.14f) else ElevatedPanel,
        border = BorderStroke(1.dp, if (isReady) TerminalEmerald else AmberWarn),
        modifier = Modifier
            .height(36.dp)
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = if (isReady) Icons.Default.CheckCircle else Icons.Default.Security,
                contentDescription = null,
                tint = if (isReady) TerminalEmerald else AmberWarn,
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
private fun AgentMessageCard(
    message: AgentMessageEntity,
    onReRunTool: (AgentToolCall) -> Unit
) {
    val isUser = message.role == "user"
    val toolCalls = remember(message.toolCallsJson) { message.parsedToolCalls() }
    var showThought by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isUser) 14.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 14.dp
            ),
            color = if (isUser) Color(0xFF17253B) else TerminalSurface,
            border = BorderStroke(
                1.dp,
                if (isUser) CyberCyan.copy(alpha = 0.5f) else GridBorder
            ),
            modifier = Modifier.fillMaxWidth(if (isUser) 0.88f else 0.96f)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header metadata
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isUser) Icons.Default.Layers else Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (isUser) CyberCyan else TerminalEmerald,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (isUser) "OPERATOR // ${message.source}" else "NEXUSCODE AGENT",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isUser) CyberCyan else TerminalEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (!isUser && message.latencyMs > 0) {
                        Text(
                            text = "${message.latencyMs}ms",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }

                // Collapsible AI Reasoning Trace
                if (!isUser && message.thoughtProcess.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ObsidianBg,
                        border = BorderStroke(1.dp, GridBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showThought = !showThought }
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = null,
                                    tint = NeuralViolet,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (showThought) "Hide Agent Reasoning ▲" else "Inspect Agent Reasoning ▼",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeuralViolet
                                )
                            }
                            AnimatedVisibility(visible = showThought) {
                                Text(
                                    text = message.thoughtProcess,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Main Message Body
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )

                // Executed Tool Call Cards
                if (toolCalls.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        toolCalls.forEach { tool ->
                            ToolCallExecutionCard(
                                tool = tool,
                                onReRun = { onReRunTool(tool) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolCallExecutionCard(
    tool: AgentToolCall,
    onReRun: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = ObsidianBg,
        border = BorderStroke(1.dp, TerminalEmerald.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = TerminalEmerald,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = tool.actionType.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = TerminalEmerald,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = ElevatedPanel,
                    modifier = Modifier.clickable(onClick = onReRun)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "Re-run tool",
                            tint = CyberCyan,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "RE-RUN",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberCyan
                        )
                    }
                }
            }

            Text(
                text = "Target: ${tool.target}",
                style = MaterialTheme.typography.labelSmall,
                color = CyberCyan
            )
            if (tool.description.isNotBlank()) {
                Text(
                    text = tool.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary
                )
            }
            if (tool.resultSummary.isNotBlank()) {
                Text(
                    text = "✓ ${tool.resultSummary}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TerminalEmerald
                )
            }
        }
    }
}

@Composable
private fun LiveRunStateBanner(runState: AgentRunState) {
    val isError = runState is AgentRunState.Error
    val borderColor = if (isError) CrimsonAlert else CyberCyan
    val text = when (runState) {
        is AgentRunState.Thinking -> runState.phaseText
        is AgentRunState.ExecutingTool -> "Executing ${runState.toolName}: ${runState.detail}"
        is AgentRunState.Error -> runState.message
        AgentRunState.Idle -> ""
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ElevatedPanel,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!isError) {
                CircularProgressIndicator(
                    color = TerminalEmerald,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = if (isError) CrimsonAlert else TextPrimary
            )
        }
    }
}
