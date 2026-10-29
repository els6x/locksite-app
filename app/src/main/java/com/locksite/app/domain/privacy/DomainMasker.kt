package com.locksite.app.domain.privacy

/**
 * Masks a locked hostname for on-screen privacy.
 *
 * Every character except the dot separators becomes [MASK_CHAR]. The label structure and
 * length are preserved so the text still reads as a domain, but the name itself cannot be
 * read over a shoulder. This is a display transform only: the stored hostname is untouched,
 * so filtering, matching and search keep working on the real name.
 */
object DomainMasker {

    const val MASK_CHAR = '•'

    fun mask(hostname: String): String = buildString(hostname.length) {
        hostname.forEach { char ->
            append(if (char == '.') char else MASK_CHAR)
        }
    }
}
