package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.WorkspaceFile
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
fun WorkspaceScreen(
    files: List<WorkspaceFile>,
    selectedFilePath: String,
    editorText: String,
    editorStatus: String,
    onSelectFile: (WorkspaceFile) -> Unit,
    onEditorTextChange: (String) -> Unit,
    onSaveFile: () -> Unit,
    onCreateNewFile: (String, String) -> Unit,
    onDeleteCurrentFile: () -> Unit,
    onImportDeviceFile: () -> Unit,
    onAskAgentToRefactor: (String) -> Unit,
    onRunOrExecuteFile: () -> Unit,
    onShareFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showRefactorDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        // 1. Top Workspace IDE Header & Device File Mounting Controls
        Surface(
            color = TerminalSurface,
            border = BorderStroke(1.dp, GridBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "LOCAL WORKSPACE & DEVICE FILES",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Path: /nexus_workspace/$selectedFilePath • $editorStatus",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberCyan
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Import Any Local Phone File via SAF
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ElevatedPanel,
                            border = BorderStroke(1.dp, CyberCyan),
                            modifier = Modifier
                                .clickable(onClick = onImportDeviceFile)
                                .testTag("import_device_file_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = "Import Phone File",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Mount File",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyberCyan
                                )
                            }
                        }

                        // Create New File
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = TerminalEmerald.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, TerminalEmerald),
                            modifier = Modifier
                                .clickable { showNewFileDialog = true }
                                .testTag("new_workspace_file_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "New File",
                                    tint = TerminalEmerald,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "New",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TerminalEmerald
                                )
                            }
                        }
                    }
                }

                // Horizontal File Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    files.forEach { file ->
                        val isSelected = file.relativePath == selectedFilePath
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) ElevatedPanel else ObsidianBg,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) TerminalEmerald else GridBorder
                            ),
                            modifier = Modifier
                                .clickable { onSelectFile(file) }
                                .testTag("workspace_file_tab_${file.name}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = if (isSelected) TerminalEmerald else TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = file.relativePath,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) TextPrimary else TextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Line-Numbered Monospace Code Editor
        val lineCount = remember(editorText) {
            editorText.lines().size.coerceAtLeast(1)
        }
        val verticalScroll = rememberScrollState()

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(ObsidianBg)
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(verticalScroll)
            ) {
                // Line Numbers Gutter
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier
                        .width(38.dp)
                        .padding(end = 8.dp, top = 4.dp)
                ) {
                    for (line in 1..lineCount) {
                        Text(
                            text = line.toString(),
                            style = TextStyle(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                color = TextMuted
                            )
                        )
                    }
                }

                // Editable Code Canvas
                BasicTextField(
                    value = editorText,
                    onValueChange = onEditorTextChange,
                    textStyle = TextStyle(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = TextPrimary
                    ),
                    cursorBrush = SolidColor(TerminalEmerald),
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 4.dp, end = 8.dp)
                        .testTag("workspace_code_editor")
                )
            }
        }

        // 3. Bottom IDE Action Bar (Save, Run with Agent, AI Refactor, Share, Delete)
        Surface(
            color = TerminalSurface,
            border = BorderStroke(1.dp, GridBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onSaveFile,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TerminalEmerald,
                            contentColor = ObsidianBg
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("save_file_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Save",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = onRunOrExecuteFile,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberCyan,
                            contentColor = ObsidianBg
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("run_file_with_agent_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Run / Execute",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { showRefactorDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeuralViolet,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("ai_edit_file_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoFixHigh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "AI Edit",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(
                        onClick = onShareFile,
                        modifier = Modifier.testTag("share_file_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share or Post File",
                            tint = CyberCyan
                        )
                    }
                    IconButton(
                        onClick = onDeleteCurrentFile,
                        modifier = Modifier.testTag("delete_file_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete File",
                            tint = TextSecondary
                        )
                    }
                }
            }
        }
    }

    if (showNewFileDialog) {
        var newPath by remember { mutableStateOf("scripts/new_agent_task.py") }
        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            containerColor = TerminalSurface,
            title = {
                Text("Create Local Workspace File", color = TextPrimary)
            },
            text = {
                OutlinedTextField(
                    value = newPath,
                    onValueChange = { newPath = it },
                    label = { Text("Relative File Path") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPath.isNotBlank()) {
                            onCreateNewFile(newPath.trim(), "# $newPath\n# Created in NexusCode Local Workspace\n")
                            showNewFileDialog = false
                        }
                    }
                ) {
                    Text("CREATE", color = TerminalEmerald)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFileDialog = false }) {
                    Text("CANCEL", color = TextSecondary)
                }
            }
        )
    }

    if (showRefactorDialog) {
        var instruction by remember { mutableStateOf("Add error handling and logging for YouTube & file automation") }
        AlertDialog(
            onDismissRequest = { showRefactorDialog = false },
            containerColor = TerminalSurface,
            title = {
                Text("AI Code Refactor (`$selectedFilePath`)", color = TextPrimary)
            },
            text = {
                OutlinedTextField(
                    value = instruction,
                    onValueChange = { instruction = it },
                    label = { Text("What should Gemini modify in this file?") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (instruction.isNotBlank()) {
                            onAskAgentToRefactor(instruction)
                            showRefactorDialog = false
                        }
                    }
                ) {
                    Text("RUN AI EDIT", color = TerminalEmerald)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRefactorDialog = false }) {
                    Text("CANCEL", color = TextSecondary)
                }
            }
        )
    }
}
