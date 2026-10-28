package com.locksite.app.domain.validation

/**
 * Hostname validation and normalisation (PRD FR-010).
 *
 * Policy for M1:
 * - Only ASCII hostnames are accepted. IDN input must be entered in its punycode form
 *   (`xn--...`). Unicode is rejected with a clear message rather than silently converted.
 * - Bare IPv4 literals are rejected: DNS filtering cannot protect an IP-based access path.
 * - The result is always lowercase with a single trailing dot removed.
 *
 * This is pure Kotlin on purpose — no Android dependency, fully unit testable.
 */
object DomainValidator {

    private const val MAX_TOTAL_LENGTH = 253
    private const val MAX_LABEL_LENGTH = 63

    private val ipv4Pattern = Regex("""^\d{1,3}(\.\d{1,3}){3}$""")

    fun validate(rawInput: String): ValidationResult {
        val trimmed = rawInput.trim()

        if (trimmed.isEmpty()) return ValidationResult.Invalid(ValidationError.EMPTY)

        if (trimmed.any { it.isWhitespace() }) {
            return ValidationResult.Invalid(ValidationError.WHITESPACE)
        }
        if (trimmed.contains("://") ||
            trimmed.contains('/') ||
            trimmed.contains(':') ||
            trimmed.contains('?') ||
            trimmed.contains('#') ||
            trimmed.contains('@')
        ) {
            return ValidationResult.Invalid(ValidationError.SCHEME_OR_PATH)
        }

        val nonAscii = trimmed.any { it.code > 127 }
        if (nonAscii) return ValidationResult.Invalid(ValidationError.NON_ASCII)

        if (trimmed.any { !(it.isLetterOrDigit() || it == '.' || it == '-') }) {
            return ValidationResult.Invalid(ValidationError.INVALID_CHARS)
        }

        val normalised = trimmed.lowercase().removeSuffix(".")

        if (normalised.isEmpty()) return ValidationResult.Invalid(ValidationError.EMPTY)
        if (normalised.contains("..")) return ValidationResult.Invalid(ValidationError.CONSECUTIVE_DOTS)
        if (normalised.length > MAX_TOTAL_LENGTH) return ValidationResult.Invalid(ValidationError.TOO_LONG)
        if (ipv4Pattern.matches(normalised)) return ValidationResult.Invalid(ValidationError.IP_ADDRESS)
        if (!normalised.contains('.')) return ValidationResult.Invalid(ValidationError.NO_DOT)

        val labels = normalised.split('.')
        for (label in labels) {
            if (label.isEmpty()) return ValidationResult.Invalid(ValidationError.CONSECUTIVE_DOTS)
            if (label.length > MAX_LABEL_LENGTH) return ValidationResult.Invalid(ValidationError.LABEL_TOO_LONG)
            if (label.startsWith('-') || label.endsWith('-')) {
                return ValidationResult.Invalid(ValidationError.INVALID_CHARS)
            }
        }

        val tld = labels.last()
        if (tld.length < 2 || tld.all { it.isDigit() }) {
            return ValidationResult.Invalid(ValidationError.TLD_INVALID)
        }

        return ValidationResult.Valid(normalised)
    }
}

sealed interface ValidationResult {
    data class Valid(val hostname: String) : ValidationResult
    data class Invalid(val error: ValidationError) : ValidationResult
}

enum class ValidationError {
    EMPTY,
    WHITESPACE,
    SCHEME_OR_PATH,
    NON_ASCII,
    INVALID_CHARS,
    CONSECUTIVE_DOTS,
    TOO_LONG,
    IP_ADDRESS,
    NO_DOT,
    LABEL_TOO_LONG,
    TLD_INVALID,
}
