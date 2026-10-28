package com.locksite.app.presentation.onboarding

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.locksite.app.R
import com.locksite.app.ui.components.CardGap
import com.locksite.app.ui.components.CenteredNote
import com.locksite.app.ui.components.PillButton
import com.locksite.app.ui.components.SoftCard
import com.locksite.app.ui.theme.LockSiteColors
import com.locksite.app.ui.theme.LockSiteTheme
import com.locksite.app.ui.theme.LockSiteTypography
import com.locksite.app.ui.theme.Radius
import com.locksite.app.ui.theme.Spacing

/**
 * First-run explanation (PRD FR-001).
 *
 * The three facts are one group, so they live in one card separated by hairlines rather
 * than three identical cards (Common Region + Prägnanz).
 */
@Composable
fun OnboardingScreen(
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LockSiteColors.Canvas)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenMargin, vertical = Spacing.xl),
    ) {
        Spacer(Modifier.height(Spacing.xxl))

        Text(
            text = stringResource(R.string.onboarding_headline),
            style = LockSiteTypography.displaySmall,
            color = LockSiteColors.Ink,
        )
        Spacer(Modifier.height(Spacing.m))
        Text(
            text = stringResource(R.string.onboarding_sub),
            style = LockSiteTypography.bodyLarge,
            color = LockSiteColors.InkMuted,
        )

        Spacer(Modifier.height(Spacing.xxl))

        SoftCard(modifier = Modifier.fillMaxWidth()) {
            OnboardingPoint(
                number = "1",
                title = stringResource(R.string.onboarding_point_1_title),
                body = stringResource(R.string.onboarding_point_1_body),
            )
            Hairline()
            OnboardingPoint(
                number = "2",
                title = stringResource(R.string.onboarding_point_2_title),
                body = stringResource(R.string.onboarding_point_2_body),
            )
            Hairline()
            OnboardingPoint(
                number = "3",
                title = stringResource(R.string.onboarding_point_3_title),
                body = stringResource(R.string.onboarding_point_3_body),
            )
        }

        Spacer(Modifier.height(Spacing.xxl))

        PillButton(text = stringResource(R.string.onboarding_cta), onClick = onStart)

        Spacer(Modifier.height(Spacing.m))
        CenteredNote(stringResource(R.string.onboarding_footnote))
        Spacer(Modifier.height(Spacing.xl))
    }
}

@Composable
private fun OnboardingPoint(
    number: String,
    title: String,
    body: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(LockSiteColors.Teal, RoundedCornerShape(Radius.pill)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number,
                style = LockSiteTypography.labelMedium,
                color = LockSiteColors.TealInk,
            )
        }
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
    }
}

@Composable
private fun Hairline() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.m)
            .height(1.dp)
            .background(LockSiteColors.Outline),
    )
}

@Preview(showBackground = true)
@Composable
private fun OnboardingPreview() {
    LockSiteTheme { OnboardingScreen(onStart = {}) }
}
