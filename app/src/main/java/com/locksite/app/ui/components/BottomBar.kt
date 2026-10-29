package com.locksite.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.locksite.app.ui.theme.LockSiteColors
import com.locksite.app.ui.theme.Radius
import com.locksite.app.ui.theme.Spacing

enum class BottomDestination(val label: String, val icon: ImageVector) {
    HOME("Beranda", Icons.Filled.Home),
    LIST("Daftar", Icons.AutoMirrored.Filled.List),
    SETTINGS("Pengaturan", Icons.Filled.Settings),
}

/**
 * Three destinations only (Hick: fewer choices, faster decisions), 44dp targets,
 * selected state carried by a filled yellow pill plus the accessibility state.
 */
@Composable
fun LockSiteBottomBar(
    current: BottomDestination,
    onSelect: (BottomDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(LockSiteColors.Surface, RoundedCornerShape(Radius.pill))
            .padding(horizontal = Spacing.s, vertical = Spacing.s),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BottomDestination.entries.forEach { destination ->
            val selected = destination == current
            // A short colour transition keeps the selected pill from snapping.
            val pillColor by animateColorAsState(
                targetValue = if (selected) LockSiteColors.Yellow else LockSiteColors.Surface,
                animationSpec = tween(durationMillis = 180),
                label = "bottomBarPill",
            )
            val tint by animateColorAsState(
                targetValue = if (selected) LockSiteColors.YellowInk else LockSiteColors.InkMuted,
                animationSpec = tween(durationMillis = 180),
                label = "bottomBarTint",
            )
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = pillColor,
                        shape = RoundedCornerShape(Radius.pill),
                    )
                    // Keep the ripple inside the round target.
                    .clip(RoundedCornerShape(Radius.pill))
                    .clickable(role = Role.Tab, onClick = { onSelect(destination) })
                    .semantics { contentDescription = destination.label },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = destination.icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
