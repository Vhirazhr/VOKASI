package com.example.vokasilearning.logic

import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale

/**
 * Converts a student's human-readable full name into the existing private Auth
 * email alias. Students never need to type dots.
 *
 * The 2026/2027 roster contains one duplicate full name. Login by that name
 * alone is deliberately rejected to prevent one student entering the other's
 * account when both are using the school-issued initial password.
 * For duplicate names enter "Nama Lengkap | Kelas".
 */
internal object StudentLoginName {
    private const val DUPLICATE_NAME_DIGEST =
        "507d41cce5272b62c382c1a19141fb61cb68a7a65937ff93e44b75a38dc8d28b"

    data class Parsed(
        val normalizedFullName: String?,
        val classHint: String?,
        val aliases: List<String>
    )

    fun normalizeFullName(value: String): String =
        value.trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    fun parse(input: String): Parsed? {
        val sections = input.split('|')
        if (sections.size > 2) return null
        val name = sections[0].trim().replace(Regex("\\s+"), " ")
        if (name.length < 2) return null

        val classHint = sections.getOrNull(1)?.trim()
            ?.replace(Regex("\\s+"), " ")?.uppercase(Locale.ROOT)
        if (sections.size == 2 && classHint.isNullOrBlank()) return null

        val isLegacyAlias = !name.contains(' ') && name.contains('.') &&
            name.matches(Regex("[A-Za-z0-9._]+"))
        if (isLegacyAlias) {
            return Parsed(null, classHint, listOf(name.lowercase(Locale.ROOT)))
        }

        val fullName = normalizeFullName(name)
        if (sha256(fullName) == DUPLICATE_NAME_DIGEST && classHint == null) {
            // Full name alone is ambiguous; class is mandatory for this pair.
            return null
        }
        val ascii = Normalizer.normalize(name, Normalizer.Form.NFD)
            .replace(Regex("[\\u0300-\\u036f]"), "")
        val base = ascii.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]+"), ".")
            .trim('.')
            .take(26)
            .trimEnd('.')
            .ifEmpty { "siswa" }
        // Existing provisioning assigns .2, .3, ... if aliases collide.
        // Only probe alternatives when a class is supplied; validate the
        // resulting authenticated roster against both name and class.
        val aliases = if (classHint == null) listOf(base)
            else (1..10).map { index ->
                if (index == 1) base
                else {
                    val suffix = ".$index"
                    base.take(30 - suffix.length).trimEnd('.') + suffix
                }
            }
        return Parsed(fullName, classHint, aliases)
    }
}
