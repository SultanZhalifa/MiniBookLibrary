package com.example.minibooklibrary.data.repository

import com.example.minibooklibrary.data.local.dao.UserDao
import com.example.minibooklibrary.data.local.entity.UserEntity
import com.example.minibooklibrary.data.preferences.PreferencesManager
import com.example.minibooklibrary.util.PasswordHasher
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserRepositoryTest {

    private val userDao: UserDao = mockk(relaxed = true)
    private val preferences: PreferencesManager = mockk(relaxed = true)
    private val repo = UserRepository(userDao, preferences)

    @Test
    fun `register fails when username is shorter than 3 chars`() = runTest {
        val result = repo.register("ab", "ab@example.com", "password123")
        assertTrue(result is AuthResult.Failure)
        assertEquals(
            AuthResult.FailureReason.USERNAME_TOO_SHORT,
            (result as AuthResult.Failure).reason
        )
    }

    @Test
    fun `register fails when email is malformed`() = runTest {
        val result = repo.register("alice", "not-an-email", "password123")
        assertEquals(AuthResult.FailureReason.INVALID_EMAIL, (result as AuthResult.Failure).reason)
    }

    @Test
    fun `register fails when password is shorter than 6 chars`() = runTest {
        val result = repo.register("alice", "alice@example.com", "12345")
        assertEquals(AuthResult.FailureReason.PASSWORD_TOO_SHORT, (result as AuthResult.Failure).reason)
    }

    @Test
    fun `register fails when username is taken`() = runTest {
        coEvery { userDao.countByUsername("alice") } returns 1
        val result = repo.register("alice", "alice@example.com", "password123")
        assertEquals(
            AuthResult.FailureReason.USERNAME_TAKEN,
            (result as AuthResult.Failure).reason
        )
    }

    @Test
    fun `register inserts new user with hashed password`() = runTest {
        coEvery { userDao.countByUsername("alice") } returns 0
        coEvery { userDao.insert(any()) } returns 42L

        val result = repo.register("alice", "alice@example.com", "password123")

        assertTrue(result is AuthResult.Success)
        val success = result as AuthResult.Success
        assertEquals(42L, success.userId)
        assertEquals("alice", success.username)

        coVerify {
            userDao.insert(match { entity ->
                entity.username == "alice" &&
                    entity.email == "alice@example.com" &&
                    entity.passwordHash != "password123" && // ensure not plaintext
                    entity.passwordHash.startsWith("pbkdf2_sha256$") &&
                    entity.passwordSalt.isNotBlank()
            })
        }
    }

    @Test
    fun `login succeeds with valid credentials and persists session`() = runTest {
        val salt = PasswordHasher.newSalt()
        val hash = PasswordHasher.hash("password123", salt)
        coEvery { userDao.findByUsername("alice") } returns UserEntity(
            id = 7L,
            username = "alice",
            email = "alice@example.com",
            passwordHash = hash,
            passwordSalt = salt
        )

        val result = repo.login("alice", "password123")

        assertTrue(result is AuthResult.Success)
        verify { preferences.currentUserId = 7L }
        verify { preferences.currentUsername = "alice" }
        // Already on the current format: no rehash.
        coVerify(exactly = 0) { userDao.updatePassword(any(), any(), any()) }
    }

    @Test
    fun `login with a legacy SHA-256 hash succeeds and upgrades it to PBKDF2`() = runTest {
        val salt = PasswordHasher.newSalt()
        coEvery { userDao.findByUsername("alice") } returns UserEntity(
            id = 7L,
            username = "alice",
            email = "alice@example.com",
            passwordHash = PasswordHasher.legacySha256("password123", salt),
            passwordSalt = salt
        )

        val result = repo.login("alice", "password123")

        assertTrue(result is AuthResult.Success)
        coVerify {
            userDao.updatePassword(7L, match { it.startsWith("pbkdf2_sha256$") }, match { it != salt })
        }
    }

    @Test
    fun `validateSession clears a session whose user no longer exists`() = runTest {
        every { preferences.isLoggedIn } returns true
        every { preferences.currentUserId } returns 7L
        coEvery { userDao.findById(7L) } returns null

        assertFalse(repo.validateSession())
        verify { preferences.clearSession() }
    }

    @Test
    fun `login fails for unknown username`() = runTest {
        coEvery { userDao.findByUsername("ghost") } returns null
        val result = repo.login("ghost", "anything")
        assertTrue(result is AuthResult.Failure)
        assertEquals(
            AuthResult.FailureReason.INVALID_CREDENTIALS,
            (result as AuthResult.Failure).reason
        )
    }

    @Test
    fun `login fails when password is wrong`() = runTest {
        val salt = PasswordHasher.newSalt()
        coEvery { userDao.findByUsername("alice") } returns UserEntity(
            id = 7L,
            username = "alice",
            email = "alice@example.com",
            passwordHash = PasswordHasher.hash("correct", salt),
            passwordSalt = salt
        )

        val result = repo.login("alice", "wrong")

        assertTrue(result is AuthResult.Failure)
        assertEquals(
            AuthResult.FailureReason.INVALID_CREDENTIALS,
            (result as AuthResult.Failure).reason
        )
    }

    @Test
    fun `logout clears session in preferences`() {
        // preferences is relaxed — clearSession() is auto-stubbed.
        repo.logout()
        verify { preferences.clearSession() }
    }
}
