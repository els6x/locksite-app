package com.locksite.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locksite.app.domain.model.ProtectionStatus
import com.locksite.app.domain.repository.BlocklistRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Dashboard state.
 *
 * [status] is NOT_CONFIGURED until the VPN service exists (M2). It is hard-coded here on
 * purpose: showing a stored boolean as "active" would be a lie (PRD FR-023, FR-030).
 */
class DashboardViewModel(
    repository: BlocklistRepository,
) : ViewModel() {

    data class UiState(
        val status: ProtectionStatus = ProtectionStatus.NOT_CONFIGURED,
        val lockedCount: Int = 0,
        val latestHostname: String? = null,
        val loading: Boolean = true,
        val loadError: Boolean = false,
    )

    val uiState: StateFlow<UiState> = repository.observeAll()
        .map { domains ->
            UiState(
                status = ProtectionStatus.NOT_CONFIGURED,
                lockedCount = domains.size,
                latestHostname = domains.firstOrNull()?.hostname,
                loading = false,
                loadError = false,
            )
        }
        .catch {
            // A read failure is an error state, never "empty and therefore safe".
            emit(UiState(loading = false, loadError = true))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState(),
        )
}
