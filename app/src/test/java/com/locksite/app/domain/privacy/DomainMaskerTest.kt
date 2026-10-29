package com.locksite.app.domain.privacy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainMaskerTest {

    @Test
    fun `masks every letter and keeps the dot separators`() {
        assertEquals("•••••••••.•••", DomainMasker.mask("instagram.com"))
    }

    @Test
    fun `keeps the label structure of a subdomain`() {
        assertEquals("•.•••.••.••", DomainMasker.mask("a.bcd.co.id"))
    }

    @Test
    fun `preserves the original length`() {
        val input = "example.com"
        assertEquals(input.length, DomainMasker.mask(input).length)
    }

    @Test
    fun `leaves no letter or digit readable`() {
        val masked = DomainMasker.mask("secret.example.com")
        assertTrue(masked.none { it.isLetterOrDigit() })
    }

    @Test
    fun `keeps every dot in place`() {
        val input = "a.b.c.d"
        val masked = DomainMasker.mask(input)
        input.indices.forEach { index ->
            if (input[index] == '.') assertEquals('.', masked[index])
        }
    }

    @Test
    fun `empty hostname stays empty`() {
        assertEquals("", DomainMasker.mask(""))
    }

    @Test
    fun `masking twice changes nothing`() {
        val once = DomainMasker.mask("x.co")
        assertEquals(once, DomainMasker.mask(once))
    }
}
