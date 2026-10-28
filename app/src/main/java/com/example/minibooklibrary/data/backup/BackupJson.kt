package com.example.minibooklibrary.data.backup

import com.example.minibooklibrary.data.local.entity.BookEntity
import com.example.minibooklibrary.data.local.entity.ReadingLogEntity
import com.example.minibooklibrary.domain.DailyPages
import com.example.minibooklibrary.domain.ReadingStatus
import org.json.JSONArray
import org.json.JSONObject

/**
 * The backup file format.
 *
 *  - Schema 1: `{ schema, exportedAt, books: [...] }`
 *  - Schema 2: adds `finishedAt` per book and a top-level `readingLog` of
 *    `{ epochDay, pages }` so streaks survive a restore. Schema 1 files still import.
 */
object BackupJson {

    const val SCHEMA_VERSION = 2

    data class Restored(val books: List<BookEntity>, val readingLog: List<ReadingLogEntity>)

    fun encode(
        books: List<BookEntity>,
        readingLog: List<DailyPages>,
        exportedAt: Long = System.currentTimeMillis()
    ): String = JSONObject().apply {
        put("schema", SCHEMA_VERSION)
        put("exportedAt", exportedAt)
        put("books", JSONArray().apply { books.forEach { put(it.toJson()) } })
        put("readingLog", JSONArray().apply {
            readingLog.forEach { day ->
                put(JSONObject().put("epochDay", day.epochDay).put("pages", day.pages))
            }
        })
    }.toString(2)

    /**
     * Parse a backup into fresh rows for [userId]. IDs are reset to zero so Room treats
     * them as new inserts and they merge into the existing library instead of colliding.
     * Throws on anything that isn't a backup file; the caller turns that into an error.
     */
    fun decode(text: String, userId: Long): Restored {
        val root = JSONObject(text)
        val booksArray = root.getJSONArray("books")
        val books = (0 until booksArray.length()).map { booksArray.getJSONObject(it).toBook(userId) }
        val logArray = root.optJSONArray("readingLog") ?: JSONArray()
        val log = (0 until logArray.length()).mapNotNull { index ->
            val entry = logArray.getJSONObject(index)
            val pages = entry.optInt("pages", 0)
            if (pages <= 0 || !entry.has("epochDay")) return@mapNotNull null
            ReadingLogEntity(
                userId = userId,
                bookId = null,
                epochDay = entry.getLong("epochDay"),
                pages = pages
            )
        }
        return Restored(books, log)
    }

    private fun BookEntity.toJson(): JSONObject = JSONObject().apply {
        put("title", title)
        put("author", author)
        put("category", category)
        put("year", year)
        put("readingStatus", readingStatus)
        put("rating", rating.toDouble())
        put("currentPage", currentPage)
        put("totalPages", totalPages)
        put("notes", notes)
        put("dateAdded", dateAdded)
        put("lastModified", lastModified)
        finishedAt?.let { put("finishedAt", it) }
        // We deliberately skip coverImageUri — local file paths don't survive a transfer.
    }

    private fun JSONObject.toBook(userId: Long): BookEntity {
        val now = System.currentTimeMillis()
        val totalPages = optInt("totalPages", 0).coerceAtLeast(0)
        val currentPage = optInt("currentPage", 0).coerceAtLeast(0)
        return BookEntity(
            id = 0L,
            userId = userId,
            title = optString("title").ifBlank { "Untitled" },
            author = optString("author").ifBlank { "Unknown" },
            category = optString("category").ifBlank { "Fiction" },
            year = optInt("year", 0),
            // Unknown values from a hand-edited file fall back instead of creating a
            // status the UI can't display.
            readingStatus = ReadingStatus.fromStorageValue(optString("readingStatus")).storageValue,
            rating = optDouble("rating", 0.0).toFloat().coerceIn(0f, 5f),
            coverImageUri = null,
            currentPage = if (totalPages > 0) currentPage.coerceAtMost(totalPages) else currentPage,
            totalPages = totalPages,
            notes = optString("notes", ""),
            dateAdded = optLong("dateAdded", now),
            lastModified = optLong("lastModified", now),
            finishedAt = if (has("finishedAt")) optLong("finishedAt") else null
        )
    }
}
