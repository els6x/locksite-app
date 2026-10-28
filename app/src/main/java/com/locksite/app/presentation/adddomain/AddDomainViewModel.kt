package com.locksite.app.presentation.adddomain

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locksite.app.R
import com.locksite.app.domain.usecase.AddDomainResult
import com.locksite.app.domain.usecase.AddDomainUseCase
import com.locksite.app.domain.validation.DomainValidator
import com.locksite.app.domain.validation.ValidationError
import com.locksite.app.domain.validation.ValidationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Add-domain flow: type → validate → confirm → persist.
 *
 * Validation runs on submit, not on every keystroke, so the user is not scolded while
 * typing (Postel: liberal input, strict meaning).
 */
class AddDomainViewModel(
    private val addDomainUseCase: AddDomainUseCase,
) : ViewModel() {

    data class UiState(
        val input: String = "",
        @StringRes val errorMessage: Int? = null,
        val pendingConfirmation: String? = null,
        val savedHostname: String? = null,
        val saving: Boolean = false,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun onInputChange(value: String) {
        _uiState.value = _uiState.value.copy(
            input = value,
            errorMessage = null,
            savedHostname = null,
        )
    }

    /** Step 1: validate, then ask for explicit confirmation. Nothing is written yet. */
    fun onSubmit() {
        val current = _uiState.value
        if (current.saving) return

        when (val result = DomainValidator.validate(current.input)) {
            is ValidationResult.Invalid ->
                _uiState.value = current.copy(errorMessage = messageFor(result.error))

            is ValidationResult.Valid ->
                _uiState.value = current.copy(
                    input = result.hostname,
                    errorMessage = null,
                    pendingConfirmation = result.hostname,
                )
        }
    }

    fun onDismissConfirmation() {
        _uiState.value = _uiState.value.copy(pendingConfirmation = null)
    }

    /** Step 2: user confirmed the lock is permanent. */
    fun onConfirmLock() {
        val hostname = _uiState.value.pendingConfirmation ?: return
        if (_uiState.value.saving) return

        _uiState.value = _uiState.value.copy(saving = true, pendingConfirmation = null)

        viewModelScope.launch {
            val result = addDomainUseCase(hostname)
            _uiState.value = when (result) {
                is AddDomainResult.Added -> _uiState.value.copy(
                    input = "",
                    saving = false,
                    savedHostname = result.hostname,
                )

                is AddDomainResult.Duplicate -> _uiState.value.copy(
                    saving = false,
                    errorMessage = R.string.add_duplicate,
                )

                is AddDomainResult.Invalid -> _uiState.value.copy(
                    saving = false,
                    errorMessage = messageFor(result.error),
                )

                AddDomainResult.Failure -> _uiState.value.copy(
                    saving = false,
                    errorMessage = R.string.add_error,
                )
            }
        }
    }

    fun onAddAnother() {
        _uiState.value = UiState()
    }

    private fun messageFor(error: ValidationError): Int = when (error) {
        ValidationError.EMPTY -> R.string.err_empty
        ValidationError.WHITESPACE -> R.string.err_space
        ValidationError.SCHEME_OR_PATH -> R.string.err_scheme
        ValidationError.NON_ASCII -> R.string.err_non_ascii
        ValidationError.INVALID_CHARS -> R.string.err_chars
        ValidationError.CONSECUTIVE_DOTS -> R.string.err_dots
        ValidationError.TOO_LONG -> R.string.err_too_long
        ValidationError.IP_ADDRESS -> R.string.err_ip
        ValidationError.NO_DOT -> R.string.err_no_dot
        ValidationError.LABEL_TOO_LONG -> R.string.err_label_len
        ValidationError.TLD_INVALID -> R.string.err_tld
    }
}
