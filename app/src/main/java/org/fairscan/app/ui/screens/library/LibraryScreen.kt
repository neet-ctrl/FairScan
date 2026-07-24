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
import androidx.compose.ui.text.font.FontWeight
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
    onClearSelection: () -> Unit,
    onDeleteDocument: (String) -> Unit,
    onRenameDocument: (String, String) -> Unit,
    onDuplicateDocument: (String) -> Unit,
    onNewScan: () -> Unit,
    onResumeScan: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSortOrderChange: (LibrarySortOrder) -> Unit,
    onSnackbarDismissed: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var renameDocumentId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteDocumentId by rememberSaveable { mutableStateOf<String?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            onSnackbarDismissed()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!uiState.isSelectionMode) {
                ExtendedFloatingActionButton(
                    onClick = onNewScan,
                    icon = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                    text = { Text("New Scan") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues),
        ) {
            // ── Top bar ─────────────────────────────────────────────────────
            AnimatedContent(
                targetState = uiState.isSelectionMode,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                label = "topbar",
            ) { isSelecting ->
                if (isSelecting) {
                    SelectionTopBar(
                        selectedCount = uiState.selectedIds.size,
                        totalCount = uiState.filteredDocuments.size,
                        onSelectAll = { /* handled by parent */ },
                        onClearSelection = onClearSelection,
                        onDeleteSelected = onDeleteSelected,
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
                    )
                }
            }

            // ── Unfinished session banner ────────────────────────────────────
            if (uiState.hasUnfinishedSession && !uiState.isSelectionMode) {
                UnfinishedSessionBanner(
                    pageCount = uiState.unfinishedPageCount,
                    onResume = onResumeScan,
                )
            }

            // ── Document grid ────────────────────────────────────────────────
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.filteredDocuments.isEmpty()) {
                EmptyLibraryState(
                    hasSearch = uiState.searchQuery.isNotEmpty(),
                    onNewScan = onNewScan,
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(uiState.filteredDocuments, key = { it.id }) { doc ->
                        val coverBytes = uiState.covers[doc.id]
                        val isSelected = doc.id in uiState.selectedIds

                        DocumentCard(
                            doc = doc,
                            coverBytes = coverBytes,
                            isSelected = isSelected,
                            isSelectionMode = uiState.isSelectionMode,
                            onClick = {
                                if (uiState.isSelectionMode) onToggleSelection(doc.id)
                                else onDocumentClick(doc.id)
                            },
                            onLongClick = { onDocumentLongClick(doc.id) },
                            onRename = { renameDocumentId = doc.id },
                            onDuplicate = { onDuplicateDocument(doc.id) },
                            onDelete = { deleteDocumentId = doc.id },
                        )
                    }
                    // bottom padding for FAB
                    item { Spacer(Modifier.height(72.dp)) }
                    item { Spacer(Modifier.height(72.dp)) }
                }
            }
        }
    }

    // ── Rename dialog ──────────────────────────────────────────────────────
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

    // ── Delete confirmation ────────────────────────────────────────────────
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
}

// ── Sub-composables ────────────────────────────────────────────────────────────

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
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
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
                    Icon(Icons.Default.Sort, contentDescription = "Sort")
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
        }
        SearchBar(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
        )
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search documents…") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
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

@Composable
private fun SelectionTopBar(
    selectedCount: Int,
    totalCount: Int,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClearSelection) {
            Icon(Icons.Default.Close, contentDescription = "Clear selection")
        }
        Text(
            "$selectedCount of $totalCount selected",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        IconButton(onClick = onDeleteSelected) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete selected",
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun UnfinishedSessionBanner(pageCount: Int, onResume: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
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
            Icon(
                Icons.Default.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
            )
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
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun EmptyLibraryState(hasSearch: Boolean, onNewScan: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
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
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (hasSearch) "Try a different search term"
            else "Scan a document and save it here to build your library",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!hasSearch) {
            Spacer(Modifier.height(28.dp))
            Button(onClick = onNewScan) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Start Scanning")
            }
        }
    }
}

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
                            // Gradient overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.15f)),
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
                            "${doc.pageCount} p",
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
                            contentDescription = "More options",
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
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
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
                            contentDescription = null,
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Outlined.Description,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(40.dp),
        )
    }
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
