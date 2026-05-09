package com.example.minibooklibrary.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.minibooklibrary.domain.ReadingStatus

/**
 * A single book in a user's personal library.
 *
 * [coverImageUri] is a string-encoded `content://` or `file://` URI managed by
 * [com.example.minibooklibrary.util.ImageStorage] — the actual file lives in app-private
 * internal storage so it survives backup/restore cleanly.
 *
 * [readingStatus] is stored as a string ([ReadingStatus.storageValue]) rather than the
 * enum ordinal — see the docs on [ReadingStatus] for why.
 */
@Entity(
    tableName = "books",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId")]
)
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val userId: Long,
    val title: String,
    val author: String,
    val category: String,
    val year: Int,
    val readingStatus: String = ReadingStatus.WANT_TO_READ.storageValue,
    val rating: Float = 0f,
    val coverImageUri: String? = null,
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val notes: String = "",
    val dateAdded: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis()
)
