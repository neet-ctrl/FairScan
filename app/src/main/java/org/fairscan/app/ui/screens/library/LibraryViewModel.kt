/*
 * Copyright 2025-2026 The FairScan authors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 */
package org.fairscan.app.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.fairscan.app.AppContainer
import org.fairscan.app.data.LibraryDocumentInfo
import org.fairscan.app.data.LibraryRepository

class LibraryViewModel(container: AppContainer) : ViewModel() {

    val libraryRepository: LibraryRepository = container.libraryRepository

    private val _uiState = MutableStateFlow(LibraryUiState(isLoading = true))
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        // Observe library changes
        viewModelScope.launch {
            libraryRepository.documents.collect { docs ->
                _uiState.update { state ->
                    state.copy(
                        documents = docs,
                        isLoading = false,
                    )
                }
                // Load covers in background
                loadCovers(docs)
            }
        }
    }

    private fun loadCovers(docs: List<LibraryDocumentInfo>) {
        viewModelScope.launch(Dispatchers.IO) {
            val newCovers = docs.associate { doc ->
                val bytes = libraryRepository.getCoverBytes(doc.id)
                doc.id to bytes
            }.filterValues { it != null }.mapValues { it.value!! }
            _uiState.update { it.copy(covers = newCovers) }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setSortOrder(order: LibrarySortOrder) {
        _uiState.update { it.copy(sortOrder = order) }
    }

    fun toggleSelection(id: String) {
        _uiState.update { state ->
            val newSelected = if (id in state.selectedIds) {
                state.selectedIds - id
            } else {
                state.selectedIds + id
            }
            state.copy(
                selectedIds = newSelected,
                isSelectionMode = newSelected.isNotEmpty(),
            )
        }
    }

    fun selectAll() {
        _uiState.update { state ->
            val allIds = state.filteredDocuments.map { it.id }.toSet()
            state.copy(selectedIds = allIds, isSelectionMode = true)
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedIds = emptySet(), isSelectionMode = false) }
    }

    fun enterSelectionMode(id: String) {
        _uiState.update { it.copy(selectedIds = setOf(id), isSelectionMode = true) }
    }

    fun deleteSelected() {
        viewModelScope.launch {
            val ids = _uiState.value.selectedIds.toSet()
            libraryRepository.deleteDocuments(ids)
            _uiState.update { state ->
                state.copy(
                    selectedIds = emptySet(),
                    isSelectionMode = false,
                    snackbarMessage = "${ids.size} document${if (ids.size == 1) "" else "s"} deleted",
                )
            }
        }
    }

    fun deleteDocument(id: String) {
        viewModelScope.launch {
            libraryRepository.deleteDocument(id)
            _uiState.update { it.copy(snackbarMessage = "Document deleted") }
        }
    }

    fun renameDocument(id: String, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            libraryRepository.renameDocument(id, newName.trim())
        }
    }

    fun duplicateDocument(id: String) {
        viewModelScope.launch {
            val original = libraryRepository.getDocument(id) ?: return@launch
            val newName = "${original.name} (copy)"
            libraryRepository.duplicateDocument(id, newName)
            _uiState.update { it.copy(snackbarMessage = "Document duplicated") }
        }
    }

    fun dismissSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun setUnfinishedSession(hasSession: Boolean, pageCount: Int) {
        _uiState.update { it.copy(hasUnfinishedSession = hasSession, unfinishedPageCount = pageCount) }
    }
}
