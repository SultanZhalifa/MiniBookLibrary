package com.example.minibooklibrary.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One chunk of reading: [pages] read on [epochDay] (days since 1970-01-01, local time).
 * The dashboard streak and weekly chart are computed from these rows.
 *
 * [bookId] is informational and deliberately has no foreign key. Deleting a book
 * shouldn't rewrite history and break a streak, and rows restored from a backup don't
 * map to a book id at all, so it's null for those.
 */
@Entity(
    tableName = "reading_log",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId", "epochDay"])]
)
data class ReadingLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val userId: Long,
    val bookId: Long?,
    val epochDay: Long,
    val pages: Int
)
