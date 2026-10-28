package com.example.minibooklibrary.data.repository

import com.example.minibooklibrary.data.local.dao.UserDao
import com.example.minibooklibrary.data.local.entity.UserEntity
import com.example.minibooklibrary.data.preferences.PreferencesManager
import com.example.minibooklibrary.util.PasswordHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Coordinates user account creation and sign-in.
 *
 * The repository owns the password-hashing detail so that ViewModels never see plaintext
 * passwords flow through them — they pass strings in and get an [AuthResult] back.
 */
class UserRepository(
    private val userDao: UserDao,
    private val preferences: PreferencesManager
) {

    /** Register a new user. Returns [AuthResult.Success] on success, otherwise a typed failure. */
    suspend fun register(username: String, email: String, password: String): AuthResult =
        withContext(Dispatchers.IO) {
            val cleanedUsername = username.trim()
            val cleanedEmail = email.trim()

            validate(cleanedUsername, cleanedEmail, password)?.let { reason ->
                return@withContext AuthResult.Failure(reason)
            }

            if (userDao.countByUsername(cleanedUsername) > 0) {
                return@withContext AuthResult.Failure(AuthResult.FailureReason.USERNAME_TAKEN)
            }

            val salt = PasswordHasher.newSalt()
            val hash = PasswordHasher.hash(password, salt)
            val newId = userDao.insert(
                UserEntity(
                    username = cleanedUsername,
                    email = cleanedEmail,
                    passwordHash = hash,
                    passwordSalt = salt
                )
            )
            AuthResult.Success(newId, cleanedUsername)
        }

    /** Verify credentials and (on success) persist the session via [PreferencesManager]. */
    suspend fun login(username: String, password: String): AuthResult =
        withContext(Dispatchers.IO) {
            val cleaned = username.trim()
            if (cleaned.isEmpty() || password.isEmpty()) {
                return@withContext AuthResult.Failure(AuthResult.FailureReason.MISSING_FIELDS)
            }
            val user = userDao.findByUsername(cleaned)
                ?: return@withContext AuthResult.Failure(AuthResult.FailureReason.INVALID_CREDENTIALS)
            if (!PasswordHasher.verify(password, user.passwordSalt, user.passwordHash)) {
                return@withContext AuthResult.Failure(AuthResult.FailureReason.INVALID_CREDENTIALS)
            }
            // Accounts created before the PBKDF2 switch get upgraded the first time we see
            // the plaintext again. There's no other moment we can do it.
            if (PasswordHasher.needsRehash(user.passwordHash)) {
                val salt = PasswordHasher.newSalt()
                userDao.updatePassword(user.id, PasswordHasher.hash(password, salt), salt)
            }
            preferences.currentUserId = user.id
            preferences.currentUsername = user.username
            AuthResult.Success(user.id, user.username)
        }

    /** Drop session info — does not delete the account, just signs out. */
    fun logout() {
        preferences.clearSession()
    }

    /** Convenience used by the dashboard to greet the user. */
    suspend fun currentUser(): UserEntity? = withContext(Dispatchers.IO) {
        val id = preferences.currentUserId
        if (id <= 0L) null else userDao.findById(id)
    }

    /**
     * True when the persisted session points at a user that still exists. Prefs and the
     * database can drift apart (cleared storage, a partial Auto Backup restore); without
     * this check the app would open the library and crash on the first foreign-key insert.
     */
    suspend fun validateSession(): Boolean {
        if (!preferences.isLoggedIn) return false
        if (currentUser() != null) return true
        preferences.clearSession()
        return false
    }

    private fun validate(username: String, email: String, password: String): AuthResult.FailureReason? =
        when {
            username.length < 3 -> AuthResult.FailureReason.USERNAME_TOO_SHORT
            !EMAIL_PATTERN.matches(email) -> AuthResult.FailureReason.INVALID_EMAIL
            password.length < 6 -> AuthResult.FailureReason.PASSWORD_TOO_SHORT
            else -> null
        }

    companion object {
        // Pure-Kotlin email regex. Replaces android.util.Patterns.EMAIL_ADDRESS so JVM
        // unit tests don't NPE on the static-field access (the Android stub jar leaves
        // those fields null, and `isReturnDefaultValues` only handles methods).
        // Practical RFC 5322-ish — good enough to bounce obvious typos at the form level.
        private val EMAIL_PATTERN = Regex(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
        )
    }
}
