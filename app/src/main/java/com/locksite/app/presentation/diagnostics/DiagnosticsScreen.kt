package com.locksite.app.presentation.diagnostics

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.locksite.app.R
import com.locksite.app.diagnostics.DiagnosticsSelfTest
import com.locksite.app.domain.model.ProtectionStatus
import com.locksite.app.presentation.common.ProtectionStatusText
import com.locksite.app.presentation.common.formatCheckTime
import com.locksite.app.ui.components.CardGap
import com.locksite.app.ui.components.CenteredNote
import com.locksite.app.ui.components.DetailHeader
import com.locksite.app.ui.components.GhostButton
import com.locksite.app.ui.components.InfoCard
import com.locksite.app.ui.components.PillButton
import com.locksite.app.ui.components.SoftCard
import com.locksite.app.ui.theme.LockSiteColors
import com.locksite.app.ui.theme.LockSiteTheme
import com.locksite.app.ui.theme.LockSiteTypography
import com.locksite.app.ui.theme.Radius
import com.locksite.app.ui.theme.Spacing

/**
 * Diagnostics (PRD section 7.1 E, FR-031).
 *
 * Shows only facts the app can actually observe: service state, Android VPN consent,
 * whether another VPN owns the tunnel, the blocklist size, and the result of an in-process
 * filter self-test. No browsing history, no query names, no payloads.
 */
@Composable
fun DiagnosticsScreen(
    viewModel: DiagnosticsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Android-level facts (consent, another VPN) are one-shot reads, not flows. Re-read
    // them whenever the screen comes back to the foreground so it never shows stale truth.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DiagnosticsContent(
        state = state,
        onBack = onBack,
        onRunTest = viewModel::runSelfTest,
        onRefresh = viewModel::refresh,
        modifier = modifier,
    )
}

@Composable
private fun DiagnosticsContent(
    state: DiagnosticsViewModel.UiState,
    onBack: () -> Unit,
    onRunTest: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LockSiteColors.Canvas)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenMargin, vertical = Spacing.l),
        verticalArrangement = Arrangement.spacedBy(CardGap),
    ) {
        DetailHeader(
            title = stringResource(R.string.diagnostics_title),
            onBack = onBack,
            backDescription = stringResource(R.string.action_back),
        )

        StatusCard(state)

        FactsCard(state)

        TestCard(state = state, onRunTest = onRunTest)

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            GhostButton(
                text = stringResource(R.string.diagnostics_refresh),
                onClick = onRefresh,
            )
        }

        InfoCard(
            title = stringResource(R.string.diagnostics_recovery_title),
            body = stringResource(R.string.diagnostics_recovery_body),
        )

        val checkedAt = state.lastCheckedAtEpochMillis
        if (checkedAt != null) {
            CenteredNote(
                text = stringResource(
                    R.string.diagnostics_last_check,
                    formatCheckTime(checkedAt),
                ),
            )
        }
    }
}

@Composable
private fun StatusCard(state: DiagnosticsViewModel.UiState) {
    SoftCard(
        modifier = Modifier.fillMaxWidth(),
        color = LockSiteColors.Teal,
        shape = RoundedCornerShape(Radius.large),
    ) {
        Text(
            text = stringResource(R.string.diagnostics_service_title),
            style = LockSiteTypography.bodyMedium,
            color = LockSiteColors.TealInk.copy(alpha = 0.75f),
        )
        Spacer(Modifier.height(Spacing.s))
        Text(
            text = stringResource(ProtectionStatusText.label(state.serviceStatus)),
            style = LockSiteTypography.headlineMedium,
            color = LockSiteColors.TealInk,
        )
        Spacer(Modifier.height(Spacing.m))
        Text(
            text = state.statusDetail
                ?: stringResource(diagnosticsNote(state.serviceStatus)),
            style = LockSiteTypography.bodyMedium,
            color = LockSiteColors.TealInk.copy(alpha = 0.8f),
        )
    }
}

/**
 * Idle/stopped notes on the dashboard tell the user to activate protection, but this
 * screen has no activate control, so it points back to Home instead.
 */
private fun diagnosticsNote(status: ProtectionStatus): Int = when (status) {
    ProtectionStatus.NOT_CONFIGURED -> R.string.diagnostics_note_idle
    ProtectionStatus.STOPPED -> R.string.diagnostics_note_stopped
    else -> ProtectionStatusText.note(status)
}

@Composable
private fun FactsCard(state: DiagnosticsViewModel.UiState) {
    SoftCard(modifier = Modifier.fillMaxWidth()) {
        FactRow(
            label = stringResource(R.string.diagnostics_permission_title),
            value = when (state.permissionGranted) {
                true -> stringResource(R.string.diagnostics_permission_granted)
                false -> stringResource(R.string.diagnostics_permission_missing)
                null -> stringResource(R.string.diagnostics_unknown)
            },
            warn = state.permissionGranted == false,
        )
        FactRow(
            label = stringResource(R.string.diagnostics_foreign_vpn_title),
            value = when (state.foreignVpnActive) {
                true -> stringResource(R.string.diagnostics_foreign_vpn_present)
                false -> stringResource(R.string.diagnostics_foreign_vpn_none)
                null -> stringResource(R.string.diagnostics_unknown)
            },
            warn = state.foreignVpnActive == true,
        )
        FactRow(
            label = stringResource(R.string.diagnostics_locked_count_title),
            value = state.lockedCount?.toString()
                ?: stringResource(R.string.diagnostics_unknown),
            warn = state.lockedCount == null,
        )
    }
}

@Composable
private fun FactRow(
    label: String,
    value: String,
    warn: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = LockSiteTypography.bodyMedium,
            color = LockSiteColors.InkMuted,
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = LockSiteTypography.bodyMedium,
            color = if (warn) LockSiteColors.Danger else LockSiteColors.Ink,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun TestCard(
    state: DiagnosticsViewModel.UiState,
    onRunTest: () -> Unit,
) {
    val finished = state.test as? DiagnosticsViewModel.TestState.Finished
    val failed = state.test is DiagnosticsViewModel.TestState.Failed
    // Nothing was blocked-checked when the blocklist is empty; a green "Lulus" there would
    // claim a verification that did not happen.
    val skipped = finished?.result?.blockCheckSkipped == true
    val passed = finished?.result?.passed == true

    val cardColor = when {
        failed -> LockSiteColors.DangerSurface
        finished == null -> LockSiteColors.Surface
        skipped -> LockSiteColors.Surface
        passed -> LockSiteColors.Green
        else -> LockSiteColors.DangerSurface
    }
    val ink = when {
        failed -> LockSiteColors.Danger
        finished == null -> LockSiteColors.Ink
        skipped -> LockSiteColors.Ink
        passed -> LockSiteColors.GreenInk
        else -> LockSiteColors.Danger
    }
    val mutedInk = ink.copy(alpha = 0.78f)

    SoftCard(modifier = Modifier.fillMaxWidth(), color = cardColor) {
        Text(
            text = stringResource(R.string.diagnostics_test_title),
            style = LockSiteTypography.titleMedium,
            color = ink,
        )
        Spacer(Modifier.height(Spacing.s))
        Text(
            text = stringResource(R.string.diagnostics_test_body),
            style = LockSiteTypography.bodyMedium,
            color = mutedInk,
        )

        when (val test = state.test) {
            DiagnosticsViewModel.TestState.NotRun -> Unit

            DiagnosticsViewModel.TestState.Running -> {
                Spacer(Modifier.height(Spacing.s))
                Text(
                    text = stringResource(R.string.diagnostics_test_running),
                    style = LockSiteTypography.bodyMedium,
                    color = mutedInk,
                )
            }

            is DiagnosticsViewModel.TestState.Failed -> {
                Spacer(Modifier.height(Spacing.s))
                Text(
                    text = stringResource(R.string.diagnostics_test_blocklist_error),
                    style = LockSiteTypography.bodyMedium,
                    color = LockSiteColors.Danger,
                )
            }

            is DiagnosticsViewModel.TestState.Finished -> {
                Spacer(Modifier.height(Spacing.s))
                val result = test.result
                CheckLine(
                    text = if (result.blockedAsExpected == null) {
                        stringResource(R.string.diagnostics_test_no_rules)
                    } else {
                        stringResource(R.string.diagnostics_test_block_check)
                    },
                    ok = result.blockedAsExpected,
                    mutedInk = mutedInk,
                )
                CheckLine(
                    text = stringResource(R.string.diagnostics_test_allow_check),
                    ok = result.allowedAsExpected,
                    mutedInk = mutedInk,
                )
            }
        }

        Spacer(Modifier.height(Spacing.m))
        PillButton(
            text = stringResource(R.string.diagnostics_test_run),
            onClick = onRunTest,
            enabled = state.test != DiagnosticsViewModel.TestState.Running,
        )
    }
}

@Composable
private fun CheckLine(
    text: String,
    ok: Boolean?,
    mutedInk: Color,
) {
    // The word itself states the outcome, so the result never depends on colour alone.
    val marker = when (ok) {
        true -> stringResource(R.string.diagnostics_check_ok)
        false -> stringResource(R.string.diagnostics_check_failed)
        null -> stringResource(R.string.diagnostics_check_skipped)
    }
    Text(
        text = stringResource(R.string.diagnostics_check_line, marker, text),
        style = LockSiteTypography.bodyMedium,
        color = if (ok == false) LockSiteColors.Danger else mutedInk,
    )
}

@Preview(showBackground = true)
@Composable
private fun DiagnosticsPreview() {
    LockSiteTheme {
        DiagnosticsContent(
            state = DiagnosticsViewModel.UiState(
                serviceStatus = ProtectionStatus.ACTIVE,
                permissionGranted = true,
                foreignVpnActive = false,
                lockedCount = 3,
                test = DiagnosticsViewModel.TestState.Finished(
                    DiagnosticsSelfTest.Result(
                        lockedProbe = "instagram.com",
                        blockedAsExpected = true,
                        allowedProbe = "probe-0.invalid",
                        allowedAsExpected = true,
                        ruleCount = 3,
                    ),
                ),
                lastCheckedAtEpochMillis = 0,
            ),
            onBack = {},
            onRunTest = {},
            onRefresh = {},
        )
    }
}
