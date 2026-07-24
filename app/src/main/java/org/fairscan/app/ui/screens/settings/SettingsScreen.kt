/*
 * Copyright 2025-2026 The FairScan authors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 */
package org.fairscan.app.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fairscan.app.R
import org.fairscan.app.data.OcrLanguage
import org.fairscan.app.domain.ExportQuality
import org.fairscan.app.ui.Navigation
import org.fairscan.app.ui.components.BackButton
import org.fairscan.app.ui.dummyNavigation
import org.fairscan.app.ui.theme.AccentColor
import org.fairscan.app.ui.theme.AppTheme
import org.fairscan.app.ui.theme.FairScanTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onDefaultColorModeChanged: (DefaultColorMode) -> Unit,
    onChooseDirectoryClick: () -> Unit,
    onResetExportDirClick: () -> Unit,
    onExportFormatChanged: (ExportFormat) -> Unit,
    onExportQualityChanged: (ExportQuality) -> Unit,
    onAppThemeChanged: (AppTheme) -> Unit,
    onAccentColorChanged: (AccentColor) -> Unit,
    navigation: Navigation,
) {
    BackHandler { navigation.back() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = { BackButton(navigation.back) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
            )
        }
    ) { paddingValues ->
        SettingsContent(
            uiState,
            onDefaultColorModeChanged,
            onChooseDirectoryClick,
            onResetExportDirClick,
            onExportFormatChanged,
            onExportQualityChanged,
            onAppThemeChanged,
            onAccentColorChanged,
            navigation,
            modifier = Modifier.padding(paddingValues),
        )
    }
}

@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    onDefaultColorModeChanged: (DefaultColorMode) -> Unit,
    onChooseDirectoryClick: () -> Unit,
    onResetExportDirClick: () -> Unit,
    onExportFormatChanged: (ExportFormat) -> Unit,
    onExportQualityChanged: (ExportQuality) -> Unit,
    onAppThemeChanged: (AppTheme) -> Unit,
    onAccentColorChanged: (AccentColor) -> Unit,
    navigation: Navigation,
    modifier: Modifier = Modifier,
) {
    val displayLocale = Locale.current.platformLocale
    val export = uiState.export
    val (folderLabel, folderLabelColor) = when {
        export.dirUri == null ->
            stringResource(R.string.download_dirname) to MaterialTheme.colorScheme.onSurface
        export.dirName != null ->
            export.dirName to MaterialTheme.colorScheme.onSurface
        else ->
            stringResource(R.string.export_folder_permission_lost) to MaterialTheme.colorScheme.error
    }

    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 12.dp, horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        val context = LocalResources.current

        // ── Appearance ──────────────────────────────────────────────────────
        Text(
            "Appearance",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 4.dp, bottom = 6.dp),
        )
        SettingsGroup {
            SingleChoiceSetting(
                title = "Theme",
                entries = AppTheme.entries,
                onValueChanged = onAppThemeChanged,
                label = { theme ->
                    when (theme) {
                        AppTheme.SYSTEM -> "Follow system"
                        AppTheme.LIGHT -> "Light"
                        AppTheme.DARK -> "Dark"
                    }
                },
                selectedValue = uiState.appTheme,
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            AccentColorSetting(
                selectedColor = uiState.accentColor,
                onAccentColorChanged = onAccentColorChanged,
            )
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        // ── Scan ────────────────────────────────────────────────────────────
        Text(
            stringResource(R.string.settings_section_scan),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        SettingsGroup {
            SingleChoiceSetting(
                title = stringResource(R.string.color_mode_default),
                entries = DefaultColorMode.entries,
                onValueChanged = onDefaultColorModeChanged,
                label = { t -> context.getString(t.labelResource) },
                selectedValue = uiState.defaultColorMode,
            )
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        // ── Export ──────────────────────────────────────────────────────────
        Text(stringResource(R.string.settings_section_export), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        SettingsGroup {
            DirectorySettingItem(
                label = stringResource(R.string.export_directory),
                folderLabel,
                folderLabelColor,
                onClick = onChooseDirectoryClick,
            )
            if (export.dirUri != null) {
                TextButton(
                    onClick = onResetExportDirClick,
                    modifier = Modifier.padding(start = 4.dp),
                ) {
                    Text(stringResource(R.string.reset_to_default))
                }
            }
            Spacer(Modifier.height(8.dp))
            SingleChoiceSetting(
                title = stringResource(R.string.export_quality),
                entries = ExportQuality.entries.reversed(),
                selectedValue = export.quality,
                onValueChanged = onExportQualityChanged,
                label = { t -> context.getString(t.labelResource) },
            )
            SingleChoiceSetting(
                title = stringResource(R.string.export_format),
                entries = ExportFormat.entries,
                selectedValue = export.format,
                onValueChanged = onExportFormatChanged,
                label = { it.name },
            )
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        // ── Cloud & Backup ──────────────────────────────────────────────────
        Text("Library & Backup", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        SettingsGroup {
            ListItem(
                leadingContent = { Icon(Icons.Default.CloudSync, contentDescription = null) },
                headlineContent = { Text("Cloud & Backup") },
                supportingContent = { Text("Back up documents to Google Drive, OneDrive, or any folder") },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                modifier = Modifier.clickable { navigation.toCloudBackupScreen() }
            )
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        // ── OCR ─────────────────────────────────────────────────────────────
        Text(
            stringResource(R.string.settings_section_ocr),
            style = MaterialTheme.typography.titleLarge,
        )
        SettingsGroup {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_ocr_languages)) },
                supportingContent = {
                    Text(uiState.enabledOcrLanguages
                        .map { OcrLanguage(it).displayName(displayLocale) }
                        .sorted()
                        .joinToString(" • ")
                        .ifEmpty { stringResource(R.string.settings_ocr_languages_disabled) }
                    )
                },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                modifier = Modifier.clickable { navigation.toOcrLanguagesScreen() }
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

// ── Accent color picker ──────────────────────────────────────────────────────

private val accentColorSwatches = mapOf(
    AccentColor.MINT to Color(0xFF00B887),
    AccentColor.BLUE to Color(0xFF1A73E8),
    AccentColor.PURPLE to Color(0xFF7C4DFF),
    AccentColor.AMBER to Color(0xFFF4B400),
    AccentColor.ROSE to Color(0xFFE91E63),
    AccentColor.SLATE to Color(0xFF607D8B),
)

@Composable
private fun AccentColorSetting(
    selectedColor: AccentColor,
    onAccentColorChanged: (AccentColor) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text("Accent color", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AccentColor.entries.forEach { color ->
                val swatch = accentColorSwatches[color] ?: Color.Gray
                val isSelected = color == selectedColor
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(swatch)
                        .then(
                            if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
                            else Modifier.border(1.5.dp, Color.Transparent, CircleShape)
                        )
                        .clickable { onAccentColorChanged(color) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

// ── Shared composables ────────────────────────────────────────────────────────

@Composable
fun SettingsGroup(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column { content() }
    }
}

@Composable
fun <T> SingleChoiceSetting(
    title: String,
    entries: List<T>,
    selectedValue: T,
    onValueChanged: (T) -> Unit,
    label: (T) -> String,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(label(selectedValue)) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
        modifier = Modifier.clickable { showDialog = true }.padding(horizontal = 4.dp)
    )

    if (showDialog) {
        AlertDialog(
            shape = MaterialTheme.shapes.large,
            containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = { showDialog = false },
            title = { Text(title) },
            text = {
                Column {
                    entries.forEach { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onValueChanged(entry); showDialog = false }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = selectedValue == entry,
                                onClick = { onValueChanged(entry); showDialog = false },
                            )
                            Text(text = label(entry), modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {},
        )
    }
}

@Composable
fun DirectorySettingItem(
    label: String,
    folderLabel: String,
    folderLabelColor: Color,
    onClick: () -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 0.dp, horizontal = 12.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurfaceVariant),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = folderLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    color = folderLabelColor,
                )
                Icon(Icons.Default.Folder, contentDescription = stringResource(R.string.change_directory))
            }
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview
@Composable
fun SettingsScreenPreviewWithoutDir() {
    SettingsScreenPreview(SettingsUiState(
        installedOcrLanguages = setOf("fra", "eng", "deu"),
        enabledOcrLanguages = setOf("fra", "eng"),
    ))
}

@Preview
@Composable
fun SettingsScreenPreviewWithDir() {
    SettingsScreenPreview(SettingsUiState(
        export = ExportSettingsUiState(dirUri = "content://root/dir"),
    ))
}

@Composable
fun SettingsScreenPreview(uiState: SettingsUiState) {
    FairScanTheme {
        SettingsScreen(
            uiState,
            onDefaultColorModeChanged = {},
            onChooseDirectoryClick = {},
            onResetExportDirClick = {},
            onExportFormatChanged = {},
            onExportQualityChanged = {},
            onAppThemeChanged = {},
            onAccentColorChanged = {},
            navigation = dummyNavigation(),
        )
    }
}
