package com.locksite.app.presentation.blocklist

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.locksite.app.R
import com.locksite.app.domain.model.LockedDomain
import com.locksite.app.presentation.common.formatLockedDate
import com.locksite.app.ui.components.FlatTextField
import com.locksite.app.ui.components.PillButton
import com.locksite.app.ui.components.PillChip
import com.locksite.app.ui.components.ScreenTitle
import com.locksite.app.ui.components.SoftCard
import com.locksite.app.ui.theme.LockSiteColors
import com.locksite.app.ui.theme.LockSiteTheme
import com.locksite.app.ui.theme.LockSiteTypography
import com.locksite.app.ui.theme.Radius
import com.locksite.app.ui.theme.Spacing

/**
 * Locked domain list (PRD FR-013).
 *
 * Read-only by design: there is no delete, edit or unblock affordance anywhere on this
 * screen, and no history of visited sites is shown.
 */
@Composable
fun BlocklistScreen(
    viewModel: BlocklistViewModel,
    onAddDomain: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LockSiteColors.Canvas)
            .padding(horizontal = Spacing.screenMargin, vertical = Spacing.l),
    ) {
        ScreenTitle(
            title = stringResource(R.string.list_title),
            subtitle = stringResource(R.string.list_subtitle),
        )

        Spacer(Modifier.height(Spacing.l))

        if (state.totalCount > 0) {
            FlatTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = stringResource(R.string.list_search_hint),
                imeAction = ImeAction.Search,
            )
            Spacer(Modifier.height(Spacing.l))
        }

        Box(modifier = Modifier.weight(1f)) {
            when {
                state.loadError -> ListMessage(
                    title = stringResource(R.string.status_error),
                    body = stringResource(R.string.add_error),
                )

                state.totalCount == 0 -> ListMessage(
                    title = stringResource(R.string.list_empty_title),
                    body = stringResource(R.string.list_empty_body),
                )

                state.domains.isEmpty() -> ListMessage(
                    title = stringResource(R.string.list_no_result),
                    body = stringResource(R.string.list_search_hint),
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.m),
                ) {
                    items(items = state.domains, key = { it.id }) { domain ->
                        DomainRow(domain)
                    }
                }
            }
        }

        Spacer(Modifier.height(Spacing.l))
        PillButton(text = stringResource(R.string.dashboard_add_domain), onClick = onAddDomain)
    }
}

@Composable
private fun DomainRow(domain: LockedDomain) {
    SoftCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = domain.hostname,
                    style = LockSiteTypography.titleMedium,
                    color = LockSiteColors.Ink,
                )
                Text(
                    text = stringResource(
                        R.string.list_added_on,
                        formatLockedDate(domain.createdAtEpochMillis),
                    ),
                    modifier = Modifier.padding(top = Spacing.xs),
                    style = LockSiteTypography.bodyMedium,
                    color = LockSiteColors.InkMuted,
                )
            }
            PillChip(
                text = stringResource(R.string.list_locked_badge),
                containerColor = LockSiteColors.Green,
                contentColor = LockSiteColors.GreenInk,
                icon = Icons.Filled.Lock,
            )
        }
    }
}

@Composable
private fun ListMessage(title: String, body: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(LockSiteColors.Teal, RoundedCornerShape(Radius.pill)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = LockSiteColors.TealInk,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.height(Spacing.l))
        Text(
            text = title,
            style = LockSiteTypography.titleMedium,
            color = LockSiteColors.Ink,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = body,
            modifier = Modifier.padding(horizontal = Spacing.xl),
            style = LockSiteTypography.bodyMedium,
            color = LockSiteColors.InkMuted,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BlocklistPreview() {
    LockSiteTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(LockSiteColors.Canvas)
                .padding(Spacing.screenMargin),
        ) {
            DomainRow(
                LockedDomain(
                    id = 1,
                    hostname = "instagram.com",
                    includeSubdomains = true,
                    createdAtEpochMillis = 1_761_600_000_000,
                ),
            )
        }
    }
}
