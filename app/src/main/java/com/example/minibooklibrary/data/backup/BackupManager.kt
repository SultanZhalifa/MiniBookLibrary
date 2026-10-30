package com.example.minibooklibrary.data.backup

import android.content.Context
import android.net.Uri
import com.example.minibooklibrary.data.local.entity.BookEntity
import com.example.minibooklibrary.domain.DailyPages

/**
 * JSON backup/restore. File I/O goes through URIs from the Storage Access Framework, so
 * the user picks where the file lives and no storage permission is needed on any API level.
 *
 * Why hand-rolled JSON instead of kotlinx-serialization or Moshi: keeps the dependency
 * graph small for a portfolio app, and the schema is tiny. The file format is human
 * readable so users can inspect/edit it if they really need to. The format itself lives
 * in [BackupJson] so it can be unit-tested without a Context.
 */
class BackupManager(private val context: Context) {

    /** Write the backup to [target]. Returns false if the file couldn't be written. */
    fun exportToJson(target: Uri, books: List<BookEntity>, readingLog: List<DailyPages>): Boolean =
        runCatching {
            context.contentResolver.openOutputStream(target, "wt")?.use { stream ->
                stream.write(BackupJson.encode(books, readingLog).toByteArray(Charsets.UTF_8))
            } ?: error("No output stream for $target")
        }.isSuccess

    /** Read [source] and convert it into rows for [userId]. Null if the file isn't a backup. */
    fun importFromJson(source: Uri, userId: Long): BackupJson.Restored? =
        runCatching {
            val text = context.contentResolver.openInputStream(source)?.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).readText()
            } ?: return null
            BackupJson.decode(text, userId)
        }.getOrNull()
}
