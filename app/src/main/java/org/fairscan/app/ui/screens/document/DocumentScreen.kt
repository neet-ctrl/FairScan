/*
 * Copyright 2025-2026 The FairScan authors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 */
package org.fairscan.app.ui.screens.document

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.toImmutableList
import net.engawapg.lib.zoomable.ZoomState
import net.engawapg.lib.zoomable.zoomable
import org.fairscan.app.R
import org.fairscan.app.domain.PageViewKey
import org.fairscan.app.domain.Rotation
import org.fairscan.app.ui.Navigation
import org.fairscan.app.ui.components.CommonPageListState
import org.fairscan.app.ui.components.ConfirmationDialog
import org.fairscan.app.ui.components.MainActionButton
import org.fairscan.app.ui.components.MyScaffold
import org.fairscan.app.ui.components.SecondaryActionButton
import org.fairscan.app.ui.dummyNavigation
import org.fairscan.app.ui.fakeDocument
import org.fairscan.app.ui.fakeImage
import org.fairscan.app.ui.theme.FairScanTheme
import org.fairscan.imageprocessing.ColorMode
import org.fairscan.imageprocessing.ColorMode.COLOR
import org.fairscan.imageprocessing.ColorMode.GRAYSCALE

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentScreen(
    uiState: DocumentUiState,
    navigation: Navigation,
    onExportClick: () -> Unit,
    onSaveToLibraryClick: (() -> Unit)?,
    onDeleteImage: () -> Unit,
    onRotateImage: (Boolean) -> Unit,
    onToggleColorMode: () -> Unit,
    onCropClick: () -> Unit,
    onPageReorder: (String, Int) -> Unit,
    onPageSelected: (Int) -> Unit,
    onTogglePageSelection: (String) -> Unit,
    onBatchDelete: () -> Unit,
    onBatchRotate: (Boolean) -> Unit,
    onBatchFilterToggle: () -> Unit,
    onExitSelectionMode: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
) {
    val showDeletePageDialog = rememberSaveable { mutableStateOf(false) }
    val showSaveDialog = rememberSaveable { mutableStateOf(false) }
    var saveDocumentName by rememberSaveable { mutableStateOf("") }

    val document = uiState.document
    val currentPageIndex = uiState.currentPageIndex

    BackHandler {
        if (uiState.isSelectionMode) onExitSelectionMode()
        else navigation.back()
    }

    val listState = rememberLazyListState()
    LaunchedEffect(currentPageIndex) {
        listState.scrollToItem(currentPageIndex)
    }

    // Save-to-library dialog
    if (showSaveDialog.value) {
        SaveToLibraryDialog(
            initialName = saveDocumentName,
            onNameChange = { saveDocumentName = it },
            onConfirm = {
                showSaveDialog.value = false
                onSaveToLibraryClick?.invoke()
            },
            onDismiss = { showSaveDialog.value = false },
        )
    }

    MyScaffold(
        navigation = navigation,
        pageListState = if (!uiState.isSelectionMode) CommonPageListState(
            document,
            onPageClick = { index -> onPageSelected(index) },
            onPageReorder = onPageReorder,
            currentPageIndex = currentPageIndex,
            listState = listState,
            showPageNumbers = true,
        ) else null,
        bottomBar = {
            if (uiState.isSelectionMode) {
                SelectionBottomBar(
                    selectedCount = uiState.selectedPageIds.size,
                    onBatchDelete = onBatchDelete,
                    onBatchRotateLeft = { onBatchRotate(false) },
                    onBatchRotateRight = { onBatchRotate(true) },
                    onBatchFilter = onBatchFilterToggle,
                    onCancel = onExitSelectionMode,
                )
            } else {
                DocumentBottomBar(
                    onExportClick = onExportClick,
                    onAddPageClick = navigation.toCameraScreen,
                    onSaveToLibraryClick = if (onSaveToLibraryClick != null) {
                        {
                            if (saveDocumentName.isBlank()) {
                                saveDocumentName = "Document"
                            }
                            showSaveDialog.value = true
                        }
                    } else null,
                    canUndo = uiState.canUndo,
                    canRedo = uiState.canRedo,
                    onUndo = onUndo,
                    onRedo = onRedo,
                    isEditingLibraryDoc = uiState.editingLibraryDocumentId != null,
                )
            }
        },
    ) { modifier ->
        DocumentPreview(
            uiState,
            { showDeletePageDialog.value = true },
            onRotateImage,
            onToggleColorMode,
            onCropClick,
            onTogglePageSelection,
            modifier,
        )
        if (showDeletePageDialog.value) {
            ConfirmationDialog(
                title = stringResource(R.string.delete_page),
                message = stringResource(R.string.delete_page_warning),
                showDialog = showDeletePageDialog,
            ) { onDeleteImage() }
        }
    }
}

@Composable
private fun SaveToLibraryDialog(
    initialName: String,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        title = { Text("Save to Library") },
        text = {
            Column {
                Text(
                    "Give this document a name:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = initialName,
                    onValueChange = onNameChange,
                    label = { Text("Document name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun DocumentPreview(
    uiState: DocumentUiState,
    onDeleteImage: () -> Unit,
    onRotateImage: (Boolean) -> Unit,
    onToggleColorMode: () -> Unit,
    onCropClick: () -> Unit,
    onTogglePageSelection: (String) -> Unit,
    modifier: Modifier,
) {
    val currentPageIndex = uiState.currentPageIndex
    val document = uiState.document
    Column(modifier = modifier.background(MaterialTheme.colorScheme.background)) {
        Box(modifier = Modifier.fillMaxSize()) {
            val bitmap = uiState.currentPage?.bitmap
            val pageKey = uiState.currentPage?.key
            if (bitmap != null && pageKey != null) {
                val imageBitmap = bitmap.asImageBitmap()
                val zoomState = remember(pageKey) {
                    ZoomState(contentSize = Size(bitmap.width.toFloat(), bitmap.height.toFloat()))
                }
                Surface(
                    modifier = Modifier.fillMaxSize(0.92f).align(Alignment.Center),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    shadowElevation = 10.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            bitmap = imageBitmap,
                            contentDescription = null,
                            modifier = Modifier.padding(8.dp).zoomable(zoomState)
                        )
                    }
                }
            }
            if (uiState.currentPage?.isLoading == true) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            }
            EditButtons(uiState, onToggleColorMode, onCropClick, modifier = Modifier.align(Alignment.BottomStart))
            RotationButtons(onRotateImage, Modifier.align(Alignment.BottomCenter))
            SecondaryActionButton(
                Icons.Outlined.Delete,
                contentDescription = stringResource(R.string.delete_page),
                onClick = { onDeleteImage() },
                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
            )
            Surface(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp),
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.84f),
            ) {
                Text(
                    "${currentPageIndex + 1} / ${document.pageCount()}",
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                )
            }
        }
    }
}

@Composable
fun RotationButtons(onRotateImage: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Surface(
            modifier = modifier.padding(8.dp),
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            tonalElevation = 4.dp,
        ) {
            Row(modifier = Modifier.padding(4.dp)) {
                @Suppress("DEPRECATION")
                SecondaryActionButton(
                    icon = Icons.Default.RotateLeft,
                    contentDescription = stringResource(R.string.rotate_left),
                    onClick = { onRotateImage(false) }
                )
                Spacer(Modifier.width(8.dp))
                @Suppress("DEPRECATION")
                SecondaryActionButton(
                    icon = Icons.Default.RotateRight,
                    contentDescription = stringResource(R.string.rotate_right),
                    onClick = { onRotateImage(true) }
                )
            }
        }
    }
}

@Composable
fun EditButtons(
    uiState: DocumentUiState,
    onToggleColorMode: () -> Unit,
    onCropClick: () -> Unit,
    modifier: Modifier,
) {
    Row(modifier = modifier.padding(8.dp)) {
        uiState.currentPage?.colorMode?.let {
            ColorModeButton(currentColorMode = it, onToggle = { onToggleColorMode() })
        }
        Spacer(Modifier.width(8.dp))
        if (uiState.currentPage?.canBeCropped == true) {
            SecondaryActionButton(
                icon = Icons.Default.Crop,
                contentDescription = stringResource(R.string.crop),
                onClick = onCropClick,
            )
        }
    }
}

@Composable
fun ColorModeButton(currentColorMode: ColorMode, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        SecondaryActionButton(
            icon = Icons.Default.AutoFixHigh,
            contentDescription = stringResource(R.string.color_mode),
            onClick = { expanded = true },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.color_mode_color)) },
                leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null) },
                onClick = { if (currentColorMode != COLOR) onToggle(); expanded = false },
                trailingIcon = { if (currentColorMode == COLOR) Icon(Icons.Default.Check, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.color_mode_grayscale)) },
                leadingIcon = { Icon(Icons.Default.Contrast, contentDescription = null) },
                onClick = { if (currentColorMode != GRAYSCALE) onToggle(); expanded = false },
                trailingIcon = { if (currentColorMode == GRAYSCALE) Icon(Icons.Default.Check, contentDescription = null) }
            )
        }
    }
}

@Composable
private fun DocumentBottomBar(
    onExportClick: () -> Unit,
    onAddPageClick: () -> Unit,
    onSaveToLibraryClick: (() -> Unit)?,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    isEditingLibraryDoc: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Undo / Redo
        Row {
            IconButton(onClick = onUndo, enabled = canUndo) {
                Icon(Icons.Default.Undo, contentDescription = "Undo",
                    tint = if (canUndo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
            }
            IconButton(onClick = onRedo, enabled = canRedo) {
                Icon(Icons.Default.Redo, contentDescription = "Redo",
                    tint = if (canRedo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
            }
        }

        // Add page button
        OutlinedButton(
            onClick = onAddPageClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.add_page), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        // Save / Export
        if (onSaveToLibraryClick != null) {
            MainActionButton(
                onClick = if (isEditingLibraryDoc) onExportClick else onSaveToLibraryClick,
                icon = if (isEditingLibraryDoc) Icons.Default.SaveAlt else Icons.Default.SaveAlt,
                text = if (isEditingLibraryDoc) "Update" else "Save",
            )
        } else {
            MainActionButton(
                onClick = onExportClick,
                icon = Icons.Default.Done,
                text = stringResource(R.string.export),
            )
        }
    }
}

@Composable
private fun SelectionBottomBar(
    selectedCount: Int,
    onBatchDelete: () -> Unit,
    onBatchRotateLeft: () -> Unit,
    onBatchRotateRight: () -> Unit,
    onBatchFilter: () -> Unit,
    onCancel: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TextButton(onClick = onCancel) { Text("Cancel") }
        Text(
            "$selectedCount selected",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Row {
            IconButton(onClick = onBatchRotateLeft) {
                Icon(Icons.Default.RotateLeft, contentDescription = "Rotate left")
            }
            IconButton(onClick = onBatchRotateRight) {
                Icon(Icons.Default.RotateRight, contentDescription = "Rotate right")
            }
            IconButton(onClick = onBatchFilter) {
                Icon(Icons.Default.AutoFixHigh, contentDescription = "Toggle filter")
            }
            IconButton(onClick = onBatchDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete selected",
                    tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
@Preview
@Preview(locale = "ar")
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
fun DocumentScreenPreview() {
    FairScanTheme {
        val image = fakeImage("gallica.bnf.fr-bpt6k5530456s-1", LocalContext.current).toBitmap()
        val document = fakeDocument(
            listOf(1, 2).map { "gallica.bnf.fr-bpt6k5530456s-$it" }.toImmutableList(),
            LocalContext.current,
        )
        val key = PageViewKey("123", Rotation.R0, null, 0)
        DocumentScreen(
            uiState = DocumentUiState(1, CurrentPageUiState(key, image, COLOR, true), document),
            navigation = dummyNavigation(),
            onExportClick = {},
            onSaveToLibraryClick = {},
            onDeleteImage = {},
            onRotateImage = {},
            onToggleColorMode = {},
            onCropClick = {},
            onPageReorder = { _, _ -> },
            onPageSelected = {},
            onTogglePageSelection = {},
            onBatchDelete = {},
            onBatchRotate = {},
            onBatchFilterToggle = {},
            onExitSelectionMode = {},
            onUndo = {},
            onRedo = {},
        )
    }
}
