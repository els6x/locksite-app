package com.locksite.app.domain

import com.locksite.app.domain.model.LockedDomain
import com.locksite.app.domain.repository.AddOutcome
import com.locksite.app.domain.repository.BlocklistRepository
import com.locksite.app.domain.usecase.AddDomainResult
import com.locksite.app.domain.usecase.AddDomainUseCase
import com.locksite.app.domain.validation.ValidationError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AddDomainUseCaseTest {

    private class FakeRepository(
        private val outcome: AddOutcome = AddOutcome.ADDED,
    ) : BlocklistRepository {
        val added = mutableListOf<Pair<String, Boolean>>()

        override fun observeAll(): Flow<List<LockedDomain>> = flowOf(emptyList())

        override fun observeCount(): Flow<Int> = flowOf(0)

        override suspend fun snapshot(): List<LockedDomain> = emptyList()

        override suspend fun add(hostname: String, includeSubdomains: Boolean): AddOutcome {
            added += hostname to includeSubdomains
            return outcome
        }
    }

    @Test
    fun `persists the normalised hostname`() = runBlocking {
        val repository = FakeRepository()
        val result = AddDomainUseCase(repository)("  INSTAGRAM.com. ")

        assertEquals(AddDomainResult.Added("instagram.com"), result)
        assertEquals(listOf("instagram.com" to true), repository.added)
    }

    @Test
    fun `invalid input is rejected before persistence`() = runBlocking {
        val repository = FakeRepository()
        val result = AddDomainUseCase(repository)("https://instagram.com/feed")

        assertEquals(AddDomainResult.Invalid(ValidationError.SCHEME_OR_PATH), result)
        assertTrue(repository.added.isEmpty())
    }

    @Test
    fun `duplicate is reported not merged`() = runBlocking {
        val repository = FakeRepository(outcome = AddOutcome.DUPLICATE)
        val result = AddDomainUseCase(repository)("instagram.com")

        assertEquals(AddDomainResult.Duplicate("instagram.com"), result)
    }

    @Test
    fun `storage failure is surfaced`() = runBlocking {
        val repository = FakeRepository(outcome = AddOutcome.FAILURE)
        val result = AddDomainUseCase(repository)("instagram.com")

        assertEquals(AddDomainResult.Failure, result)
    }
}
