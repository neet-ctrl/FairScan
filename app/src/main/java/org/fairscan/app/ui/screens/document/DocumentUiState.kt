/*
 * Copyright 2025-2026 The FairScan authors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 */
package org.fairscan.app.ui.screens.document

import android.graphics.Bitmap
import org.fairscan.app.domain.PageViewKey
import org.fairscan.app.ui.state.DocumentUiModel
import org.fairscan.imageprocessing.ColorMode

data class DocumentUiState(
    val currentPageIndex: Int,
    val currentPage: CurrentPageUiState?,
    val document: DocumentUiModel,
    val selectedPageIds: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val editingLibraryDocumentId: String? = null,
)

data class CurrentPageUiState(
    val key: PageViewKey,
    val bitmap: Bitmap?,
    val colorMode: ColorMode?,
    val canBeCropped: Boolean = false,
    val isLoading: Boolean = false,
)
