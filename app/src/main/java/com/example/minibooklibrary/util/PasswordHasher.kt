package com.example.minibooklibrary.util

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Lightweight salted SHA-256 password hashing.
 *
 * Why not bcrypt/Argon2: those would add a dependency and aren't free on cold start of an
 * offline-first portfolio app. SHA-256 with a per-user salt is enough to defeat trivial
 * rainbow-table attacks against a leaked SQLite file, which is the realistic threat
 * model here. Real production code should use a memory-hard KDF.
 */
object PasswordHasher {

    private const val SALT_BYTES = 16

    /** Generate a fresh salt (Base64-ish hex string, safe to store in SQLite). */
    fun newSalt(): String {
        val bytes = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(bytes)
        return bytes.toHex()
    }

    /** Hash [password] with [salt]. Both inputs are required and must not be blank. */
    fun hash(password: String, salt: String): String {
        require(password.isNotEmpty()) { "Password must not be empty" }
        require(salt.isNotBlank()) { "Salt must not be blank" }
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt.toByteArray(Charsets.UTF_8))
        digest.update(password.toByteArray(Charsets.UTF_8))
        return digest.digest().toHex()
    }

    /** Constant-time-ish comparison so callers don't need to remember to do it themselves. */
    fun verify(password: String, salt: String, expectedHash: String): Boolean {
        val computed = hash(password, salt)
        if (computed.length != expectedHash.length) return false
        var result = 0
        for (i in computed.indices) {
            result = result or (computed[i].code xor expectedHash[i].code)
        }
        return result == 0
    }

    private fun ByteArray.toHex(): String =
        joinToString(separator = "") { "%02x".format(it) }
}
