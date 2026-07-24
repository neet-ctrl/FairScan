/*
 * Copyright 2025-2026 The FairScan authors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 */
package org.fairscan.app.ui.screens.library

import android.graphics.BitmapFactory
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.fairscan.app.data.LibraryDocumentInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    uiState: LibraryUiState,
    onDocumentClick: (String) -> Unit,
    onDocumentLongClick: (String) -> Unit,
    onToggleSelection: (String) -> Unit,
    onDeleteSelected: () -> Unit,
    onSelectAll: () -> Unit,
    onMergeSelected: (String) -> Unit,
    onClearSelection: () -> Unit,
    onDeleteDocument: (String) -> Unit,
    onRenameDocument: (String, String) -> Unit,
    onDuplicateDocument: (String) -> Unit,
    onNewScan: () -> Unit,
    onScanClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
    onResumeScan: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSortOrderChange: (LibrarySortOrder) -> Unit,
    onSnackbarDismissed: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var renameDocumentId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteDocumentId by rememberSaveable { mutableStateOf<String?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    var previewDocumentId by rememberSaveable { mutableStateOf<String?>(null) }
    var showMergeDialog by rememberSaveable { mutableStateOf(false) }
    var mergeDocumentName by rememberSaveable { mutableStateOf("Merged Document") }

    // Tablet: 3 columns; phone: 2 columns
    val configuration = LocalConfiguration.current
    val columns = if (configuration.screenWidthDp >= 600) 3 else 2

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            onSnackbarDismissed()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            AnimatedContent(
                targetState = uiState.isSelectionMode,
                transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
                label = "bottom_bar",
            ) { isSelecting ->
                if (isSelecting) {
                    SelectionBottomBar(
                        selectedCount = uiState.selectedIds.size,
                        onDeleteSelected = onDeleteSelected,
                        onSelectAll = onSelectAll,
                        onMerge = {
                            mergeDocumentName = "Merged Document"
                            showMergeDialog = true
                        },
                        onClearSelection = onClearSelection,
                    )
                } else {
                    NavigationBar {
                        NavigationBarItem(
                            selected = true,
                            onClick = {},
                            icon = { Icon(Icons.Outlined.FolderOpen, contentDescription = null) },
                            label = { Text("Library") },
                        )
                        NavigationBarItem(
                            selected = false,
                            onClick = onScanClick,
                            icon = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                            label = { Text("Scan") },
                        )
                        NavigationBarItem(
                            selected = false,
                            onClick = onSettingsClick,
                            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                            label = { Text("Settings") },
                        )
                    }
                }
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues),
        ) {
            // ── Top bar ──────────────────────────────────────────────────────
            AnimatedContent(
                targetState = uiState.isSelectionMode,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
                label = "topbar",
            ) { isSelecting ->
                if (isSelecting) {
                    SelectionTopBar(
                        selectedCount = uiState.selectedIds.size,
                        totalCount = uiState.filteredDocuments.size,
                        onClearSelection = onClearSelection,
                        onDeleteSelected = onDeleteSelected,
                        onSelectAll = onSelectAll,
                        onMerge = {
                            mergeDocumentName = "Merged Document"
                            showMergeDialog = true
                        },
                    )
                } else {
                    LibraryTopBar(
                        searchQuery = uiState.searchQuery,
                        onSearchQueryChange = onSearchQueryChange,
                        sortOrder = uiState.sortOrder,
                        onSortMenuClick = { showSortMenu = true },
                        showSortMenu = showSortMenu,
                        onSortOrderChange = { order -> onSortOrderChange(order); showSortMenu = false },
                        onDismissSortMenu = { showSortMenu = false },
                        onAboutClick = onAboutClick,
                    )
                }
            }

            // ── Unfinished session banner ─────────────────────────────────────
            AnimatedVisibility(
                visible = uiState.hasUnfinishedSession && !uiState.isSelectionMode,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                UnfinishedSessionBanner(
                    pageCount = uiState.unfinishedPageCount,
                    onResume = onResumeScan,
                )
            }

            // ── Document grid ─────────────────────────────────────────────────
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.filteredDocuments.isEmpty()) {
                EmptyLibraryState(
                    hasSearch = uiState.searchQuery.isNotEmpty(),
                    onNewScan = onScanClick,
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(uiState.filteredDocuments, key = { it.id }) { doc ->
                        val coverBytes = uiState.covers[doc.id]
                        val isSelected = doc.id in uiState.selectedIds

                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value != SwipeToDismissBoxValue.Settled && !uiState.isSelectionMode) {
                                    deleteDocumentId = doc.id
                                }
                                false // we show dialog; don't auto-remove
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            modifier = Modifier.animateItem(),
                            enableDismissFromStartToEnd = false,
                            enableDismissFromEndToStart = !uiState.isSelectionMode,
                            backgroundContent = {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .clip(MaterialTheme.shapes.large)
                                        .background(MaterialTheme.colorScheme.errorContainer),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(end = 20.dp),
                                    )
                                }
                            },
                        ) {
                            DocumentCard(
                                doc = doc,
                                coverBytes = coverBytes,
                                isSelected = isSelected,
                                isSelectionMode = uiState.isSelectionMode,
                                onClick = {
                                    if (uiState.isSelectionMode) onToggleSelection(doc.id)
                                    else previewDocumentId = doc.id
                                },
                                onLongClick = { onDocumentLongClick(doc.id) },
                                onRename = { renameDocumentId = doc.id },
                                onDuplicate = { onDuplicateDocument(doc.id) },
                                onDelete = { deleteDocumentId = doc.id },
                            )
                        }
                    }
                    // bottom padding
                    item { Spacer(Modifier.height(80.dp)) }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    // ── Document preview bottom sheet ─────────────────────────────────────────
    previewDocumentId?.let { id ->
        val doc = uiState.documents.find { it.id == id }
        val coverBytes = uiState.covers[id]
        if (doc != null) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { previewDocumentId = null },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                DocumentPreviewSheet(
                    doc = doc,
                    coverBytes = coverBytes,
                    onOpen = { previewDocumentId = null; onDocumentClick(id) },
                    onRename = { previewDocumentId = null; renameDocumentId = id },
                    onDuplicate = { previewDocumentId = null; onDuplicateDocument(id) },
                    onDelete = { previewDocumentId = null; deleteDocumentId = id },
                )
            }
        }
    }

    // ── Rename dialog ─────────────────────────────────────────────────────────
    renameDocumentId?.let { id ->
        val doc = uiState.documents.find { it.id == id }
        if (doc != null) {
            var name by rememberSaveable(id) { mutableStateOf(doc.name) }
            AlertDialog(
                onDismissRequest = { renameDocumentId = null },
                shape = MaterialTheme.shapes.large,
                title = { Text("Rename document") },
                text = {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Document name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                confirmButton = {
                    Button(onClick = { onRenameDocument(id, name); renameDocumentId = null }) {
                        Text("Rename")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { renameDocumentId = null }) { Text("Cancel") }
                },
            )
        }
    }

    // ── Delete confirmation ───────────────────────────────────────────────────
    deleteDocumentId?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteDocumentId = null },
            shape = MaterialTheme.shapes.large,
            title = { Text("Delete document") },
            text = { Text("This document will be permanently deleted.") },
            confirmButton = {
                Button(
                    onClick = { onDeleteDocument(id); deleteDocumentId = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteDocumentId = null }) { Text("Cancel") }
            },
        )
    }

    // ── Merge dialog ──────────────────────────────────────────────────────────
    if (showMergeDialog) {
        AlertDialog(
            onDismissRequest = { showMergeDialog = false },
            shape = MaterialTheme.shapes.large,
            title = { Text("Merge ${uiState.selectedIds.size} documents") },
            text = {
                Column {
                    Text(
                        "All pages from the selected documents will be combined into one document.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = mergeDocumentName,
                        onValueChange = { mergeDocumentName = it },
                        label = { Text("Merged document name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showMergeDialog = false
                        onMergeSelected(mergeDocumentName.ifBlank { "Merged Document" })
                    },
                    enabled = mergeDocumentName.isNotBlank(),
                ) { Text("Merge") }
            },
            dismissButton = {
                TextButton(onClick = { showMergeDialog = false }) { Text("Cancel") }
            },
        )
    }
}

// ── Document preview bottom sheet ─────────────────────────────────────────────

@Composable
private fun DocumentPreviewSheet(
    doc: LibraryDocumentInfo,
    coverBytes: ByteArray?,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Preview image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .padding(horizontal = 24.dp)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (coverBytes != null) {
                val bitmap = remember(coverBytes) {
                    BitmapFactory.decodeByteArray(coverBytes, 0, coverBytes.size)
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Icon(
                        Icons.Outlined.Description,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                }
            } else {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            doc.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "${doc.pageCount} page${if (doc.pageCount == 1) "" else "s"} · ${formatRelativeDate(doc.modifiedAt)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onOpen,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        ) {
            Icon(Icons.Default.OpenInFull, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Open Document", style = MaterialTheme.typography.labelLarge)
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onRename,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Rename")
            }
            OutlinedButton(
                onClick = onDuplicate,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Copy")
            }
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
            ) {
                Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Delete")
            }
        }
    }
}

// ── Selection top bar (replaces normal top bar in selection mode) ─────────────

@Composable
private fun SelectionTopBar(
    selectedCount: Int,
    totalCount: Int,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    onSelectAll: () -> Unit,
    onMerge: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClearSelection) {
            Icon(Icons.Default.Close, "Clear selection")
        }
        Text(
            "$selectedCount of $totalCount selected",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        if (selectedCount >= 2) {
            TextButton(onClick = onMerge) {
                Text("Merge", color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        TextButton(onClick = onSelectAll) {
            Text("All", color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        IconButton(onClick = onDeleteSelected) {
            Icon(
                Icons.Default.Delete,
                "Delete selected",
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

// ── Selection bottom bar ──────────────────────────────────────────────────────

@Composable
private fun SelectionBottomBar(
    selectedCount: Int,
    onDeleteSelected: () -> Unit,
    onSelectAll: () -> Unit,
    onMerge: () -> Unit,
    onClearSelection: () -> Unit,
) {
    Surface(
        shadowElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClearSelection) {
                Icon(Icons.Default.Close, "Clear selection")
            }
            Text(
                "$selectedCount selected",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (selectedCount >= 2) {
                TextButton(onClick = onMerge) { Text("Merge") }
            }
            TextButton(onClick = onSelectAll) { Text("All") }
            IconButton(onClick = onDeleteSelected) {
                Icon(
                    Icons.Default.Delete,
                    "Delete selected",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

// ── Top bar ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    sortOrder: LibrarySortOrder,
    onSortMenuClick: () -> Unit,
    showSortMenu: Boolean,
    onSortOrderChange: (LibrarySortOrder) -> Unit,
    onDismissSortMenu: () -> Unit,
    onAboutClick: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Library",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Box {
                IconButton(onClick = onSortMenuClick) {
                    Icon(Icons.Default.Sort, "Sort")
                }
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = onDismissSortMenu,
                ) {
                    val sortOptions = listOf(
                        LibrarySortOrder.DATE_DESC to "Newest first",
                        LibrarySortOrder.DATE_ASC to "Oldest first",
                        LibrarySortOrder.NAME_ASC to "Name A–Z",
                        LibrarySortOrder.NAME_DESC to "Name Z–A",
                        LibrarySortOrder.PAGE_COUNT_DESC to "Most pages",
                    )
                    sortOptions.forEach { (order, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = { onSortOrderChange(order) },
                            trailingIcon = {
                                if (sortOrder == order) Icon(Icons.Default.Check, null)
                            },
                        )
                    }
                }
            }
            IconButton(onClick = onAboutClick) {
                Icon(Icons.Default.Info, "About")
            }
        }
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search documents…") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, "Clear search")
                    }
                }
            },
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            singleLine = true,
        )
        Spacer(Modifier.height(4.dp))
    }
}

// ── Unfinished session banner ──────────────────────────────────────────────────

@Composable
private fun UnfinishedSessionBanner(pageCount: Int, onResume: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onResume),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.secondary)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Continue scanning",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    "$pageCount page${if (pageCount == 1) "" else "s"} not yet saved",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                )
            }
            Icon(Icons.Default.ChevronRight, null)
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyLibraryState(hasSearch: Boolean, onNewScan: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.extraLarge,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (hasSearch) Icons.Default.SearchOff else Icons.Outlined.FolderOpen,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.size(56.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = if (hasSearch) "No documents found" else "Your library is empty",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (hasSearch) "Try a different search term"
            else "Tap Scan below to capture your first document",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (!hasSearch) {
            Spacer(Modifier.height(28.dp))
            Button(onClick = onNewScan) {
                Icon(Icons.Default.CameraAlt, null)
                Spacer(Modifier.width(8.dp))
                Text("Start Scanning")
            }
        }
    }
}

// ── Document card ──────────────────────────────────────────────────────────────

@Composable
private fun DocumentCard(
    doc: LibraryDocumentInfo,
    coverBytes: ByteArray?,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    val border = if (isSelected)
        BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
    else
        BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(isSelectionMode) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() },
                )
            },
        shape = MaterialTheme.shapes.large,
        border = border,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 0.dp else 2.dp),
    ) {
        Box {
            Column {
                // Cover image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    if (coverBytes != null) {
                        val bitmap = remember(coverBytes) {
                            BitmapFactory.decodeByteArray(coverBytes, 0, coverBytes.size)
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                                contentScale = ContentScale.Crop,
                            )
                            // Subtle gradient overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.12f),
                                            )
                                        )
                                    )
                            )
                        } else {
                            CoverPlaceholder()
                        }
                    } else {
                        CoverPlaceholder()
                    }

                    // Page count badge
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.82f),
                    ) {
                        Text(
                            "${doc.pageCount}p",
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }

                // Document info
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Text(
                        doc.name,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        formatRelativeDate(doc.modifiedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Overflow menu button
            if (!isSelectionMode) {
                Box(modifier = Modifier.align(Alignment.TopEnd)) {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            "More options",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            leadingIcon = { Icon(Icons.Default.Edit, null) },
                            onClick = { showMenu = false; onRename() },
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicate") },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
                            onClick = { showMenu = false; onDuplicate() },
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, null,
                                    tint = MaterialTheme.colorScheme.error)
                            },
                            onClick = { showMenu = false; onDelete() },
                        )
                    }
                }
            }

            // Selection checkbox overlay
            if (isSelectionMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .size(26.dp)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            CircleShape,
                        )
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CoverPlaceholder() {
    Icon(
        Icons.Outlined.Description,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.size(40.dp),
    )
}

private fun formatRelativeDate(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        diff < 7 * 86_400_000L -> "${diff / 86_400_000}d ago"
        else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}
