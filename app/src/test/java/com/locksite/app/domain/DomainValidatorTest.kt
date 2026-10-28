package com.locksite.app.domain

import com.locksite.app.domain.validation.DomainValidator
import com.locksite.app.domain.validation.ValidationError
import com.locksite.app.domain.validation.ValidationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainValidatorTest {

    private fun assertInvalid(expected: ValidationError, input: String) {
        val result = DomainValidator.validate(input)
        assertTrue("expected Invalid for '$input' but was $result", result is ValidationResult.Invalid)
        assertEquals(expected, (result as ValidationResult.Invalid).error)
    }

    private fun assertValid(expectedHostname: String, input: String) {
        val result = DomainValidator.validate(input)
        assertTrue("expected Valid for '$input' but was $result", result is ValidationResult.Valid)
        assertEquals(expectedHostname, (result as ValidationResult.Valid).hostname)
    }

    @Test
    fun `accepts a plain hostname`() {
        assertValid("example.com", "example.com")
    }

    @Test
    fun `accepts multi label hostname`() {
        assertValid("a.b.co.id", "a.b.co.id")
    }

    @Test
    fun `normalises uppercase and surrounding spaces`() {
        assertValid("example.com", "  EXAMPLE.COM  ")
    }

    @Test
    fun `removes a single trailing dot`() {
        assertValid("example.com", "example.com.")
    }

    @Test
    fun `rejects empty input`() {
        assertInvalid(ValidationError.EMPTY, "   ")
    }

    @Test
    fun `rejects scheme and path`() {
        assertInvalid(ValidationError.SCHEME_OR_PATH, "https://example.com")
        assertInvalid(ValidationError.SCHEME_OR_PATH, "example.com/path")
        assertInvalid(ValidationError.SCHEME_OR_PATH, "example.com:8080")
        assertInvalid(ValidationError.SCHEME_OR_PATH, "example.com?q=1")
    }

    @Test
    fun `rejects whitespace inside the hostname`() {
        assertInvalid(ValidationError.WHITESPACE, "exa mple.com")
    }

    @Test
    fun `rejects a hostname without a dot`() {
        assertInvalid(ValidationError.NO_DOT, "localhost")
    }

    @Test
    fun `rejects IPv4 literals`() {
        assertInvalid(ValidationError.IP_ADDRESS, "192.168.1.1")
    }

    @Test
    fun `rejects non ascii input`() {
        assertInvalid(ValidationError.NON_ASCII, "exämple.com")
    }

    @Test
    fun `rejects consecutive dots`() {
        assertInvalid(ValidationError.CONSECUTIVE_DOTS, "example..com")
    }

    @Test
    fun `rejects labels starting or ending with a hyphen`() {
        assertInvalid(ValidationError.INVALID_CHARS, "-example.com")
        assertInvalid(ValidationError.INVALID_CHARS, "example-.com")
    }

    @Test
    fun `rejects a one character TLD`() {
        assertInvalid(ValidationError.TLD_INVALID, "example.c")
    }

    @Test
    fun `rejects an all numeric TLD`() {
        assertInvalid(ValidationError.TLD_INVALID, "example.123")
    }

    @Test
    fun `rejects an over long label`() {
        val label = "a".repeat(64)
        assertInvalid(ValidationError.LABEL_TOO_LONG, "$label.com")
    }
}
