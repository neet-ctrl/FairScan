/*
 * Copyright 2025-2026 The FairScan authors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 */
package org.fairscan.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import org.fairscan.app.domain.PageViewKey
import org.fairscan.app.domain.Rotation
import org.fairscan.app.ui.state.DocumentUiModel
import org.fairscan.app.ui.state.PageThumbnail
import org.fairscan.imageprocessing.ColorMode

fun dummyNavigation() = Navigation(
    toCameraScreen = {},
    toEditImageScreen = {},
    toDocumentScreen = {},
    toExportScreen = {},
    toLibraryScreen = {},
    toAboutScreen = {},
    toLibrariesScreen = {},
    toSettingsScreen = {},
    toOcrLanguagesScreen = {},
    toCloudBackupScreen = {},
    back = {},
    shouldDisplayBackButton = { false },
)

fun fakeImage(name: String, context: Context): Bitmap {
    return context.assets.open("$name.jpg").use {
        BitmapFactory.decodeStream(it)
    }
}

@Composable
fun fakeDocument(
    imageNames: ImmutableList<String>,
    context: Context,
): DocumentUiModel {
    val pages = imageNames.mapIndexed { index, name ->
        val bitmap = fakeImage(name, context)
        val key = PageViewKey(name, Rotation.R0, ColorMode.COLOR, 0)
        PageThumbnail(key, null)
    }.toImmutableList()
    return DocumentUiModel(pages)
}
