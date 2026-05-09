package com.example.minibooklibrary.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.minibooklibrary.data.local.entity.BookEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manual JSON serializer for backup/restore.
 *
 * Why hand-rolled JSON instead of kotlinx-serialization or Moshi: keeps the dependency
 * graph small for a portfolio app, and the schema is tiny. The file format is human
 * readable so users can inspect/edit it if they really need to.
 */
class BackupManager(private val context: Context) {

    /** Serialize [books] to a JSON file in Downloads and return its URI. */
    fun exportToJson(books: List<BookEntity>): Uri? {
        val payload = JSONObject().apply {
            put("schema", SCHEMA_VERSION)
            put("exportedAt", System.currentTimeMillis())
            put(
                "books",
                JSONArray().apply { books.forEach { put(it.toJson()) } }
            )
        }
        val fileName = "MiniBookLibrary_backup_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())}.json"
        return writeToDownloads(fileName, payload.toString(2))
    }

    /**
     * Read [uri] as JSON and convert it back into a list of books for [userId].
     * The IDs are reset to zero so that Room treats them as fresh inserts (avoids
     * collisions with the user's existing library).
     */
    fun importFromJson(uri: Uri, userId: Long): List<BookEntity>? {
        return try {
            val text = context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream)).readText()
            } ?: return null

            val root = JSONObject(text)
            val booksArray = root.getJSONArray("books")
            (0 until booksArray.length()).map { idx ->
                booksArray.getJSONObject(idx).toBook(userId)
            }
        } catch (t: Throwable) {
            null
        }
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
        // We deliberately skip coverImageUri — local file paths don't survive a transfer.
    }

    private fun JSONObject.toBook(userId: Long): BookEntity = BookEntity(
        id = 0L,
        userId = userId,
        title = optString("title", "Untitled"),
        author = optString("author", "Unknown"),
        category = optString("category", "Fiction"),
        year = optInt("year", 0),
        readingStatus = optString("readingStatus", "want_to_read"),
        rating = optDouble("rating", 0.0).toFloat(),
        coverImageUri = null,
        currentPage = optInt("currentPage", 0),
        totalPages = optInt("totalPages", 0),
        notes = optString("notes", ""),
        dateAdded = optLong("dateAdded", System.currentTimeMillis()),
        lastModified = optLong("lastModified", System.currentTimeMillis())
    )

    private fun writeToDownloads(fileName: String, content: String): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/json")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
            resolver.openOutputStream(uri)?.use { stream ->
                stream.write(content.toByteArray(Charsets.UTF_8))
            }
            uri
        } else {
            @Suppress("DEPRECATION")
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadDir.exists()) downloadDir.mkdirs()
            val file = File(downloadDir, fileName)
            FileOutputStream(file).use { it.write(content.toByteArray(Charsets.UTF_8)) }
            androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        }
    }

    companion object {
        private const val SCHEMA_VERSION = 1
    }
}
