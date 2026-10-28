package com.locksite.app.domain.usecase

import com.locksite.app.domain.repository.AddOutcome
import com.locksite.app.domain.repository.BlocklistRepository
import com.locksite.app.domain.validation.DomainValidator
import com.locksite.app.domain.validation.ValidationError

/**
 * Validates and persists a new locked domain (PRD FR-010, FR-012).
 *
 * Order is fixed: normalise → validate → persist. Nothing is written before the hostname
 * is proven valid, and a duplicate is never silently accepted.
 */
class AddDomainUseCase(
    private val repository: BlocklistRepository,
) {
    suspend operator fun invoke(
        rawHostname: String,
        includeSubdomains: Boolean = true,
    ): AddDomainResult {
        val normalisedHostname = when (val result = DomainValidator.validate(rawHostname)) {
            is com.locksite.app.domain.validation.ValidationResult.Invalid ->
                return AddDomainResult.Invalid(result.error)
            is com.locksite.app.domain.validation.ValidationResult.Valid -> result.hostname
        }

        return when (repository.add(normalisedHostname, includeSubdomains)) {
            AddOutcome.ADDED -> AddDomainResult.Added(normalisedHostname)
            AddOutcome.DUPLICATE -> AddDomainResult.Duplicate(normalisedHostname)
            AddOutcome.FAILURE -> AddDomainResult.Failure
        }
    }
}

sealed interface AddDomainResult {
    data class Added(val hostname: String) : AddDomainResult
    data class Duplicate(val hostname: String) : AddDomainResult
    data class Invalid(val error: ValidationError) : AddDomainResult
    data object Failure : AddDomainResult
}
