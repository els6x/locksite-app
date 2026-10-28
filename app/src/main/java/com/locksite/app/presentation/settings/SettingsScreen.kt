package com.locksite.app.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.locksite.app.ui.theme.LockSiteColors
import com.locksite.app.ui.theme.LockSiteTheme
import com.locksite.app.ui.theme.Spacing

/**
 * Settings (PRD section 7.1 F).
 *
 * No toggle to clear the blocklist exists here or anywhere else in the release build.
 * Diagnostics is a separate screen so this page stays a reference, not a control panel.
 */
@Composable
fun SettingsScreen(
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

@Preview(showBackground = true)
@Composable
private fun SettingsPreview() {
    LockSiteTheme { SettingsScreen(onOpenDiagnostics = {}) }
}
