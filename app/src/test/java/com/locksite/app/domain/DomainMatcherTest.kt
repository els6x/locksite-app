package com.locksite.app.domain

import com.locksite.app.domain.matcher.DomainMatcher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainMatcherTest {

    @Test
    fun `exact hostname matches`() {
        assertTrue(DomainMatcher.matches("example.com", "example.com", includeSubdomains = true))
    }

    @Test
    fun `subdomain matches when scope includes subdomains`() {
        assertTrue(DomainMatcher.matches("www.example.com", "example.com", includeSubdomains = true))
        assertTrue(DomainMatcher.matches("a.b.example.com", "example.com", includeSubdomains = true))
    }

    @Test
    fun `subdomain does not match when scope is exact only`() {
        assertFalse(DomainMatcher.matches("www.example.com", "example.com", includeSubdomains = false))
        assertTrue(DomainMatcher.matches("example.com", "example.com", includeSubdomains = false))
    }

    @Test
    fun `look-alike domain does not match`() {
        assertFalse(DomainMatcher.matches("notexample.com", "example.com", includeSubdomains = true))
    }

    @Test
    fun `suffix injection does not match`() {
        assertFalse(
            DomainMatcher.matches("example.com.attacker.test", "example.com", includeSubdomains = true),
        )
    }

    @Test
    fun `matching is case insensitive`() {
        assertTrue(DomainMatcher.matches("WWW.Example.COM", "example.com", includeSubdomains = true))
    }

    @Test
    fun `trailing dots are ignored`() {
        assertTrue(DomainMatcher.matches("example.com.", "example.com", includeSubdomains = true))
        assertTrue(DomainMatcher.matches("example.com", "example.com.", includeSubdomains = true))
    }

    @Test
    fun `empty input never matches`() {
        assertFalse(DomainMatcher.matches("", "example.com", includeSubdomains = true))
        assertFalse(DomainMatcher.matches("example.com", "", includeSubdomains = true))
    }

    @Test
    fun `matchesAny finds one blocking rule`() {
        val rules = listOf("instagram.com" to true, "example.com" to false)
        assertTrue(DomainMatcher.matchesAny("m.instagram.com", rules))
        assertFalse(DomainMatcher.matchesAny("sub.example.com", rules))
    }
}
