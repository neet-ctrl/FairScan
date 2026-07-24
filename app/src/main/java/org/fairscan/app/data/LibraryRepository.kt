/*
 * Copyright 2025-2026 The FairScan authors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 */
package org.fairscan.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Manages the persistent document library stored at [filesDir]/library/.
 *
 * Each saved document lives in its own subdirectory:
 *   [filesDir]/library/{id}/scanned_pages/  — processed image files
 *   [filesDir]/library/{id}/sources/        — original source images
 *   [filesDir]/library/{id}/cover.jpg       — first-page thumbnail
 *
 * The index of all documents is kept in [filesDir]/library/index.json.
 *
 * The current active session is always stored directly in [filesDir]/scanned_pages/
 * and [filesDir]/sources/ (unchanged from before). This repository copies files
 * between the session dir and library dirs.
 */
class LibraryRepository(val filesDir: File) {

    private val json = Json { prettyPrint = false; encodeDefaults = true; ignoreUnknownKeys = true }
    private val libraryDir = File(filesDir, "library").apply { mkdirs() }
    private val indexFile = File(libraryDir, "index.json")

    private val _documents = MutableStateFlow<List<LibraryDocumentInfo>>(emptyList())
    val documents: StateFlow<List<LibraryDocumentInfo>> = _documents.asStateFlow()

    init {
        _documents.value = loadIndex()
    }

    // --- Index management ---

    private fun loadIndex(): List<LibraryDocumentInfo> {
        if (!indexFile.exists()) return emptyList()
        return runCatching {
            json.decodeFromString<LibraryIndex>(indexFile.readText()).documents
        }.getOrElse { emptyList() }
    }

    private fun saveIndex() {
        indexFile.writeText(json.encodeToString(LibraryIndex(_documents.value)))
    }

    // --- Public API ---

    /**
     * Saves the current session (files in [filesDir]/scanned_pages and sources)
     * into a new library document entry with the given [name].
     * [coverBytes] is the JPEG bytes of the cover thumbnail (first page).
     * [pageCount] is the number of pages in the document.
     */
    suspend fun saveSession(
        name: String,
        coverBytes: ByteArray?,
        pageCount: Int,
    ): LibraryDocumentInfo = withContext(Dispatchers.IO) {
        val id = System.currentTimeMillis().toString()
        val docDir = File(libraryDir, id).apply { mkdirs() }

        copySessionToDocDir(docDir)
        if (coverBytes != null) {
            File(docDir, "cover.jpg").writeBytes(coverBytes)
        }

        val now = System.currentTimeMillis()
        val info = LibraryDocumentInfo(
            id = id,
            name = name,
            createdAt = now,
            modifiedAt = now,
            pageCount = pageCount,
        )
        _documents.value = listOf(info) + _documents.value
        saveIndex()
        info
    }

    /**
     * Updates an existing library document with the current session state.
     */
    suspend fun updateFromSession(
        id: String,
        coverBytes: ByteArray?,
        pageCount: Int,
    ) = withContext(Dispatchers.IO) {
        val docDir = File(libraryDir, id)
        if (!docDir.exists()) return@withContext

        File(docDir, "scanned_pages").deleteRecursively()
        File(docDir, "sources").deleteRecursively()
        copySessionToDocDir(docDir)
        if (coverBytes != null) {
            File(docDir, "cover.jpg").writeBytes(coverBytes)
        }

        val now = System.currentTimeMillis()
        _documents.value = _documents.value.map { doc ->
            if (doc.id == id) doc.copy(modifiedAt = now, pageCount = pageCount) else doc
        }
        saveIndex()
    }

    /**
     * Copies the library document's files into the active session directory,
     * replacing whatever was there. After this call, the caller must reload
     * the ImageRepository from disk.
     */
    suspend fun exportToSession(id: String) = withContext(Dispatchers.IO) {
        val docDir = File(libraryDir, id)
        if (!docDir.exists()) return@withContext

        // Clear session
        File(filesDir, "scanned_pages").also { it.deleteRecursively(); it.mkdirs() }
        File(filesDir, "sources").also { it.deleteRecursively(); it.mkdirs() }

        // Copy library doc to session
        File(docDir, "scanned_pages").also { src ->
            if (src.exists()) src.copyRecursively(File(filesDir, "scanned_pages"), overwrite = true)
        }
        File(docDir, "sources").also { src ->
            if (src.exists()) src.copyRecursively(File(filesDir, "sources"), overwrite = true)
        }
    }

    suspend fun deleteDocument(id: String) = withContext(Dispatchers.IO) {
        File(libraryDir, id).deleteRecursively()
        _documents.value = _documents.value.filter { it.id != id }
        saveIndex()
    }

    suspend fun deleteDocuments(ids: Set<String>) = withContext(Dispatchers.IO) {
        ids.forEach { File(libraryDir, it).deleteRecursively() }
        _documents.value = _documents.value.filter { it.id !in ids }
        saveIndex()
    }

    suspend fun renameDocument(id: String, newName: String) = withContext(Dispatchers.IO) {
        _documents.value = _documents.value.map { doc ->
            if (doc.id == id) doc.copy(name = newName, modifiedAt = System.currentTimeMillis())
            else doc
        }
        saveIndex()
    }

    suspend fun duplicateDocument(id: String, newName: String): LibraryDocumentInfo? =
        withContext(Dispatchers.IO) {
            val source = _documents.value.find { it.id == id } ?: return@withContext null
            val sourceDir = File(libraryDir, id)
            val newId = (System.currentTimeMillis() + 1).toString()
            val newDir = File(libraryDir, newId).apply { mkdirs() }

            if (sourceDir.exists()) {
                sourceDir.copyRecursively(newDir, overwrite = true)
            }

            val now = System.currentTimeMillis()
            val newInfo = source.copy(id = newId, name = newName, createdAt = now, modifiedAt = now)
            _documents.value = listOf(newInfo) + _documents.value
            saveIndex()
            newInfo
        }

    /**
     * Merges the documents identified by [ids] into a single new document named [newName].
     * Pages from each source document are concatenated in creation-date order.
     * The source documents are deleted after the merge.
     */
    suspend fun mergeDocuments(ids: List<String>, newName: String): LibraryDocumentInfo? =
        withContext(Dispatchers.IO) {
            if (ids.size < 2) return@withContext null

            val newId = (System.currentTimeMillis() + 2).toString()
            val newDir = File(libraryDir, newId).apply { mkdirs() }
            val newScannedDir = File(newDir, "scanned_pages").apply { mkdirs() }
            val newSourceDir = File(newDir, "sources").apply { mkdirs() }

            val allPages = mutableListOf<PageV2>()
            var coverBytes: ByteArray? = null

            // Sort by creation date so the oldest doc's pages come first
            val orderedIds = ids.mapNotNull { id -> _documents.value.find { it.id == id } }
                .sortedBy { it.createdAt }
                .map { it.id }

            for ((index, id) in orderedIds.withIndex()) {
                val docDir = File(libraryDir, id)
                if (!docDir.exists()) continue

                // Take cover from the first document
                if (index == 0) {
                    val coverFile = File(docDir, "cover.jpg")
                    if (coverFile.exists()) coverBytes = coverFile.readBytes()
                }

                // Copy processed images
                val scannedDir = File(docDir, "scanned_pages")
                scannedDir.listFiles()?.forEach { file ->
                    if (file.name != "document.json") {
                        file.copyTo(File(newScannedDir, file.name), overwrite = true)
                    }
                }

                // Copy source images
                val sourceDir = File(docDir, "sources")
                sourceDir.listFiles()?.forEach { file ->
                    file.copyTo(File(newSourceDir, file.name), overwrite = true)
                }

                // Read and concatenate page metadata
                val metadataFile = File(scannedDir, "document.json")
                if (metadataFile.exists()) {
                    runCatching {
                        val parsed = json.decodeFromString<DocumentMetadataV2>(metadataFile.readText())
                        allPages.addAll(parsed.pages)
                    }
                }
            }

            // Write merged document.json
            File(newScannedDir, "document.json")
                .writeText(json.encodeToString(DocumentMetadataV2.serializer(), DocumentMetadataV2(pages = allPages)))

            // Write cover
            if (coverBytes != null) {
                File(newDir, "cover.jpg").writeBytes(coverBytes)
            }

            val now = System.currentTimeMillis()
            val newInfo = LibraryDocumentInfo(
                id = newId,
                name = newName,
                createdAt = now,
                modifiedAt = now,
                pageCount = allPages.size,
            )

            // Remove old documents from disk and index, add merged
            val idsSet = orderedIds.toSet()
            orderedIds.forEach { File(libraryDir, it).deleteRecursively() }
            _documents.value = listOf(newInfo) + _documents.value.filter { it.id !in idsSet }
            saveIndex()
            newInfo
        }

    fun getCoverBytes(id: String): ByteArray? {
        val cover = File(File(libraryDir, id), "cover.jpg")
        return if (cover.exists()) runCatching { cover.readBytes() }.getOrNull() else null
    }

    fun getDocument(id: String): LibraryDocumentInfo? =
        _documents.value.find { it.id == id }

    /** Exports all documents as PDFs to a chosen target directory (for cloud backup). */
    fun getLibraryDir(): File = libraryDir

    // --- Private helpers ---

    private fun copySessionToDocDir(docDir: File) {
        val sessionScanned = File(filesDir, "scanned_pages")
        val sessionSources = File(filesDir, "sources")

        if (sessionScanned.exists()) {
            sessionScanned.copyRecursively(File(docDir, "scanned_pages"), overwrite = true)
        }
        if (sessionSources.exists()) {
            sessionSources.copyRecursively(File(docDir, "sources"), overwrite = true)
        }
    }
}
