package com.locksite.app.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.locksite.app.R
import com.locksite.app.domain.model.ProtectionStatus
import com.locksite.app.ui.components.CardGap
import com.locksite.app.ui.components.EmptyHint
import com.locksite.app.ui.components.GhostButton
import com.locksite.app.ui.components.GreetingHeader
import com.locksite.app.ui.components.PillButton
import com.locksite.app.ui.components.SoftCard
import com.locksite.app.ui.theme.LockSiteColors
import com.locksite.app.ui.theme.LockSiteTheme
import com.locksite.app.ui.theme.LockSiteTypography
import com.locksite.app.ui.theme.Radius
import com.locksite.app.ui.theme.Spacing

/**
 * Home screen: the status field is the single focal point (Von Restorff), the yellow
 * button is the only primary action, and the status is stated in words — not colour alone.
 *
 * When protection is not running, activating it *is* the next action, so it takes the
 * yellow slot and "add domain" drops to a ghost button. Only one yellow action per screen.
 */
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onAddDomain: () -> Unit,
    onOpenList: () -> Unit,
    onOpenSettings: () -> Unit,
    onActivateProtection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DashboardContent(
        state = state,
        onAddDomain = onAddDomain,
        onOpenList = onOpenList,
        onOpenSettings = onOpenSettings,
        onActivateProtection = onActivateProtection,
        modifier = modifier,
    )
}

@Composable
private fun DashboardContent(
    state: DashboardViewModel.UiState,
    onAddDomain: () -> Unit,
    onOpenList: () -> Unit,
    onOpenSettings: () -> Unit,
    onActivateProtection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val needsActivation = !state.protectionRunning

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LockSiteColors.Canvas)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenMargin, vertical = Spacing.l),
        verticalArrangement = Arrangement.spacedBy(CardGap),
    ) {
        GreetingHeader(
            greeting = stringResource(R.string.dashboard_greeting),
            lead = stringResource(R.string.dashboard_lead),
            actionIcon = Icons.Filled.Settings,
            actionDescription = stringResource(R.string.nav_settings),
            onAction = onOpenSettings,
        )

        StatusCard(
            status = state.status,
            statusDetail = state.statusDetail,
            loadError = state.loadError,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            SoftCard(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.dashboard_locked_count),
                    style = LockSiteTypography.bodyMedium,
                    color = LockSiteColors.InkMuted,
                )
                Spacer(Modifier.height(Spacing.s))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = state.lockedCount.toString(),
                        style = LockSiteTypography.displaySmall,
                        color = LockSiteColors.Ink,
                    )
                    Text(
                        text = stringResource(R.string.dashboard_locked_unit),
                        modifier = Modifier.padding(start = Spacing.xs, bottom = 6.dp),
                        style = LockSiteTypography.bodyMedium,
                        color = LockSiteColors.InkMuted,
                    )
                }
            }

            SoftCard(modifier = Modifier.weight(1f), color = LockSiteColors.Green) {
                Text(
                    text = stringResource(R.string.dashboard_last_added),
                    style = LockSiteTypography.bodyMedium,
                    color = LockSiteColors.GreenInk.copy(alpha = 0.75f),
                )
                Spacer(Modifier.height(Spacing.s))
                Text(
                    text = state.latestHostname ?: "—",
                    style = LockSiteTypography.titleMedium,
                    color = LockSiteColors.GreenInk,
                    maxLines = 2,
                )
            }
        }

        if (needsActivation) {
            PillButton(
                text = stringResource(R.string.dashboard_activate),
                onClick = onActivateProtection,
                // While the system dialog or the service is starting, a second tap would
                // only queue another start request.
                enabled = state.status != ProtectionStatus.PERMISSION_PENDING,
            )
        } else {
            PillButton(
                text = stringResource(R.string.dashboard_add_domain),
                onClick = onAddDomain,
            )
        }

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            GhostButton(
                text = if (needsActivation) {
                    stringResource(R.string.dashboard_add_domain)
                } else {
                    stringResource(R.string.dashboard_view_list)
                },
                onClick = if (needsActivation) onAddDomain else onOpenList,
            )
        }

        if (!state.loading && !state.loadError && state.lockedCount == 0) {
            EmptyHint(
                title = stringResource(R.string.list_empty_title),
                body = stringResource(R.string.dashboard_empty_hint),
            )
        }
    }
}

@Composable
private fun StatusCard(
    status: ProtectionStatus,
    statusDetail: String?,
    loadError: Boolean,
) {
    SoftCard(
        modifier = Modifier.fillMaxWidth(),
        color = LockSiteColors.Teal,
        shape = RoundedCornerShape(Radius.large),
    ) {
        Text(
            text = stringResource(R.string.dashboard_status_title),
            style = LockSiteTypography.bodyMedium,
            color = LockSiteColors.TealInk.copy(alpha = 0.75f),
        )
        Spacer(Modifier.height(Spacing.s))
        Text(
            text = stringResource(statusLabel(status)),
            style = LockSiteTypography.headlineMedium,
            color = LockSiteColors.TealInk,
        )
        Spacer(Modifier.height(Spacing.m))
        // A sentence, not a chip: the note explains the state and can wrap to two lines.
        Text(
            text = statusDetail ?: stringResource(statusNote(status)),
            style = LockSiteTypography.bodyMedium,
            color = LockSiteColors.TealInk.copy(alpha = 0.8f),
        )
        if (loadError) {
            Spacer(Modifier.height(Spacing.s))
            Text(
                text = stringResource(R.string.status_error),
                style = LockSiteTypography.bodyMedium,
                color = LockSiteColors.Danger,
            )
        }
    }
}

private fun statusLabel(status: ProtectionStatus): Int = when (status) {
    ProtectionStatus.NOT_CONFIGURED -> R.string.status_not_configured
    ProtectionStatus.PERMISSION_PENDING -> R.string.status_permission_pending
    ProtectionStatus.ACTIVE -> R.string.status_active
    ProtectionStatus.ERROR -> R.string.status_error
    ProtectionStatus.STOPPED -> R.string.status_stopped
    ProtectionStatus.CONFLICT_WITH_OTHER_VPN -> R.string.status_conflict
}

private fun statusNote(status: ProtectionStatus): Int = when (status) {
    ProtectionStatus.NOT_CONFIGURED -> R.string.dashboard_note_idle
    ProtectionStatus.PERMISSION_PENDING -> R.string.dashboard_note_permission
    ProtectionStatus.ACTIVE -> R.string.dashboard_note_active
    ProtectionStatus.ERROR -> R.string.dashboard_note_error
    ProtectionStatus.STOPPED -> R.string.dashboard_note_stopped
    ProtectionStatus.CONFLICT_WITH_OTHER_VPN -> R.string.dashboard_note_conflict
}

@Preview(showBackground = true)
@Composable
private fun DashboardPreview() {
    LockSiteTheme {
        DashboardContent(
            state = DashboardViewModel.UiState(
                status = ProtectionStatus.ACTIVE,
                lockedCount = 3,
                latestHostname = "instagram.com",
                loading = false,
            ),
            onAddDomain = {},
            onOpenList = {},
            onOpenSettings = {},
            onActivateProtection = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardInactivePreview() {
    LockSiteTheme {
        DashboardContent(
            state = DashboardViewModel.UiState(
                status = ProtectionStatus.NOT_CONFIGURED,
                lockedCount = 2,
                latestHostname = "x.com",
                loading = false,
            ),
            onAddDomain = {},
            onOpenList = {},
            onOpenSettings = {},
            onActivateProtection = {},
        )
    }
}
