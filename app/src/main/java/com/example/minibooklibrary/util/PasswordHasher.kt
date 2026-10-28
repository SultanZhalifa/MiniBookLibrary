package com.example.minibooklibrary.util

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Salted PBKDF2-HMAC-SHA256 password hashing.
 *
 * The first version of the app used a single round of salted SHA-256. That stops
 * rainbow tables but a GPU can still try billions of guesses per second against a
 * leaked SQLite file. PBKDF2 ships with the platform (API 26+) so it costs no
 * dependency, and 600k iterations is the current OWASP recommendation for SHA-256.
 *
 * Stored hashes carry their own algorithm and iteration count
 * (`pbkdf2_sha256$600000$<hex>`), so the cost can be raised later without a schema
 * change. Hashes without that prefix are the old SHA-256 format: they still verify,
 * and [needsRehash] tells the caller to upgrade them on the next successful login.
 */
object PasswordHasher {

    private const val SALT_BYTES = 16
    private const val KEY_BITS = 256
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val PREFIX = "pbkdf2_sha256"
    internal const val ITERATIONS = 600_000

    /** Generate a fresh random salt as a hex string, safe to store in SQLite. */
    fun newSalt(): String {
        val bytes = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(bytes)
        return bytes.toHex()
    }

    /** Hash [password] with [salt]. Both inputs are required and must not be blank. */
    fun hash(password: String, salt: String, iterations: Int = ITERATIONS): String {
        require(password.isNotEmpty()) { "Password must not be empty" }
        require(salt.isNotBlank()) { "Salt must not be blank" }
        return "$PREFIX\$$iterations\$${pbkdf2(password, salt, iterations)}"
    }

    /** Check [password] against a stored hash in either the current or the legacy format. */
    fun verify(password: String, salt: String, storedHash: String): Boolean {
        if (password.isEmpty() || salt.isBlank()) return false
        val computed = if (storedHash.startsWith("$PREFIX\$")) {
            val iterations = storedHash.split('$').getOrNull(1)?.toIntOrNull()?.takeIf { it > 0 } ?: return false
            hash(password, salt, iterations)
        } else {
            legacySha256(password, salt)
        }
        // MessageDigest.isEqual is constant-time for equal-length inputs.
        return MessageDigest.isEqual(
            computed.toByteArray(Charsets.UTF_8),
            storedHash.toByteArray(Charsets.UTF_8)
        )
    }

    /** True when [storedHash] uses the legacy format or fewer iterations than today's default. */
    fun needsRehash(storedHash: String): Boolean {
        if (!storedHash.startsWith("$PREFIX\$")) return true
        val iterations = storedHash.split('$').getOrNull(1)?.toIntOrNull() ?: return true
        return iterations < ITERATIONS
    }

    private fun pbkdf2(password: String, salt: String, iterations: Int): String {
        val spec = PBEKeySpec(
            password.toCharArray(),
            salt.toByteArray(Charsets.UTF_8),
            iterations,
            KEY_BITS
        )
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded.toHex()
        } finally {
            spec.clearPassword()
        }
    }

    /** The v1 format: one round of SHA-256 over salt + password. Kept only to verify old accounts. */
    internal fun legacySha256(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt.toByteArray(Charsets.UTF_8))
        digest.update(password.toByteArray(Charsets.UTF_8))
        return digest.digest().toHex()
    }

    private fun ByteArray.toHex(): String =
        joinToString(separator = "") { "%02x".format(it) }
}
