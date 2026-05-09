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
}
