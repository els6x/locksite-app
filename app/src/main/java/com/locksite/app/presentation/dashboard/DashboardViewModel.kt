package com.locksite.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locksite.app.domain.model.LockedDomain
import com.locksite.app.domain.model.ProtectionStatus
import com.locksite.app.domain.repository.BlocklistRepository
import com.locksite.app.vpn.VpnController
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Dashboard state.
 *
 * [status] comes straight from [VpnController], which is pushed by the running service.
 * There is no stored "protection on" flag anywhere in the app: if the service is not
 * running, the dashboard cannot say it is.
 */
class DashboardViewModel(
    repository: BlocklistRepository,
    vpnController: VpnController,
    maskDomains: StateFlow<Boolean>,
) : ViewModel() {

    data class UiState(
        val status: ProtectionStatus = ProtectionStatus.NOT_CONFIGURED,
        val statusDetail: String? = null,
        val lockedCount: Int = 0,
        val latestHostname: String? = null,
        val maskDomains: Boolean = false,
        val loading: Boolean = true,
        val loadError: Boolean = false,
    ) {
        val protectionRunning: Boolean get() = status == ProtectionStatus.ACTIVE
    }

    private val domains = repository.observeAll()
        .map<List<LockedDomain>, Result<List<LockedDomain>>> { list -> Result.success(list) }
        .catch { error -> emit(Result.failure(error)) }

    val uiState: StateFlow<UiState> =
        combine(
            domains,
            vpnController.status,
            vpnController.detail,
            maskDomains,
        ) { result, status, detail, mask ->
            val list = result.getOrNull()
            UiState(
                status = status,
                statusDetail = detail,
                lockedCount = list?.size ?: 0,
                latestHostname = list?.firstOrNull()?.hostname,
                maskDomains = mask,
                loading = false,
                // A read failure is an error state, never "empty and therefore safe".
                loadError = result.isFailure,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState(),
        )
}
