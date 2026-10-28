package com.example.minibooklibrary.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
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
 *
 * The copy is downscaled to [MAX_EDGE_PX] and re-encoded as JPEG. A phone photo is
 * 4-8 MB; a cover never renders bigger than the detail header, and the widget has to
 * push its bitmap through a binder transaction with a ~1 MB limit.
 */
object ImageStorage {

    private const val TAG = "ImageStorage"
    private const val DIRECTORY = "covers"
    private const val MAX_EDGE_PX = 1200
    private const val JPEG_QUALITY = 85

    /**
     * Copy [sourceUri] into internal storage and return the absolute file path. Returns
     * null on any I/O failure — callers should fall back to the placeholder cover.
     * Does disk and decode work; call it off the main thread.
     */
    fun saveCoverImage(context: Context, sourceUri: Uri): String? {
        return try {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(sourceUri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                ?: return null
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, MAX_EDGE_PX)
            }
            val sampled = resolver.openInputStream(sourceUri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: return null
            val orientation = resolver.openInputStream(sourceUri)?.use {
                ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
            val bitmap = scaleAndRotate(sampled, orientation)

            val coversDir = File(context.filesDir, DIRECTORY).apply { mkdirs() }
            val target = File(coversDir, "cover_${UUID.randomUUID()}.jpg")
            FileOutputStream(target).use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
            bitmap.recycle()
            target.absolutePath
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to copy cover image from $sourceUri", t)
            null
        }
    }

    /**
     * Decode a stored cover small enough for [maxEdgePx]. Used by the home-screen widget,
     * which can't hand a full-size bitmap to the launcher.
     */
    fun loadThumbnail(path: String, maxEdgePx: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxEdgePx)
        }
        return BitmapFactory.decodeFile(path, options)
    }

    /** Best-effort delete; ignored if the file is already gone. */
    fun deleteCoverImage(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).delete() }
    }

    /** Largest power of two that keeps the long edge at or above [maxEdge]. */
    internal fun sampleSizeFor(width: Int, height: Int, maxEdge: Int): Int {
        var sample = 1
        while (maxOf(width, height) / (sample * 2) >= maxEdge) sample *= 2
        return sample
    }

    private fun scaleAndRotate(source: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        val longEdge = maxOf(source.width, source.height)
        if (longEdge > MAX_EDGE_PX) {
            val scale = MAX_EDGE_PX.toFloat() / longEdge
            matrix.postScale(scale, scale)
        }
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        }
        if (matrix.isIdentity) return source
        val result = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        if (result !== source) source.recycle()
        return result
    }
}
