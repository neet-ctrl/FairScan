/*
 * Copyright 2025-2026 The FairScan authors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 */
package org.fairscan.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

enum class AppTheme { SYSTEM, LIGHT, DARK }

enum class AccentColor { MINT, BLUE, PURPLE, AMBER, ROSE, SLATE }

private val FairScanShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

private fun buildLightScheme(accent: AccentColor): ColorScheme = when (accent) {
    AccentColor.MINT -> lightColorScheme(
        primary = Primary, onPrimary = OnPrimary,
        primaryContainer = PrimaryContainer, onPrimaryContainer = OnPrimaryContainer,
        secondary = Secondary, onSecondary = OnSecondary,
        secondaryContainer = SecondaryContainer, onSecondaryContainer = OnSecondaryContainer,
        tertiary = Tertiary, onTertiary = OnTertiary,
        tertiaryContainer = TertiaryContainer, onTertiaryContainer = OnTertiaryContainer,
        error = Error, onError = OnError,
        errorContainer = ErrorContainer, onErrorContainer = OnErrorContainer,
        background = Background, onBackground = OnBackground,
        surface = Surface, onSurface = OnSurface,
        surfaceVariant = SurfaceVariant, onSurfaceVariant = OnSurfaceVariant,
        outline = Outline,
    )
    AccentColor.BLUE -> lightColorScheme(
        primary = BluePrimary, onPrimary = BlueOnPrimary,
        primaryContainer = BluePrimaryContainer, onPrimaryContainer = BlueOnPrimaryContainer,
        error = Error, onError = OnError,
        errorContainer = ErrorContainer, onErrorContainer = OnErrorContainer,
        background = Background, onBackground = OnBackground,
        surface = Surface, onSurface = OnSurface,
        surfaceVariant = SurfaceVariant, onSurfaceVariant = OnSurfaceVariant,
        outline = Outline,
    )
    AccentColor.PURPLE -> lightColorScheme(
        primary = PurplePrimary, onPrimary = PurpleOnPrimary,
        primaryContainer = PurplePrimaryContainer, onPrimaryContainer = PurpleOnPrimaryContainer,
        error = Error, onError = OnError,
        errorContainer = ErrorContainer, onErrorContainer = OnErrorContainer,
        background = Background, onBackground = OnBackground,
        surface = Surface, onSurface = OnSurface,
        surfaceVariant = SurfaceVariant, onSurfaceVariant = OnSurfaceVariant,
        outline = Outline,
    )
    AccentColor.AMBER -> lightColorScheme(
        primary = AmberPrimary, onPrimary = AmberOnPrimary,
        primaryContainer = AmberPrimaryContainer, onPrimaryContainer = AmberOnPrimaryContainer,
        error = Error, onError = OnError,
        errorContainer = ErrorContainer, onErrorContainer = OnErrorContainer,
        background = Background, onBackground = OnBackground,
        surface = Surface, onSurface = OnSurface,
        surfaceVariant = SurfaceVariant, onSurfaceVariant = OnSurfaceVariant,
        outline = Outline,
    )
    AccentColor.ROSE -> lightColorScheme(
        primary = RosePrimary, onPrimary = RoseOnPrimary,
        primaryContainer = RosePrimaryContainer, onPrimaryContainer = RoseOnPrimaryContainer,
        error = Error, onError = OnError,
        errorContainer = ErrorContainer, onErrorContainer = OnErrorContainer,
        background = Background, onBackground = OnBackground,
        surface = Surface, onSurface = OnSurface,
        surfaceVariant = SurfaceVariant, onSurfaceVariant = OnSurfaceVariant,
        outline = Outline,
    )
    AccentColor.SLATE -> lightColorScheme(
        primary = SlatePrimary, onPrimary = SlateOnPrimary,
        primaryContainer = SlatePrimaryContainer, onPrimaryContainer = SlateOnPrimaryContainer,
        error = Error, onError = OnError,
        errorContainer = ErrorContainer, onErrorContainer = OnErrorContainer,
        background = Background, onBackground = OnBackground,
        surface = Surface, onSurface = OnSurface,
        surfaceVariant = SurfaceVariant, onSurfaceVariant = OnSurfaceVariant,
        outline = Outline,
    )
}

private fun buildDarkScheme(accent: AccentColor): ColorScheme = when (accent) {
    AccentColor.MINT -> darkColorScheme(
        primary = PrimaryDark, onPrimary = OnPrimaryDark,
        primaryContainer = PrimaryContainerDark, onPrimaryContainer = OnPrimaryContainerDark,
        secondary = SecondaryDark, onSecondary = OnSecondaryDark,
        secondaryContainer = SecondaryContainerDark, onSecondaryContainer = OnSecondaryContainerDark,
        tertiary = TertiaryDark, onTertiary = OnTertiaryDark,
        tertiaryContainer = TertiaryContainerDark, onTertiaryContainer = OnTertiaryContainerDark,
        error = ErrorDark, onError = OnErrorDark,
        errorContainer = ErrorContainerDark, onErrorContainer = OnErrorContainerDark,
        background = BackgroundDark, onBackground = OnBackgroundDark,
        surface = SurfaceDark, onSurface = OnSurfaceDark,
        surfaceVariant = SurfaceVariantDark, onSurfaceVariant = OnSurfaceVariantDark,
        outline = OutlineDark,
    )
    AccentColor.BLUE -> darkColorScheme(
        primary = BluePrimaryDark, onPrimary = BlueOnPrimaryDark,
        primaryContainer = BluePrimaryContainerDark, onPrimaryContainer = BlueOnPrimaryContainerDark,
        error = ErrorDark, onError = OnErrorDark,
        errorContainer = ErrorContainerDark, onErrorContainer = OnErrorContainerDark,
        background = BackgroundDark, onBackground = OnBackgroundDark,
        surface = SurfaceDark, onSurface = OnSurfaceDark,
        surfaceVariant = SurfaceVariantDark, onSurfaceVariant = OnSurfaceVariantDark,
        outline = OutlineDark,
    )
    AccentColor.PURPLE -> darkColorScheme(
        primary = PurplePrimaryDark, onPrimary = PurpleOnPrimaryDark,
        primaryContainer = PurplePrimaryContainerDark, onPrimaryContainer = PurpleOnPrimaryContainerDark,
        error = ErrorDark, onError = OnErrorDark,
        errorContainer = ErrorContainerDark, onErrorContainer = OnErrorContainerDark,
        background = BackgroundDark, onBackground = OnBackgroundDark,
        surface = SurfaceDark, onSurface = OnSurfaceDark,
        surfaceVariant = SurfaceVariantDark, onSurfaceVariant = OnSurfaceVariantDark,
        outline = OutlineDark,
    )
    AccentColor.AMBER -> darkColorScheme(
        primary = AmberPrimaryDark, onPrimary = AmberOnPrimaryDark,
        primaryContainer = AmberPrimaryContainerDark, onPrimaryContainer = AmberOnPrimaryContainerDark,
        error = ErrorDark, onError = OnErrorDark,
        errorContainer = ErrorContainerDark, onErrorContainer = OnErrorContainerDark,
        background = BackgroundDark, onBackground = OnBackgroundDark,
        surface = SurfaceDark, onSurface = OnSurfaceDark,
        surfaceVariant = SurfaceVariantDark, onSurfaceVariant = OnSurfaceVariantDark,
        outline = OutlineDark,
    )
    AccentColor.ROSE -> darkColorScheme(
        primary = RosePrimaryDark, onPrimary = RoseOnPrimaryDark,
        primaryContainer = RosePrimaryContainerDark, onPrimaryContainer = RoseOnPrimaryContainerDark,
        error = ErrorDark, onError = OnErrorDark,
        errorContainer = ErrorContainerDark, onErrorContainer = OnErrorContainerDark,
        background = BackgroundDark, onBackground = OnBackgroundDark,
        surface = SurfaceDark, onSurface = OnSurfaceDark,
        surfaceVariant = SurfaceVariantDark, onSurfaceVariant = OnSurfaceVariantDark,
        outline = OutlineDark,
    )
    AccentColor.SLATE -> darkColorScheme(
        primary = SlatePrimaryDark, onPrimary = SlateOnPrimaryDark,
        primaryContainer = SlatePrimaryContainerDark, onPrimaryContainer = SlateOnPrimaryContainerDark,
        error = ErrorDark, onError = OnErrorDark,
        errorContainer = ErrorContainerDark, onErrorContainer = OnErrorContainerDark,
        background = BackgroundDark, onBackground = OnBackgroundDark,
        surface = SurfaceDark, onSurface = OnSurfaceDark,
        surfaceVariant = SurfaceVariantDark, onSurfaceVariant = OnSurfaceVariantDark,
        outline = OutlineDark,
    )
}

@Composable
fun FairScanTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    accentColor: AccentColor = AccentColor.MINT,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (appTheme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }
    val colorScheme = if (darkTheme) buildDarkScheme(accentColor) else buildLightScheme(accentColor)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = FairScanShapes,
        content = content,
    )
}
