package com.locksite.app.presentation.blocklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locksite.app.domain.model.LockedDomain
import com.locksite.app.domain.repository.BlocklistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Read-only list of locked domains.
 *
 * There is no delete or edit action here by design (PRD section 6).
 */
class BlocklistViewModel(
    repository: BlocklistRepository,
    maskDomains: StateFlow<Boolean>,
) : ViewModel() {

    data class UiState(
        val domains: List<LockedDomain> = emptyList(),
        val query: String = "",
        val totalCount: Int = 0,
        val maskDomains: Boolean = false,
        val loading: Boolean = true,
        val loadError: Boolean = false,
    )

    private val query = MutableStateFlow("")

    val uiState: StateFlow<UiState> = combine(
        repository.observeAll(),
        query,
        maskDomains,
    ) { domains, search, mask ->
        val filtered = if (search.isBlank()) {
            domains
        } else {
            domains.filter { it.hostname.contains(search.trim(), ignoreCase = true) }
        }
        UiState(
            domains = filtered,
            query = search,
            totalCount = domains.size,
            maskDomains = mask,
            loading = false,
            loadError = false,
        )
    }
        .catch { emit(UiState(loading = false, loadError = true)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState(),
        )

    fun onQueryChange(value: String) {
        query.value = value
    }
}
