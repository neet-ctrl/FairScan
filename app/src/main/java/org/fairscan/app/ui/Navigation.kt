/*
 * Copyright 2025-2026 The FairScan authors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 */
package org.fairscan.app.ui

sealed class Screen {
    sealed class Main : Screen() {
        object Library : Main()
        object Onboarding : Main()
        object Camera : Main()
        object EditImage : Main()
        data class Document(val initialPage: Int = 0) : Main()
        object Export : Main()
        object ResumeScan : Main()
    }
    sealed class Overlay : Screen() {
        object About : Overlay()
        object Libraries : Overlay()
        object Settings : Overlay()
        object OcrLanguages : Overlay()
        object CloudBackup : Overlay()
    }
}

data class Navigation(
    val toCameraScreen: () -> Unit,
    val toEditImageScreen: () -> Unit,
    val toDocumentScreen: () -> Unit,
    val toExportScreen: () -> Unit,
    val toLibraryScreen: () -> Unit,
    val toAboutScreen: () -> Unit,
    val toLibrariesScreen: () -> Unit,
    val toSettingsScreen: (() -> Unit)?,
    val toOcrLanguagesScreen: () -> Unit,
    val toCloudBackupScreen: () -> Unit,
    val back: () -> Unit,
    val shouldDisplayBackButton: () -> Boolean,
)

@ConsistentCopyVisibility
data class NavigationState private constructor(val stack: List<Screen>, val root: Screen.Main) {

    companion object {
        fun initial(root: Screen.Main = Screen.Main.Library): NavigationState {
            return NavigationState(listOf(root), root)
        }
    }

    val current: Screen get() = stack.last()

    fun navigateTo(destination: Screen): NavigationState {
        return if (destination is Screen.Overlay) {
            copy(stack = stack + destination)
        } else {
            copy(stack = listOf(destination), root = if (destination is Screen.Main) destination else root)
        }
    }

    fun navigateBack(): NavigationState {
        // Explicit screen cases must come BEFORE `root -> this` because navigateTo()
        // sets `root` to the destination, so Camera/Document/etc. would otherwise
        // match `root -> this` and silently do nothing.
        return when (current) {
            is Screen.Main.ResumeScan -> copy(stack = listOf(Screen.Main.Library))
            is Screen.Main.Onboarding -> this // Back handled by system
            is Screen.Main.Camera -> copy(stack = listOf(Screen.Main.Library))
            is Screen.Main.Document -> copy(stack = listOf(Screen.Main.Library))
            is Screen.Main.EditImage -> copy(stack = listOf(Screen.Main.Document()))
            is Screen.Main.Export -> copy(stack = listOf(Screen.Main.Library))
            is Screen.Overlay -> copy(stack = stack.dropLast(1))
            root -> this // Library root: back handled by system (exits app)
            else -> this
        }
    }
}
