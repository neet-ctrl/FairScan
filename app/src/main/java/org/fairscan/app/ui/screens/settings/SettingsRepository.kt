/*
 * Copyright 2025-2026 The FairScan authors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 */
package org.fairscan.app.ui.screens.settings

import android.content.Context
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.fairscan.app.R
import org.fairscan.app.domain.ExportQuality
import org.fairscan.app.ui.theme.AccentColor
import org.fairscan.app.ui.theme.AppTheme
import org.fairscan.imageprocessing.ColorMode

class SettingsRepository(
    private val context: Context,
    private val dataStore: DataStore<Preferences>,
) {

    private val DEFAULT_COLOR_MODE = stringPreferencesKey("default_color_mode")
    private val EXPORT_DIR_URI = stringPreferencesKey("export_dir_uri")
    private val EXPORT_FORMAT = stringPreferencesKey("export_format")
    private val EXPORT_QUALITY = stringPreferencesKey("export_quality")
    private val APP_THEME = stringPreferencesKey("app_theme")
    private val ACCENT_COLOR = stringPreferencesKey("accent_color")
    private val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    private val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
    private val BACKUP_DIR_URI = stringPreferencesKey("backup_dir_uri")
    private val LAST_BACKUP_TIME = stringPreferencesKey("last_backup_time")

    val defaultColorMode: Flow<DefaultColorMode> =
        dataStore.data.map { prefs ->
            when (prefs[DEFAULT_COLOR_MODE]) {
                "AUTO" -> DefaultColorMode.AUTO
                "COLOR" -> DefaultColorMode.COLOR
                "GRAYSCALE" -> DefaultColorMode.GRAYSCALE
                else -> DefaultColorMode.AUTO
            }
        }

    val exportDirUri: Flow<String?> =
        dataStore.data.map { prefs -> prefs[EXPORT_DIR_URI] }

    fun resolveExportDirName(uri: String): String? {
        return DocumentFile.fromTreeUri(context, uri.toUri())?.name
    }

    val exportFormat: Flow<ExportFormat> =
        dataStore.data.map { prefs ->
            when (prefs[EXPORT_FORMAT]) {
                "JPEG" -> ExportFormat.JPEG
                "PDF", null -> ExportFormat.PDF
                else -> ExportFormat.PDF
            }
        }

    val exportQuality: Flow<ExportQuality> =
        dataStore.data.map { prefs ->
            when (prefs[EXPORT_QUALITY]) {
                "LOW" -> ExportQuality.LOW
                "HIGH" -> ExportQuality.HIGH
                "BALANCED", null -> ExportQuality.BALANCED
                else -> ExportQuality.BALANCED
            }
        }

    val appTheme: Flow<AppTheme> =
        dataStore.data.map { prefs ->
            when (prefs[APP_THEME]) {
                "LIGHT" -> AppTheme.LIGHT
                "DARK" -> AppTheme.DARK
                else -> AppTheme.SYSTEM
            }
        }

    val accentColor: Flow<AccentColor> =
        dataStore.data.map { prefs ->
            when (prefs[ACCENT_COLOR]) {
                "BLUE" -> AccentColor.BLUE
                "PURPLE" -> AccentColor.PURPLE
                "AMBER" -> AccentColor.AMBER
                "ROSE" -> AccentColor.ROSE
                "SLATE" -> AccentColor.SLATE
                else -> AccentColor.MINT
            }
        }

    val onboardingDone: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[ONBOARDING_DONE] ?: false }

    val autoBackupEnabled: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[AUTO_BACKUP_ENABLED] ?: false }

    val backupDirUri: Flow<String?> =
        dataStore.data.map { prefs -> prefs[BACKUP_DIR_URI] }

    val lastBackupTime: Flow<Long?> =
        dataStore.data.map { prefs -> prefs[LAST_BACKUP_TIME]?.toLongOrNull() }

    // --- Setters ---

    suspend fun setDefaultColorMode(mode: DefaultColorMode) {
        dataStore.edit { prefs -> prefs[DEFAULT_COLOR_MODE] = mode.name }
    }

    suspend fun setExportDirUri(uri: String?) {
        dataStore.edit { prefs ->
            if (uri == null) prefs.remove(EXPORT_DIR_URI)
            else prefs[EXPORT_DIR_URI] = uri
        }
    }

    suspend fun setExportFormat(format: ExportFormat) {
        dataStore.edit { prefs -> prefs[EXPORT_FORMAT] = format.name }
    }

    suspend fun setExportQuality(quality: ExportQuality) {
        dataStore.edit { prefs -> prefs[EXPORT_QUALITY] = quality.name }
    }

    suspend fun setAppTheme(theme: AppTheme) {
        dataStore.edit { prefs -> prefs[APP_THEME] = theme.name }
    }

    suspend fun setAccentColor(color: AccentColor) {
        dataStore.edit { prefs -> prefs[ACCENT_COLOR] = color.name }
    }

    suspend fun setOnboardingDone(done: Boolean) {
        dataStore.edit { prefs -> prefs[ONBOARDING_DONE] = done }
    }

    suspend fun setAutoBackupEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[AUTO_BACKUP_ENABLED] = enabled }
    }

    suspend fun setBackupDirUri(uri: String?) {
        dataStore.edit { prefs ->
            if (uri == null) prefs.remove(BACKUP_DIR_URI)
            else prefs[BACKUP_DIR_URI] = uri
        }
    }

    suspend fun setLastBackupTime(time: Long) {
        dataStore.edit { prefs -> prefs[LAST_BACKUP_TIME] = time.toString() }
    }
}

enum class DefaultColorMode(val colorMode: ColorMode?, val labelResource: Int) {
    AUTO(null, R.string.color_mode_auto),
    COLOR(ColorMode.COLOR, R.string.color_mode_color),
    GRAYSCALE(ColorMode.GRAYSCALE, R.string.color_mode_grayscale),
}

enum class ExportFormat(val mimeType: String) {
    PDF("application/pdf"),
    JPEG("image/jpeg"),
}
