/*
 * Copyright 2025-2026 The FairScan authors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 */
package org.fairscan.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.fairscan.app.data.ImageRepository
import org.fairscan.app.data.LibraryRepository
import org.fairscan.app.data.Logger
import org.fairscan.app.data.PageSnapshot
import org.fairscan.app.domain.CapturedPage
import org.fairscan.app.domain.Rotation
import org.fairscan.app.domain.ScanPage
import org.fairscan.app.ui.NavigationState
import org.fairscan.app.ui.Screen
import org.fairscan.app.ui.screens.crop.CropInitState
import org.fairscan.app.ui.screens.document.CurrentPageUiState
import org.fairscan.app.ui.screens.document.DocumentUiState
import org.fairscan.app.ui.screens.settings.SettingsRepository
import org.fairscan.app.ui.state.DocumentUiModel
import org.fairscan.app.ui.state.PageThumbnail
import org.fairscan.imageprocessing.ColorMode
import org.fairscan.imageprocessing.ImageSize
import org.fairscan.imageprocessing.Quad
import kotlin.math.min

private const val MAX_UNDO_STACK = 20

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    val imageRepository: ImageRepository,
    logger: Logger,
    val libraryRepository: LibraryRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _navigationState = MutableStateFlow<NavigationState?>(null)
    val currentScreen: StateFlow<Screen?> = _navigationState.map { it?.current }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _pages = MutableStateFlow<List<ScanPage>>(emptyList())

    // Undo / Redo stacks: each entry is an ordered list of (pageId, manualRotationDegrees)
    private val undoStack = ArrayDeque<List<PageSnapshot>>()
    private val redoStack = ArrayDeque<List<PageSnapshot>>()
    private val _canUndo = MutableStateFlow(false)
    private val _canRedo = MutableStateFlow(false)

    // Multi-select
    private val _selectedPageIds = MutableStateFlow<Set<String>>(emptySet())
    private val _isSelectionMode = MutableStateFlow(false)

    // Library editing
    private val _currentLibraryDocumentId = MutableStateFlow<String?>(null)
    val currentLibraryDocumentId: StateFlow<String?> = _currentLibraryDocumentId

    init {
        viewModelScope.launch {
            val pages = imageRepository.pages()
            _pages.value = pages

            val onboardingDone = settingsRepository.onboardingDone.first()
            _navigationState.value = when {
                !onboardingDone -> NavigationState.initial(Screen.Main.Onboarding)
                pages.isNotEmpty() -> NavigationState.initial(Screen.Main.Library)
                    .navigateTo(Screen.Main.Library) // Library is root; unfinished scan visible as card
                else -> NavigationState.initial(Screen.Main.Library)
            }
        }
    }

    val documentUiModel: StateFlow<DocumentUiModel> =
        _pages.map { pages ->
            pages.map {
                val jpeg = imageRepository.getThumbnail(it.key())
                PageThumbnail(it.key(), jpeg)
            }.toImmutableList()
        }
            .flowOn(Dispatchers.IO)
            .map { DocumentUiModel(it) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = DocumentUiModel(),
            )

    private val _currentPageIndex = MutableStateFlow(0)
    private val _loadingPageId = MutableStateFlow<String?>(null)
    private val currentPageUiState: Flow<CurrentPageUiState?> =
        combine(_currentPageIndex, _pages, _loadingPageId) { index, pages, loadingId ->
            Pair(pages.getOrNull(index), loadingId)
        }
            .mapLatest { (page, loadingId) ->
                page?.let {
                    val isLoading = (it.id == loadingId)
                    val canBeCropped = page.metadata != null
                    val bitmap = try {
                        imageRepository.jpegBytes(it.key())?.toBitmap()
                    } catch (e: Exception) {
                        logger.e("MainViewModel", "Failed to load image for ${it.id}", e)
                        null
                    }
                    CurrentPageUiState(it.key(), bitmap, it.colorMode, canBeCropped, isLoading)
                }
            }
            .flowOn(Dispatchers.IO)

    val documentUiState: StateFlow<DocumentUiState> =
        combine(
            _currentPageIndex,
            currentPageUiState,
            documentUiModel,
            _selectedPageIds,
            _isSelectionMode,
            _canUndo,
            _canRedo,
            _currentLibraryDocumentId,
        ) { args ->
            val index = args[0] as Int
            @Suppress("UNCHECKED_CAST")
            val page = args[1] as CurrentPageUiState?
            val document = args[2] as DocumentUiModel
            @Suppress("UNCHECKED_CAST")
            val selected = args[3] as Set<String>
            val selectionMode = args[4] as Boolean
            val canUndo = args[5] as Boolean
            val canRedo = args[6] as Boolean
            val libDocId = args[7] as String?
            DocumentUiState(
                currentPageIndex = index,
                currentPage = page,
                document = document,
                selectedPageIds = selected,
                isSelectionMode = selectionMode,
                canUndo = canUndo,
                canRedo = canRedo,
                editingLibraryDocumentId = libDocId,
            )
        }
            .stateIn(
                viewModelScope, SharingStarted.Eagerly,
                DocumentUiState(0, null, DocumentUiModel()),
            )

    fun onPageSelected(index: Int) {
        _currentPageIndex.value = index
    }

    fun navigateTo(destination: Screen) {
        if (destination is Screen.Main.Document) {
            require(_pages.value.isNotEmpty()) {
                "Cannot navigate to DocumentScreen with zero pages"
            }
            _currentPageIndex.value = min(_pages.value.size - 1, destination.initialPage)
        }
        _navigationState.update { it?.navigateTo(destination) }
    }

    fun navigateBack() {
        _navigationState.update { stack -> stack?.navigateBack() }
    }

    // ── Undo / Redo ──────────────────────────────────────────────────────────

    private fun snapshotCurrentPages(): List<PageSnapshot> =
        _pages.value.map { PageSnapshot(it.id, it.manualRotation.degrees) }

    private fun pushUndo() {
        val snap = snapshotCurrentPages()
        undoStack.addLast(snap)
        if (undoStack.size > MAX_UNDO_STACK) undoStack.removeFirst()
        redoStack.clear()
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = false
    }

    fun undo() {
        val snap = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(snapshotCurrentPages())
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = true
        applySnapshot(snap)
    }

    fun redo() {
        val snap = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(snapshotCurrentPages())
        _canUndo.value = true
        _canRedo.value = redoStack.isNotEmpty()
        applySnapshot(snap)
    }

    private fun applySnapshot(snap: List<PageSnapshot>) {
        viewModelScope.launch {
            val pages = withContext(Dispatchers.IO) {
                imageRepository.restoreFromSnapshot(snap)
                imageRepository.pages()
            }
            _pages.value = pages
        }
    }

    // ── Page operations ──────────────────────────────────────────────────────

    fun rotateCurrentPage(clockwise: Boolean) {
        pushUndo()
        viewModelScope.launch {
            val pages = withContext(Dispatchers.IO) {
                imageRepository.rotate(currentPage().id, clockwise)
                imageRepository.pages()
            }
            _pages.value = pages
        }
    }

    fun movePage(id: String, newIndex: Int) {
        pushUndo()
        viewModelScope.launch {
            val pages = withContext(Dispatchers.IO) {
                imageRepository.movePage(id, newIndex)
                imageRepository.pages()
            }
            _pages.value = pages
        }
    }

    fun deleteCurrentPage() {
        undoStack.clear()
        redoStack.clear()
        _canUndo.value = false
        _canRedo.value = false
        viewModelScope.launch {
            val pages = withContext(Dispatchers.IO) {
                imageRepository.delete(currentPage().id)
                imageRepository.pages()
            }
            if (pages.isEmpty()) {
                navigateTo(Screen.Main.Camera)
                _currentPageIndex.value = 0
            } else if (_currentPageIndex.value >= pages.size) {
                _currentPageIndex.value = pages.size - 1
            }
            _pages.value = pages
        }
    }

    fun toggleCurrentPageColorMode() {
        pushUndo()
        viewModelScope.launch {
            val currentPage = currentPage()
            currentPage.colorMode?.let {
                _loadingPageId.value = currentPage.id
                val newColorMode = if (it == ColorMode.COLOR) ColorMode.GRAYSCALE else ColorMode.COLOR
                val pages = withContext(Dispatchers.IO) {
                    imageRepository.setColorMode(currentPage.id, newColorMode)
                    imageRepository.pages()
                }
                _pages.value = pages
                _loadingPageId.value = null
            }
        }
    }

    fun setCurrentPageUserQuad(userQuad: Quad) {
        viewModelScope.launch {
            val currentPage = currentPage()
            val totalRotation = currentPage.totalRotation()
            val rotateIterations = (4 - totalRotation.degrees / 90) % 4
            val newQuad = userQuad.rotate90(rotateIterations, ImageSize(1, 1))
            _loadingPageId.value = currentPage.id
            val pages = withContext(Dispatchers.IO) {
                imageRepository.setUserQuad(currentPage.id, newQuad)
                imageRepository.pages()
            }
            _pages.value = pages
            _loadingPageId.value = null
        }
    }

    private fun currentPage(): ScanPage {
        val index = _currentPageIndex.value
        val pages = _pages.value
        return pages.getOrNull(index) ?: throw IllegalStateException(
            "No current page for index $index (${pages.size} pages)"
        )
    }

    // ── Multi-select ─────────────────────────────────────────────────────────

    fun togglePageSelection(pageId: String) {
        _selectedPageIds.update { set ->
            if (pageId in set) set - pageId else set + pageId
        }
        _isSelectionMode.value = _selectedPageIds.value.isNotEmpty()
    }

    fun exitSelectionMode() {
        _selectedPageIds.value = emptySet()
        _isSelectionMode.value = false
    }

    fun batchDeleteSelected() {
        val ids = _selectedPageIds.value.toSet()
        if (ids.isEmpty()) return
        undoStack.clear(); redoStack.clear()
        _canUndo.value = false; _canRedo.value = false
        exitSelectionMode()
        viewModelScope.launch {
            val pages = withContext(Dispatchers.IO) {
                ids.forEach { imageRepository.delete(it) }
                imageRepository.pages()
            }
            if (pages.isEmpty()) {
                navigateTo(Screen.Main.Camera)
                _currentPageIndex.value = 0
            } else if (_currentPageIndex.value >= pages.size) {
                _currentPageIndex.value = pages.size - 1
            }
            _pages.value = pages
        }
    }

    fun batchRotateSelected(clockwise: Boolean) {
        val ids = _selectedPageIds.value.toSet()
        if (ids.isEmpty()) return
        pushUndo()
        viewModelScope.launch {
            val pages = withContext(Dispatchers.IO) {
                ids.forEach { imageRepository.rotate(it, clockwise) }
                imageRepository.pages()
            }
            _pages.value = pages
        }
    }

    fun batchToggleFilterSelected() {
        val ids = _selectedPageIds.value.toSet()
        if (ids.isEmpty()) return
        pushUndo()
        viewModelScope.launch {
            val currentPages = _pages.value
            val pages = withContext(Dispatchers.IO) {
                ids.forEach { id ->
                    val page = currentPages.find { it.id == id }
                    page?.colorMode?.let { mode ->
                        val newMode = if (mode == ColorMode.COLOR) ColorMode.GRAYSCALE else ColorMode.COLOR
                        imageRepository.setColorMode(id, newMode)
                    }
                }
                imageRepository.pages()
            }
            _pages.value = pages
        }
    }

    // ── Library integration ──────────────────────────────────────────────────

    /**
     * Saves the current scan session to the library with the given [name].
     * After saving, clears the session and navigates to Library.
     */
    fun saveToLibrary(name: String) {
        viewModelScope.launch {
            val pageCount = _pages.value.size
            if (pageCount == 0) return@launch
            val coverBytes = withContext(Dispatchers.IO) {
                _pages.value.firstOrNull()?.let {
                    imageRepository.getThumbnail(it.key())?.bytes
                }
            }
            withContext(Dispatchers.IO) {
                val docId = _currentLibraryDocumentId.value
                if (docId != null) {
                    libraryRepository.updateFromSession(docId, coverBytes, pageCount)
                } else {
                    libraryRepository.saveSession(name, coverBytes, pageCount)
                }
            }
            _currentLibraryDocumentId.value = null
            clearUndoRedo()
            startNewDocument()
            navigateTo(Screen.Main.Library)
        }
    }

    /**
     * Opens a library document for editing. Copies its files into the active session,
     * reloads ImageRepository from disk, and navigates to the Document screen.
     */
    fun openLibraryDocument(docId: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                imageRepository.clear()
                libraryRepository.exportToSession(docId)
                imageRepository.reload()
            }
            _currentLibraryDocumentId.value = docId
            clearUndoRedo()
            val pages = imageRepository.pages()
            _pages.value = pages
            if (pages.isNotEmpty()) {
                _currentPageIndex.value = 0
                navigateTo(Screen.Main.Document())
            }
        }
    }

    fun clearLibraryEditingState() {
        _currentLibraryDocumentId.value = null
    }

    private fun clearUndoRedo() {
        undoStack.clear(); redoStack.clear()
        _canUndo.value = false; _canRedo.value = false
    }

    fun startNewDocument() {
        _pages.value = persistentListOf()
        _currentLibraryDocumentId.value = null
        clearUndoRedo()
        exitSelectionMode()
        viewModelScope.launch {
            withContext(Dispatchers.IO) { imageRepository.clear() }
        }
    }

    fun handleImageCaptured(capturedPage: CapturedPage) {
        viewModelScope.launch {
            val pages = withContext(Dispatchers.IO) {
                val sourceJpeg = capturedPage.sourceJpeg.await()
                imageRepository.add(
                    capturedPage.pageJpeg,
                    sourceJpeg,
                    capturedPage.metadata,
                    capturedPage.colorMode,
                )
                imageRepository.pages()
            }
            _pages.value = pages
        }
    }

    private val _cropInitState = MutableStateFlow<CropInitState>(CropInitState.Loading)
    val cropInitState: StateFlow<CropInitState> = _cropInitState
    private var cropInitialStateJob: Job? = null

    fun onClickOnCropButton() {
        cropInitialStateJob?.cancel()
        cropInitialStateJob = viewModelScope.launch {
            _cropInitState.value = CropInitState.Loading
            val page = currentPage()
            val metadata = page.metadata
            val rotation = page.totalRotation()
            val bitmap = withContext(Dispatchers.IO) {
                val source = imageRepository.source(page.id)
                val bytes = source?.bytes ?: return@withContext null
                val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (original != null && rotation != Rotation.R0) {
                    val matrix = Matrix().apply { postRotate(rotation.degrees.toFloat()) }
                    Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
                } else original
            }
            val quad = metadata?.normalizedQuad?.rotate90(rotation.degrees / 90, ImageSize(1, 1))
            _cropInitState.value = if (bitmap == null || quad == null)
                CropInitState.Error
            else
                CropInitState.Ready(page.id, bitmap, quad)
            navigateTo(Screen.Main.EditImage)
        }
    }

    /** Expose unfinished session state for library screen header. */
    val hasUnfinishedSession: StateFlow<Boolean> =
        _pages.map { it.isNotEmpty() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val unfinishedPageCount: StateFlow<Int> =
        _pages.map { it.size }
            .stateIn(viewModelScope, SharingStarted.Eagerly, 0)
}
