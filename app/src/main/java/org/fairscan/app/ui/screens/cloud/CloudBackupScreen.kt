/*
 * Copyright 2025-2026 The FairScan authors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 */
package org.fairscan.app.ui.screens.cloud

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudBackupScreen(
    onBack: () -> Unit,
    onPickBackupFolder: () -> Unit,
    onPickRestoreFile: () -> Unit,
    isBackingUp: Boolean,
    lastBackupTime: Long?,
    backupFolderName: String?,
    documentCount: Int,
    onBackupNow: () -> Unit,
    onToggleAutoBackup: (Boolean) -> Unit,
    autoBackupEnabled: Boolean,
    snackbarMessage: String?,
    onSnackbarDismissed: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        if (snackbarMessage != null) {
            snackbarHostState.showSnackbar(snackbarMessage)
            onSnackbarDismissed()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cloud & Backup") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(8.dp))

            // Status card
            StatusCard(
                documentCount = documentCount,
                lastBackupTime = lastBackupTime,
                isBackingUp = isBackingUp,
            )

            Spacer(Modifier.height(20.dp))

            // Backup destination section
            Text(
                "Backup destination",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            BackupSection(
                folderName = backupFolderName,
                isBackingUp = isBackingUp,
                autoBackupEnabled = autoBackupEnabled,
                onPickFolder = onPickBackupFolder,
                onBackupNow = onBackupNow,
                onToggleAutoBackup = onToggleAutoBackup,
            )

            Spacer(Modifier.height(20.dp))

            // Cloud providers info
            Text(
                "Supported cloud providers",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            CloudProvidersCard()

            Spacer(Modifier.height(20.dp))

            // Restore section
            Text(
                "Restore",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            RestoreCard(onPickRestoreFile = onPickRestoreFile)

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StatusCard(
    documentCount: Int,
    lastBackupTime: Long?,
    isBackingUp: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ),
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = MaterialTheme.shapes.medium,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (isBackingUp) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 3.dp,
                    )
                } else {
                    Icon(
                        Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
            Column {
                Text(
                    text = if (isBackingUp) "Backing up…" else "$documentCount documents",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (lastBackupTime != null) {
                        "Last backup: ${formatDate(lastBackupTime)}"
                    } else {
                        "No backup yet"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BackupSection(
    folderName: String?,
    isBackingUp: Boolean,
    autoBackupEnabled: Boolean,
    onPickFolder: () -> Unit,
    onBackupNow: () -> Unit,
    onToggleAutoBackup: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(4.dp)) {
            // Folder picker
            ListItem(
                leadingContent = {
                    Icon(
                        Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                headlineContent = { Text("Backup folder") },
                supportingContent = {
                    Text(
                        folderName ?: "No folder selected — tap to choose",
                        color = if (folderName == null) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                trailingContent = {
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                },
                modifier = Modifier
                    .then(
                        if (!isBackingUp) Modifier.clickable(onClick = onPickFolder)
                        else Modifier
                    ),
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Auto-backup toggle
            ListItem(
                leadingContent = {
                    Icon(Icons.Default.Autorenew, contentDescription = null)
                },
                headlineContent = { Text("Automatic backup") },
                supportingContent = { Text("Back up whenever a document is saved") },
                trailingContent = {
                    Switch(
                        checked = autoBackupEnabled,
                        onCheckedChange = onToggleAutoBackup,
                        enabled = folderName != null,
                    )
                },
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Back up now
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Button(
                    onClick = onBackupNow,
                    enabled = folderName != null && !isBackingUp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Back Up Now")
                }
            }
        }
    }
}

@Composable
private fun CloudProvidersCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "When you choose a backup folder, you can pick any cloud storage that's connected to your Android Files app:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            listOf(
                "Google Drive" to Icons.Default.Cloud,
                "OneDrive" to Icons.Default.Cloud,
                "Dropbox" to Icons.Default.Cloud,
                "Any local folder" to Icons.Default.FolderCopy,
            ).forEach { (name, icon) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp),
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(name, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun RestoreCard(onPickRestoreFile: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Restore a document from a previously exported PDF or from your backup folder.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onPickRestoreFile,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Choose File to Restore")
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

// Suppress for clickable extension usage
private fun Modifier.clickable(onClick: () -> Unit): Modifier =
    this.then(androidx.compose.foundation.clickable(onClick = onClick))
