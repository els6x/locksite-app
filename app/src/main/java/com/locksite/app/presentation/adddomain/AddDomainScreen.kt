package com.locksite.app.presentation.adddomain

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.locksite.app.R
import com.locksite.app.ui.components.CardGap
import com.locksite.app.ui.components.DetailHeader
import com.locksite.app.ui.components.FlatTextField
import com.locksite.app.ui.components.GhostButton
import com.locksite.app.ui.components.PillButton
import com.locksite.app.ui.components.SectionLabel
import com.locksite.app.ui.components.SoftCard
import com.locksite.app.ui.theme.LockSiteColors
import com.locksite.app.ui.theme.LockSiteTheme
import com.locksite.app.ui.theme.LockSiteTypography
import com.locksite.app.ui.theme.Radius
import com.locksite.app.ui.theme.Spacing

/**
 * Add-domain screen (PRD FR-010..FR-012).
 *
 * Two explicit steps: the form never locks anything on its own, and the confirmation
 * dialog states the consequence before the irreversible action.
 */
@Composable
fun AddDomainScreen(
    viewModel: AddDomainViewModel,
    onBack: () -> Unit,
    onOpenList: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LockSiteColors.Canvas)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenMargin, vertical = Spacing.l),
    ) {
        DetailHeader(
            title = stringResource(R.string.add_title),
            onBack = onBack,
            backDescription = stringResource(R.string.action_back),
        )

        Spacer(Modifier.height(Spacing.xl))

        val saved = state.savedHostname
        if (saved != null) {
            SavedConfirmation(
                hostname = saved,
                onAddAnother = viewModel::onAddAnother,
                onOpenList = onOpenList,
            )
        } else {
            AddDomainForm(
                state = state,
                onInputChange = viewModel::onInputChange,
                onSubmit = viewModel::onSubmit,
            )
        }
    }

    val pending = state.pendingConfirmation
    if (pending != null) {
        PermanentLockDialog(
            hostname = pending,
            onConfirm = viewModel::onConfirmLock,
            onDismiss = viewModel::onDismissConfirmation,
        )
    }
}

@Composable
private fun AddDomainForm(
    state: AddDomainViewModel.UiState,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CardGap)) {
        Column {
            SectionLabel(text = stringResource(R.string.add_field_label))
            Spacer(Modifier.height(Spacing.s))
            FlatTextField(
                value = state.input,
                onValueChange = onInputChange,
                placeholder = stringResource(R.string.add_field_placeholder),
                isError = state.errorMessage != null,
                onImeAction = onSubmit,
            )
            val error = state.errorMessage
            if (error != null) {
                Text(
                    text = stringResource(error),
                    modifier = Modifier.padding(top = Spacing.s),
                    style = LockSiteTypography.bodyMedium,
                    color = LockSiteColors.Danger,
                )
            }
        }

        SoftCard(modifier = Modifier.fillMaxWidth(), color = LockSiteColors.Surface) {
            SectionLabel(text = stringResource(R.string.add_scope_title))
            Spacer(Modifier.height(Spacing.s))
            Text(
                text = stringResource(R.string.add_scope_body),
                style = LockSiteTypography.bodyMedium,
                color = LockSiteColors.InkMuted,
            )
        }

        PillButton(
            text = stringResource(R.string.add_lock_button),
            onClick = onSubmit,
            enabled = state.input.isNotBlank() && !state.saving,
        )
    }
}

@Composable
private fun SavedConfirmation(
    hostname: String,
    onAddAnother: () -> Unit,
    onOpenList: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CardGap)) {
        SoftCard(modifier = Modifier.fillMaxWidth(), color = LockSiteColors.Green) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(LockSiteColors.Surface.copy(alpha = 0.7f), RoundedCornerShape(Radius.pill)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = LockSiteColors.GreenInk,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.height(Spacing.m))
            Text(
                text = stringResource(R.string.add_success, hostname),
                style = LockSiteTypography.titleLarge,
                color = LockSiteColors.GreenInk,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = stringResource(R.string.add_confirm_warning),
                style = LockSiteTypography.bodyMedium,
                color = LockSiteColors.GreenInk.copy(alpha = 0.8f),
            )
        }

        PillButton(text = stringResource(R.string.dashboard_add_domain), onClick = onAddAnother)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            GhostButton(text = stringResource(R.string.dashboard_view_list), onClick = onOpenList)
        }
    }
}

/**
 * Confirmation dialog. Dismissing by tapping outside is disabled: the choice must be
 * explicit, and the confirm button is the darkest element on screen.
 */
@Composable
private fun PermanentLockDialog(
    hostname: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false),
    ) {
        SoftCard(
            modifier = Modifier.fillMaxWidth(),
            color = LockSiteColors.Surface,
            shape = RoundedCornerShape(Radius.large),
        ) {
            Text(
                text = stringResource(R.string.add_confirm_title),
                style = LockSiteTypography.titleLarge,
                color = LockSiteColors.Ink,
            )
            Spacer(Modifier.height(Spacing.s))
            Text(
                text = stringResource(R.string.add_confirm_body, hostname),
                style = LockSiteTypography.bodyLarge,
                color = LockSiteColors.InkMuted,
            )

            Spacer(Modifier.height(Spacing.l))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LockSiteColors.DangerSurface, RoundedCornerShape(Radius.control))
                    .padding(Spacing.m),
                horizontalArrangement = Arrangement.spacedBy(Spacing.s),
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = LockSiteColors.Danger,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = stringResource(R.string.add_confirm_warning),
                    style = LockSiteTypography.bodyMedium,
                    color = LockSiteColors.Danger,
                    textAlign = TextAlign.Start,
                )
            }

            Spacer(Modifier.height(Spacing.xl))
            PillButton(
                text = stringResource(R.string.add_confirm_yes),
                onClick = onConfirm,
                containerColor = LockSiteColors.Ink,
                contentColor = LockSiteColors.Surface,
            )
            Spacer(Modifier.height(Spacing.s))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                GhostButton(
                    text = stringResource(R.string.add_confirm_cancel),
                    onClick = onDismiss,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AddDomainPreview() {
    LockSiteTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(LockSiteColors.Canvas)
                .padding(Spacing.screenMargin),
        ) {
            AddDomainForm(
                state = AddDomainViewModel.UiState(input = "instagram.com"),
                onInputChange = {},
                onSubmit = {},
            )
        }
    }
}
