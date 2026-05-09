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

            when (val validation = validate(cleanedUsername, cleanedEmail, password)) {
                is ValidationResult.Invalid -> return@withContext AuthResult.Failure(
                    AuthResult.FailureReason.VALIDATION,
                    validation.message
                )
                ValidationResult.Valid -> Unit
            }

            if (userDao.countByUsername(cleanedUsername) > 0) {
                return@withContext AuthResult.Failure(
                    AuthResult.FailureReason.USERNAME_TAKEN,
                    "That username is already taken"
                )
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
                return@withContext AuthResult.Failure(
                    AuthResult.FailureReason.VALIDATION,
                    "Username and password are required"
                )
            }
            val user = userDao.findByUsername(cleaned)
                ?: return@withContext AuthResult.Failure(
                    AuthResult.FailureReason.INVALID_CREDENTIALS,
                    "Invalid username or password"
                )
            val ok = PasswordHasher.verify(password, user.passwordSalt, user.passwordHash)
            if (!ok) {
                AuthResult.Failure(
                    AuthResult.FailureReason.INVALID_CREDENTIALS,
                    "Invalid username or password"
                )
            } else {
                preferences.currentUserId = user.id
                preferences.currentUsername = user.username
                AuthResult.Success(user.id, user.username)
            }
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

    private sealed class ValidationResult {
        data object Valid : ValidationResult()
        data class Invalid(val message: String) : ValidationResult()
    }

    private fun validate(username: String, email: String, password: String): ValidationResult {
        if (username.length < 3) return ValidationResult.Invalid("Username must be at least 3 characters")
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return ValidationResult.Invalid("Please enter a valid email address")
        }
        if (password.length < 6) return ValidationResult.Invalid("Password must be at least 6 characters")
        return ValidationResult.Valid
    }
}
