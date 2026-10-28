package com.example.minibooklibrary.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHasherTest {

    @Test
    fun `hash is deterministic for same salt and password`() {
        val salt = "abc123"
        val a = PasswordHasher.hash("hunter2", salt)
        val b = PasswordHasher.hash("hunter2", salt)
        assertEquals(a, b)
    }

    @Test
    fun `different salts produce different hashes for same password`() {
        val a = PasswordHasher.hash("hunter2", "salt1")
        val b = PasswordHasher.hash("hunter2", "salt2")
        assertNotEquals(a, b)
    }

    @Test
    fun `verify accepts the correct password`() {
        val salt = PasswordHasher.newSalt()
        val hash = PasswordHasher.hash("hunter2", salt)
        assertTrue(PasswordHasher.verify("hunter2", salt, hash))
    }

    @Test
    fun `verify rejects an incorrect password`() {
        val salt = PasswordHasher.newSalt()
        val hash = PasswordHasher.hash("hunter2", salt)
        assertFalse(PasswordHasher.verify("hunter3", salt, hash))
    }

    @Test
    fun `verify is constant time-ish for hashes of same length`() {
        // Not a real timing test; just confirms we don't bail on first mismatch by length.
        val salt = PasswordHasher.newSalt()
        val correct = PasswordHasher.hash("password", salt)
        val tamperedSameLength = correct.dropLast(2) + "FF"
        assertFalse(PasswordHasher.verify("password", salt, tamperedSameLength))
    }

    @Test
    fun `hash uses the PBKDF2 format with the current iteration count`() {
        val hash = PasswordHasher.hash("hunter2", PasswordHasher.newSalt())
        assertTrue(hash.startsWith("pbkdf2_sha256\$${PasswordHasher.ITERATIONS}\$"))
        assertFalse(PasswordHasher.needsRehash(hash))
    }

    @Test
    fun `legacy SHA-256 hashes still verify and are flagged for rehash`() {
        val salt = PasswordHasher.newSalt()
        val legacy = PasswordHasher.legacySha256("hunter2", salt)
        assertTrue(PasswordHasher.verify("hunter2", salt, legacy))
        assertFalse(PasswordHasher.verify("hunter3", salt, legacy))
        assertTrue(PasswordHasher.needsRehash(legacy))
    }

    @Test
    fun `hashes with fewer iterations than today's default need a rehash`() {
        val salt = PasswordHasher.newSalt()
        val weak = PasswordHasher.hash("hunter2", salt, iterations = 1_000)
        assertTrue(PasswordHasher.verify("hunter2", salt, weak))
        assertTrue(PasswordHasher.needsRehash(weak))
    }

    @Test
    fun `malformed stored hash is rejected rather than throwing`() {
        assertFalse(PasswordHasher.verify("hunter2", "salt", "pbkdf2_sha256\$0\$abcd"))
        assertFalse(PasswordHasher.verify("hunter2", "salt", "pbkdf2_sha256\$nope\$abcd"))
    }
}
