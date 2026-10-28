package com.locksite.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.locksite.app.ui.theme.LockSiteColors
import com.locksite.app.ui.theme.LockSiteTypography
import com.locksite.app.ui.theme.Radius
import com.locksite.app.ui.theme.Spacing

/**
 * Flat surface used for every block in the app.
 *
 * No shadow, no elevation, no border by default: grouping is carried by the colour field
 * and the gap between cards (proximity ratio >= 2x inside vs between).
 */
@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    color: Color = LockSiteColors.Surface,
    shape: RoundedCornerShape = RoundedCornerShape(Radius.card),
    contentPadding: androidx.compose.foundation.layout.PaddingValues =
        androidx.compose.foundation.layout.PaddingValues(Spacing.cardPadding),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .background(color = color, shape = shape)
            .padding(contentPadding),
        content = content,
    )
}

@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LockSiteColors.InkMuted,
) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        style = LockSiteTypography.labelMedium,
        color = color,
    )
}

@Composable
fun EmptyHint(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    SoftCard(modifier = modifier.fillMaxWidth(), color = LockSiteColors.Surface) {
        Text(
            text = title,
            style = LockSiteTypography.titleMedium,
            color = LockSiteColors.Ink,
        )
        Text(
            text = body,
            modifier = Modifier.padding(top = Spacing.s),
            style = LockSiteTypography.bodyMedium,
            color = LockSiteColors.InkMuted,
        )
    }
}

/** Title + explanatory paragraph on a flat card. Used by Settings and Diagnostics. */
@Composable
fun InfoCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    SoftCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = LockSiteTypography.titleMedium,
            color = LockSiteColors.Ink,
        )
        Text(
            text = body,
            modifier = Modifier.padding(top = Spacing.s),
            style = LockSiteTypography.bodyMedium,
            color = LockSiteColors.InkMuted,
        )
    }
}

@Composable
fun CenteredNote(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.s),
        style = LockSiteTypography.bodyMedium,
        color = LockSiteColors.InkMuted,
        textAlign = TextAlign.Center,
    )
}

/** Standard vertical gap between cards (2 steps = 16dp). */
val CardGap = Spacing.l

/** Standard gap between sections (3 steps = 32dp). */
val SectionGap = Spacing.xxl

/** Small inset used inside a card between a label and its value. */
val InnerGap = Spacing.s

internal val NoElevation = 0.dp
