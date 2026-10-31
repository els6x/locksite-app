package com.locksite.app.presentation.settings

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.locksite.app.BuildConfig
import com.locksite.app.R
import com.locksite.app.ui.components.CardGap
import com.locksite.app.ui.components.GhostButton
import com.locksite.app.ui.components.InfoCard
import com.locksite.app.ui.components.ScreenTitle
import com.locksite.app.ui.components.SoftCard
import com.locksite.app.ui.theme.LockSiteColors
import com.locksite.app.ui.theme.LockSiteTheme
import com.locksite.app.ui.theme.LockSiteTypography
import com.locksite.app.ui.theme.Spacing

/**
 * Settings (PRD section 7.1 F).
 *
 * No toggle to clear the blocklist exists here or anywhere else in the release build.
 * Diagnostics is a separate screen so this page stays a reference, not a control panel.
 */
@Composable
fun SettingsScreen(
    maskDomains: Boolean,
    onToggleMask: (Boolean) -> Unit,
    adGuardEnabled: Boolean,
    onToggleAdGuard: (Boolean) -> Unit,
    onOpenDiagnostics: () -> Unit,
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
        ScreenTitle(title = stringResource(R.string.settings_title))

        InfoCard(
            title = stringResource(R.string.settings_about_title),
            body = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
        )
        InfoCard(
            title = stringResource(R.string.settings_privacy_title),
            body = stringResource(R.string.settings_privacy_body),
        )
        ToggleCard(
            title = stringResource(R.string.settings_mask_title),
            body = stringResource(R.string.settings_mask_body),
            checked = maskDomains,
            onCheckedChange = onToggleMask,
        )
        ToggleCard(
            title = stringResource(R.string.settings_adguard_title),
            body = stringResource(R.string.settings_adguard_body),
            checked = adGuardEnabled,
            onCheckedChange = onToggleAdGuard,
        )
        InfoCard(
            title = stringResource(R.string.settings_recovery_title),
            body = stringResource(R.string.settings_recovery_body),
        )
        InfoCard(
            title = stringResource(R.string.settings_limit_title),
            body = stringResource(R.string.settings_limit_body),
        )

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            GhostButton(
                text = stringResource(R.string.settings_diagnostics_open),
                onClick = onOpenDiagnostics,
            )
        }

        Spacer(Modifier.height(Spacing.s))
    }
}

/** One setting row: a title, an explanation, and a switch. */
@Composable
private fun ToggleCard(
    title: String,
    body: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    SoftCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = LockSiteTypography.titleMedium,
                    color = LockSiteColors.Ink,
                )
                Text(
                    text = body,
                    modifier = Modifier.padding(top = Spacing.xs),
                    style = LockSiteTypography.bodyMedium,
                    color = LockSiteColors.InkMuted,
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = LockSiteColors.YellowInk,
                    checkedTrackColor = LockSiteColors.Yellow,
                    uncheckedThumbColor = LockSiteColors.InkMuted,
                    uncheckedTrackColor = LockSiteColors.SurfaceMuted,
                    uncheckedBorderColor = LockSiteColors.Outline,
                ),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsPreview() {
    LockSiteTheme {
        SettingsScreen(
            maskDomains = true,
            onToggleMask = {},
            adGuardEnabled = true,
            onToggleAdGuard = {},
            onOpenDiagnostics = {},
        )
    }
}
