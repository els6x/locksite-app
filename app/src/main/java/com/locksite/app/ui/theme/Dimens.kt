package com.locksite.app.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Spacing and shape contract. Every gap in the app must come from [Spacing];
 * a value outside this scale is a bug, not a preference.
 *
 * Rhythm rule: inside a group = 1 step (8), between groups = 2 steps (16–24),
 * between sections = 3 steps (32–48).
 */
object Spacing {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp

    /** Screen side margin. */
    val screenMargin = 20.dp

    /** Padding inside cards. */
    val cardPadding = 20.dp
}

object Radius {
    val control = 16.dp
    val card = 24.dp
    val pill = 100.dp
}

/** Minimum touch target (Fitts's law: >= 44dp, >= 8dp apart). */
val MinTouchTarget = 44.dp
