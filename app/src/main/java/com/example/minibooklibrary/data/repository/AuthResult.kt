package com.example.minibooklibrary.data.repository

/**
 * Result type for authentication operations. Modeled as a sealed class so the UI can
 * branch on outcome without depending on exception types.
 *
 * Failures carry a reason rather than a message: the repository doesn't know the user's
 * language, so the UI maps each reason to a string resource and to the field it belongs to.
 */
sealed class AuthResult {
    data class Success(val userId: Long, val username: String) : AuthResult()
    data class Failure(val reason: FailureReason) : AuthResult()

    enum class FailureReason {
        MISSING_FIELDS,
        USERNAME_TOO_SHORT,
        INVALID_EMAIL,
        PASSWORD_TOO_SHORT,
        USERNAME_TAKEN,
        INVALID_CREDENTIALS
    }
}
