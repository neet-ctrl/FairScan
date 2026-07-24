/*
 * Copyright 2025-2026 The FairScan authors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 */
package org.fairscan.app.ui.screens.library

import org.fairscan.app.data.LibraryDocumentInfo

enum class LibrarySortOrder { DATE_DESC, DATE_ASC, NAME_ASC, NAME_DESC, PAGE_COUNT_DESC }

data class LibraryUiState(
    val documents: List<LibraryDocumentInfo> = emptyList(),
    val covers: Map<String, ByteArray> = emptyMap(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val sortOrder: LibrarySortOrder = LibrarySortOrder.DATE_DESC,
    val selectedIds: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val hasUnfinishedSession: Boolean = false,
    val unfinishedPageCount: Int = 0,
    val snackbarMessage: String? = null,
) {
    val filteredDocuments: List<LibraryDocumentInfo>
        get() {
            val q = searchQuery.trim().lowercase()
            val filtered = if (q.isEmpty()) documents
            else documents.filter { it.name.lowercase().contains(q) }
            return when (sortOrder) {
                LibrarySortOrder.DATE_DESC -> filtered.sortedByDescending { it.modifiedAt }
                LibrarySortOrder.DATE_ASC -> filtered.sortedBy { it.modifiedAt }
                LibrarySortOrder.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
                LibrarySortOrder.NAME_DESC -> filtered.sortedByDescending { it.name.lowercase() }
                LibrarySortOrder.PAGE_COUNT_DESC -> filtered.sortedByDescending { it.pageCount }
            }
        }
}
