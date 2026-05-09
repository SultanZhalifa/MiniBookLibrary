package com.example.minibooklibrary.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persisted user account.
 *
 * Passwords are stored as a SHA-256 hash with a per-record salt — see
 * [com.example.minibooklibrary.util.PasswordHasher]. This is far from production-grade
 * (a memory-hard KDF like Argon2 / bcrypt would be appropriate) but it removes the obvious
 * footgun of storing plaintext while keeping the dependency footprint small.
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
