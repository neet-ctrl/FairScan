/*
 * Copyright 2025-2026 The FairScan authors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 */
package org.fairscan.app.data

import kotlinx.serialization.Serializable

@Serializable
data class LibraryDocumentInfo(
    val id: String,
    val name: String,
    val createdAt: Long,
    val modifiedAt: Long,
    val pageCount: Int,
)

@Serializable
data class LibraryIndex(
    val documents: List<LibraryDocumentInfo> = emptyList()
)

data class PageSnapshot(
    val pageId: String,
    val manualRotationDegrees: Int,
)
