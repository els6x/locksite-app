package com.locksite.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.locksite.app.ui.theme.LockSiteColors
import com.locksite.app.ui.theme.LockSiteTypography
import com.locksite.app.ui.theme.MinTouchTarget
import com.locksite.app.ui.theme.Radius
import com.locksite.app.ui.theme.Spacing

/**
 * Primary action. Yellow is reserved for exactly one element per screen
 * (Von Restorff: one focal point, not a decoration).
 */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = LockSiteColors.Yellow,
    contentColor: Color = LockSiteColors.YellowInk,
    enabled: Boolean = true,
) {
    val alpha = if (enabled) 1f else 0.45f
    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .background(containerColor.copy(alpha = alpha), RoundedCornerShape(Radius.pill))
            // Clip before clickable so the ripple is bounded by the pill, not a grey rectangle.
            .clip(RoundedCornerShape(Radius.pill))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.xl, vertical = Spacing.l),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = LockSiteTypography.labelLarge,
            color = contentColor.copy(alpha = alpha),
        )
    }
}

/** Secondary action: white surface, hairline outline. */
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = MinTouchTarget)
            .background(LockSiteColors.Surface, RoundedCornerShape(Radius.pill))
            .clip(RoundedCornerShape(Radius.pill))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.l, vertical = Spacing.m),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = LockSiteTypography.labelMedium,
            color = LockSiteColors.Ink,
        )
    }
}

/** Small status chip: text + optional icon, never colour alone. */
@Composable
fun PillChip(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = LockSiteColors.SurfaceMuted,
    contentColor: Color = LockSiteColors.InkMuted,
    icon: ImageVector? = null,
) {
    Row(
        modifier = modifier
            .background(containerColor, RoundedCornerShape(Radius.pill))
            .padding(horizontal = Spacing.m, vertical = Spacing.xs + 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(text = text, style = LockSiteTypography.labelSmall, color = contentColor)
    }
}

/** Circular icon button used in headers and the bottom bar. */
@Composable
fun IconCircle(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = LockSiteColors.Surface,
    contentColor: Color = LockSiteColors.Ink,
) {
    Box(
        modifier = modifier
            .size(MinTouchTarget)
            .background(containerColor, RoundedCornerShape(Radius.pill))
            .clip(RoundedCornerShape(Radius.pill))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(20.dp),
        )
    }
}
