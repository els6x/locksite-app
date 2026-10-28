package com.locksite.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Single light scheme that matches the reference design.
 *
 * Dark mode is intentionally not implemented in M1: the palette is a light pastel
 * system and inventing a dark variant would need its own contrast pass. Tracked as a
 * known limitation in docs/DESIGN_SYSTEM.md.
 */
private val LockSiteColorScheme = lightColorScheme(
    primary = LockSiteColors.Yellow,
    onPrimary = LockSiteColors.YellowInk,
    secondary = LockSiteColors.Teal,
    onSecondary = LockSiteColors.TealInk,
    tertiary = LockSiteColors.Green,
    onTertiary = LockSiteColors.GreenInk,
    background = LockSiteColors.Canvas,
    onBackground = LockSiteColors.Ink,
    surface = LockSiteColors.Surface,
    onSurface = LockSiteColors.Ink,
    surfaceVariant = LockSiteColors.SurfaceMuted,
    onSurfaceVariant = LockSiteColors.InkMuted,
    outline = LockSiteColors.Outline,
    outlineVariant = LockSiteColors.Outline,
    error = LockSiteColors.Danger,
    onError = LockSiteColors.Surface,
    errorContainer = LockSiteColors.DangerSurface,
    onErrorContainer = LockSiteColors.Danger,
)

@Composable
fun LockSiteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LockSiteColorScheme,
        typography = LockSiteTypography,
        content = content,
    )
}
