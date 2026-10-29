package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.AutomationMacroEntity
import com.example.data.ExecutionLogEntity
import com.example.data.ToolActionType
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElevatedPanel
import com.example.ui.theme.GridBorder
import com.example.ui.theme.NeuralViolet
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TerminalEmerald
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AutomationsScreen(
    macros: List<AutomationMacroEntity>,
    executionLogs: List<ExecutionLogEntity>,
    onRunMacro: (AutomationMacroEntity) -> Unit,
    onCreateMacro: (String, String, String, ToolActionType, String, Int) -> Unit,
    onDeleteMacro: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .testTag("automations_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header & Create Macro Action
        item {
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
                        imageVector = Icons.Default.AutoMode,
                        contentDescription = null,
                        tint = TerminalEmerald
                    )
                    Column {
                        Text(
                            text = "AUTOMATION MACROS & TASKS",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "One-tap cross-app pipelines & scheduled agent jobs",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TerminalEmerald,
                        contentColor = ObsidianBg
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("new_macro_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "New Macro",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 2. Macro Cards
        items(macros, key = { it.id }) { macro ->
            MacroCard(
                macro = macro,
                onRun = { onRunMacro(macro) },
                onDelete = { onDeleteMacro(macro.id) }
            )
        }

        // 3. Live Step-by-Step Agent Execution Audit Log
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = CyberCyan
                )
                Text(
                    text = "LIVE AGENT EXECUTION LOG (${executionLogs.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        items(executionLogs, key = { it.id }) { log ->
            ExecutionLogRow(log = log)
        }
    }

    if (showCreateDialog) {
        var name by remember { mutableStateOf("YouTube Daily Tech Digest") }
        var desc by remember { mutableStateOf("Opens YouTube with Floating Bubble and searches latest tech news") }
        var target by remember { mutableStateOf("com.google.android.youtube") }
        var payload by remember { mutableStateOf("Daily AI & Android Dev News") }
        var intervalStr by remember { mutableStateOf("60") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = TerminalSurface,
            title = {
                Text("Create Automation Macro", color = TextPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Macro Name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = target,
                        onValueChange = { target = it },
                        label = { Text("Target App or File") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = payload,
                        onValueChange = { payload = it },
                        label = { Text("Search Query / Payload") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = intervalStr,
                        onValueChange = { intervalStr = it },
                        label = { Text("Schedule Interval (minutes, 0 = manual)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (name.isNotBlank()) {
                            val mins = intervalStr.toIntOrNull() ?: 0
                            val type = if (target.contains("youtube", ignoreCase = true)) {
                                ToolActionType.YOUTUBE_ACTION
                            } else {
                                ToolActionType.LAUNCH_APP
                            }
                            onCreateMacro(name, desc, target, type, payload, mins)
                            showCreateDialog = false
                        }
                    }
                ) {
                    Text("SAVE MACRO", color = TerminalEmerald)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("CANCEL", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun MacroCard(
    macro: AutomationMacroEntity,
    onRun: () -> Unit,
    onDelete: () -> Unit
) {
    val steps = remember(macro.stepsJson) { macro.parsedSteps() }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = TerminalSurface,
        border = BorderStroke(1.dp, GridBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = macro.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ElevatedPanel
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = if (macro.intervalMinutes > 0) NeuralViolet else CyberCyan,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = if (macro.intervalMinutes > 0) {
                                        "EVERY ${macro.intervalMinutes}M"
                                    } else {
                                        macro.triggerType
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (macro.intervalMinutes > 0) NeuralViolet else CyberCyan
                                )
                            }
                        }
                    }
                    Text(
                        text = macro.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Macro",
                        tint = TextMuted
                    )
                }
            }

            // Steps Preview
            if (steps.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ObsidianBg,
                    border = BorderStroke(1.dp, GridBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        steps.forEach { step ->
                            Text(
                                text = "${step.stepOrder}. [${step.actionType.name}] ${step.label}",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberCyan
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Runs completed: ${macro.runCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )

                Button(
                    onClick = onRun,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TerminalEmerald,
                        contentColor = ObsidianBg
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("run_macro_${macro.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "EXECUTE PIPELINE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ExecutionLogRow(log: ExecutionLogEntity) {
    val badgeColor = when (log.status) {
        "ERROR" -> CrimsonAlert
        "RUNNING" -> CyberCyan
        else -> TerminalEmerald
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = TerminalSurface,
        border = BorderStroke(1.dp, GridBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.stepTitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = badgeColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = log.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Text(
                text = log.status,
                style = MaterialTheme.typography.labelSmall,
                color = badgeColor
            )
        }
    }
}
