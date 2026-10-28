package com.locksite.app.presentation.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locksite.app.diagnostics.DiagnosticsSelfTest
import com.locksite.app.domain.model.ProtectionStatus
import com.locksite.app.domain.repository.BlocklistRepository
import com.locksite.app.vpn.VpnController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Diagnostics state (PRD FR-031).
 *
 * Everything shown is a real observation: the service state comes from the running
 * service, the permission and conflict flags come from Android, and the self-test reads
 * the actual blocklist. Nothing is persisted, so a stale "all good" cannot survive a
 * restart.
 */
class DiagnosticsViewModel(
    private val repository: BlocklistRepository,
    private val vpnController: VpnController,
) : ViewModel() {

    data class UiState(
        val serviceStatus: ProtectionStatus = ProtectionStatus.NOT_CONFIGURED,
        val statusDetail: String? = null,
        /** `null` means "could not determine", never "denied". */
        val permissionGranted: Boolean? = null,
        /** `null` means "could not determine", never "no other VPN". */
        val foreignVpnActive: Boolean? = null,
        /** `null` when the blocklist could not be counted. */
        val lockedCount: Int? = null,
        val test: TestState = TestState.NotRun,
        val lastCheckedAtEpochMillis: Long? = null,
    )

    sealed interface TestState {
        data object NotRun : TestState
        data object Running : TestState
        data class Finished(val result: DiagnosticsSelfTest.Result) : TestState
        data class Failed(val reason: FailureReason) : TestState
    }

    enum class FailureReason { BLOCKLIST_UNREADABLE }

    private data class Snapshot(
        val permissionGranted: Boolean? = null,
        val foreignVpnActive: Boolean? = null,
        val test: TestState = TestState.NotRun,
        val lastCheckedAtEpochMillis: Long? = null,
    )

    private val snapshot = MutableStateFlow(Snapshot())

    /**
     * `-1` marks "not known yet" or a failed read.
     *
     * [onStart] matters: `combine` waits for every flow to emit, so without it a slow or
     * stuck count query would leave the whole screen on its initial value — reporting
     * "Belum aktif" even while the service is running.
     */
    private val count = repository.observeCount()
        .onStart { emit(-1) }
        .catch { emit(-1) }

    val uiState: StateFlow<UiState> = combine(
        vpnController.status,
        vpnController.detail,
        count,
        snapshot,
    ) { status, detail, lockedCount, local ->
        UiState(
            serviceStatus = status,
            statusDetail = detail,
            permissionGranted = local.permissionGranted,
            foreignVpnActive = local.foreignVpnActive,
            lockedCount = lockedCount.takeIf { it >= 0 },
            test = local.test,
            lastCheckedAtEpochMillis = local.lastCheckedAtEpochMillis,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UiState(),
    )

    init {
        refresh()
    }

    /** Re-reads the Android-level facts. Cheap enough to run on every screen resume. */
    fun refresh() {
        runCatching { vpnController.refreshConflictState() }
        val permission = runCatching { vpnController.hasPermission() }.getOrNull()
        val foreignVpn = runCatching { vpnController.foreignVpnState() }.getOrNull()

        snapshot.update {
            it.copy(
                permissionGranted = permission,
                foreignVpnActive = foreignVpn,
                lastCheckedAtEpochMillis = System.currentTimeMillis(),
            )
        }
    }

    fun runSelfTest() {
        if (snapshot.value.test is TestState.Running) return
        snapshot.update { it.copy(test = TestState.Running) }

        viewModelScope.launch {
            val rules = try {
                repository.snapshot()
            } catch (error: Exception) {
                snapshot.update {
                    it.copy(
                        test = TestState.Failed(FailureReason.BLOCKLIST_UNREADABLE),
                        lastCheckedAtEpochMillis = System.currentTimeMillis(),
                    )
                }
                return@launch
            }

            // Pure string work over every rule; keep it off the main thread.
            val result = withContext(Dispatchers.Default) { DiagnosticsSelfTest.run(rules) }
            snapshot.update {
                it.copy(
                    test = TestState.Finished(result),
                    lastCheckedAtEpochMillis = System.currentTimeMillis(),
                )
            }
        }
    }
}
