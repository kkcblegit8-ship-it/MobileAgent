package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.agent.AgentRunState
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElevatedPanel
import com.example.ui.theme.GlassHudSurface
import com.example.ui.theme.GridBorder
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeuralViolet
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TerminalEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun FloatingAgentHudOverlay(
    isVisible: Boolean,
    isExpanded: Boolean,
    targetAppName: String,
    liveStepTicker: String,
    runState: AgentRunState,
    isListening: Boolean,
    liveTranscript: String,
    onToggleExpand: (Boolean) -> Unit,
    onSubmitPrompt: (String) -> Unit,
    onToggleVoice: () -> Unit,
    onQuickYouTubeAction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var quickInput by remember { mutableStateOf("") }

    val pulseTransition = rememberInfiniteTransition(label = "orb_pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (runState !is AgentRunState.Idle || isListening) 1.12f else 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_scale"
    )

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
    ) {
        // Collapsed Draggable Cyber-Orb Bubble
        AnimatedVisibility(
            visible = !isExpanded,
            enter = scaleIn(spring(dampingRatio = 0.75f, stiffness = 400f)) + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(58.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF162436),
                                ObsidianBg
                            )
                        )
                    )
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            colors = if (isListening) {
                                listOf(NeuralViolet, CyberCyan)
                            } else {
                                listOf(TerminalEmerald, CyberCyan)
                            }
                        ),
                        shape = CircleShape
                    )
                    .clickable { onToggleExpand(true) }
                    .testTag("floating_agent_bubble")
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = "Open Floating Agent HUD",
                        tint = TerminalEmerald,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "AI",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Expanded Mini Command HUD over active screen / apps
        AnimatedVisibility(
            visible = isExpanded,
            enter = scaleIn(spring(dampingRatio = 0.8f, stiffness = 380f)) + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = GlassHudSurface,
                border = BorderStroke(1.5.dp, CyberCyan.copy(alpha = 0.75f)),
                shadowElevation = 16.dp,
                modifier = Modifier
                    .widthIn(min = 290.dp, max = 340.dp)
                    .testTag("floating_agent_hud_card")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Top HUD bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(if (isListening) NeuralViolet else TerminalEmerald)
                            )
                            Column {
                                Text(
                                    text = "NEXUS // OVERLAY HUD",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TerminalEmerald,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "ACTIVE TARGET: ${targetAppName.uppercase()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyberCyan
                                )
                            }
                        }

                        IconButton(
                            onClick = { onToggleExpand(false) },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("minimize_floating_hud_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Minimize Floating HUD",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Live Step Execution Ticker
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ElevatedPanel)
                            .border(1.dp, GridBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (isListening && liveTranscript.isNotBlank()) {
                                "🎤 \"$liveTranscript\""
                            } else {
                                "⚡ $liveStepTicker"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Quick 1-Tap Cross-App Chips inside Floating HUD
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        HudQuickChip(
                            label = "▶ Open YouTube",
                            onClick = { onQuickYouTubeAction("AI Coding Agents") }
                        )
                        HudQuickChip(
                            label = "🔍 Search in App",
                            onClick = {
                                onSubmitPrompt("Open YouTube and search for latest Kotlin Compose tutorials")
                            }
                        )
                        HudQuickChip(
                            label = "📝 Log Note",
                            onClick = {
                                onSubmitPrompt("Write a quick summary note to notes/overlay_capture.md")
                            }
                        )
                    }

                    // Compact Prompt & Voice Input Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = quickInput,
                            onValueChange = { quickInput = it },
                            placeholder = {
                                Text(
                                    text = "Command or voice...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(color = TextPrimary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (quickInput.isNotBlank()) {
                                        onSubmitPrompt(quickInput)
                                        quickInput = ""
                                    }
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TerminalEmerald,
                                unfocusedBorderColor = GridBorder,
                                focusedContainerColor = ObsidianBg,
                                unfocusedContainerColor = ObsidianBg
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("floating_hud_input")
                        )

                        // Voice Mic Button
                        IconButton(
                            onClick = onToggleVoice,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isListening) NeuralViolet else ElevatedPanel)
                                .border(1.dp, NeuralViolet, RoundedCornerShape(10.dp))
                                .testTag("floating_hud_voice_button")
                        ) {
                            Icon(
                                imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = "Voice Command in Overlay",
                                tint = Color.White
                            )
                        }

                        // Send Prompt Button
                        IconButton(
                            onClick = {
                                if (quickInput.isNotBlank()) {
                                    onSubmitPrompt(quickInput)
                                    quickInput = ""
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(TerminalEmerald)
                                .testTag("floating_hud_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Execute Overlay Command",
                                tint = ObsidianBg
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HudQuickChip(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = ObsidianBg,
        border = BorderStroke(1.dp, GridBorder),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = CyberCyan,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextPrimary
            )
        }
    }
}
