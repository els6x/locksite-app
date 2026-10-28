package com.locksite.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.locksite.app.ui.theme.LockSiteColors
import com.locksite.app.ui.theme.LockSiteTypography
import com.locksite.app.ui.theme.MinTouchTarget
import com.locksite.app.ui.theme.Radius
import com.locksite.app.ui.theme.Spacing

/**
 * Header used on the main screens: a round avatar placeholder, a two-line greeting and
 * one action on the right. Mirrors the reference layout.
 */
@Composable
fun GreetingHeader(
    greeting: String,
    lead: String,
    actionIcon: ImageVector,
    actionDescription: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(LockSiteColors.Teal, RoundedCornerShape(Radius.pill)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = greeting.take(1).uppercase(),
                style = LockSiteTypography.titleMedium,
                color = LockSiteColors.TealInk,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = greeting,
                style = LockSiteTypography.titleMedium,
                color = LockSiteColors.Ink,
            )
            Text(
                text = lead,
                style = LockSiteTypography.bodyMedium,
                color = LockSiteColors.InkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconCircle(
            icon = actionIcon,
            contentDescription = actionDescription,
            onClick = onAction,
        )
    }
}

/**
 * Header used on detail screens: back button, title, optional trailing action.
 */
@Composable
fun DetailHeader(
    title: String,
    onBack: () -> Unit,
    backDescription: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        Box(
            modifier = Modifier
                .size(MinTouchTarget)
                .background(LockSiteColors.Surface, RoundedCornerShape(Radius.pill))
                .clickable(role = Role.Button, onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = backDescription,
                tint = LockSiteColors.Ink,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = LockSiteTypography.titleLarge,
            color = LockSiteColors.Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        trailing?.invoke()
    }
}

/** Plain screen title without navigation affordances. */
@Composable
fun ScreenTitle(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = LockSiteTypography.headlineMedium,
            color = LockSiteColors.Ink,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                modifier = Modifier.padding(top = Spacing.xs),
                style = LockSiteTypography.bodyMedium,
                color = LockSiteColors.InkMuted,
            )
        }
    }
}

internal val HeaderTint = Color.Unspecified
