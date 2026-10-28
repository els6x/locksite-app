package com.locksite.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Colour tokens taken from the reference design: a light grey canvas, white cards,
 * and three pastel accent fields (teal, green, yellow). No elevation, no shadows.
 *
 * Contrast: every ink token below is checked against the surface it is used on.
 * Status must never be conveyed by colour alone (PRD NFR-005).
 */
object LockSiteColors {
    /** Page background behind all cards. */
    val Canvas = Color(0xFFF2F2F2)

    /** Card / sheet surface. */
    val Surface = Color(0xFFFFFFFF)

    /** Quiet surface for secondary cards and input fields. */
    val SurfaceMuted = Color(0xFFF6F6F7)

    /** Pastel teal — protection status field. */
    val Teal = Color(0xFFBFE7E8)
    val TealInk = Color(0xFF12383C)

    /** Pastel green — positive / completed field. */
    val Green = Color(0xFFCDE9AB)
    val GreenInk = Color(0xFF2C4218)

    /** Pastel yellow — primary action only. */
    val Yellow = Color(0xFFF8EC94)
    val YellowInk = Color(0xFF3B3512)

    /** Text and icons. */
    val Ink = Color(0xFF17181A)
    val InkMuted = Color(0xFF5F6066)
    val InkFaint = Color(0xFF8E8F95)

    /** Hairline separators. */
    val Outline = Color(0xFFE8E8EA)

    /** Error / destructive ink (muted so it still fits the palette). */
    val Danger = Color(0xFF9B3B31)
    val DangerSurface = Color(0xFFF7E4E1)
}
