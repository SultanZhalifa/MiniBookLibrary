package com.example.minibooklibrary.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persisted user account.
 *
 * [passwordHash] is a self-describing PBKDF2 hash (or a legacy SHA-256 one that gets
 * upgraded on next login) and [passwordSalt] its per-user salt. See
 * [com.example.minibooklibrary.util.PasswordHasher].
 */
@Entity(
    tableName = "users",
    indices = [Index(value = ["username"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val username: String,
    val email: String,
    val passwordHash: String,
    val passwordSalt: String,
    val createdAt: Long = System.currentTimeMillis()
)
