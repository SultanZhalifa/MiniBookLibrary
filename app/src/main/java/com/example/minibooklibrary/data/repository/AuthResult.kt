package com.example.minibooklibrary.data.repository

/**
 * Result type for authentication operations. Modeled as a sealed class so the UI can
 * branch on outcome without depending on exception types.
 */
sealed class AuthResult {
    data class Success(val userId: Long, val username: String) : AuthResult()
    data class Failure(val reason: FailureReason, val message: String) : AuthResult()

    enum class FailureReason {
        USERNAME_TAKEN,
        INVALID_CREDENTIALS,
        VALIDATION,
        UNKNOWN
    }
}
