package com.example.minibooklibrary.util

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Copies user-picked cover images into our app-private internal storage.
 *
 * Why we copy instead of saving the original `content://` URI: the gallery URI may grant
 * us only short-lived read permission (especially via Photo Picker). By copying the
 * bytes locally we get a stable, persistent path the RecyclerView can reload across
 * cold starts.
 */
object ImageStorage {

    private const val TAG = "ImageStorage"
    private const val DIRECTORY = "covers"

    /**
     * Copy [sourceUri] into internal storage and return the absolute file path. Returns
     * null on any I/O failure — callers should fall back to the placeholder cover.
     */
    fun saveCoverImage(context: Context, sourceUri: Uri): String? {
        return try {
            val coversDir = File(context.filesDir, DIRECTORY).apply { mkdirs() }
            val target = File(coversDir, "cover_${UUID.randomUUID()}.jpg")

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(target).use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            target.absolutePath
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to copy cover image from $sourceUri", t)
            null
        }
    }

    /** Best-effort delete; ignored if the file is already gone. */
    fun deleteCoverImage(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).delete() }
    }
}
