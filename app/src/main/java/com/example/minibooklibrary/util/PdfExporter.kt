package com.example.minibooklibrary.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.minibooklibrary.data.local.entity.BookEntity
import com.example.minibooklibrary.domain.ReadingStatus
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Renders the user's library to a PDF using the platform [PdfDocument] API — no
 * third-party PDF library required.
 *
 * Layout: a single-column table per page with a header strip, padded rows, and a footer
 * showing page numbers. Pages are sized to US Letter at 72 DPI for predictable rendering.
 *
 * Output goes through MediaStore on Q+ (no permission needed) and falls back to the
 * legacy public Downloads dir on older devices.
 */
class PdfExporter(private val context: Context) {

    /** Render [books] to a PDF and return the public URI of the resulting file. */
    fun export(books: List<BookEntity>): Uri? {
        val document = PdfDocument()
        try {
            renderPages(document, books)
            val fileName = "MiniBookLibrary_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())}.pdf"
            return writeToDownloads(fileName, document)
        } finally {
            document.close()
        }
    }

    private fun renderPages(document: PdfDocument, books: List<BookEntity>) {
        val titlePaint = Paint().apply {
            color = Color.parseColor("#1F2937")
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val subtitlePaint = Paint().apply {
            color = Color.parseColor("#6B7280")
            textSize = 12f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }
        val rowTitlePaint = Paint().apply {
            color = Color.parseColor("#1F2937")
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val rowMetaPaint = Paint().apply {
            color = Color.parseColor("#374151")
            textSize = 11f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }
        val dividerPaint = Paint().apply {
            color = Color.parseColor("#E5E7EB")
            strokeWidth = 0.5f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        val pageWidth = 612 // 8.5in @ 72dpi
        val pageHeight = 792 // 11in @ 72dpi
        val margin = 40f
        val rowHeight = 64f
        val maxRowsPerPage = ((pageHeight - margin * 2 - 100f) / rowHeight).toInt()

        val totalPages = if (books.isEmpty()) 1 else (books.size + maxRowsPerPage - 1) / maxRowsPerPage

        for (pageIndex in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            // Header
            canvas.drawText("Mini Book Library", margin, margin + 10f, titlePaint)
            val timestamp = SimpleDateFormat("MMM d, yyyy 'at' HH:mm", Locale.getDefault()).format(Date())
            canvas.drawText("Exported on $timestamp · ${books.size} books", margin, margin + 30f, subtitlePaint)
            canvas.drawLine(margin, margin + 50f, pageWidth - margin, margin + 50f, dividerPaint)

            if (books.isEmpty()) {
                canvas.drawText("No books to export.", margin, margin + 90f, rowMetaPaint)
            } else {
                val start = pageIndex * maxRowsPerPage
                val end = minOf(start + maxRowsPerPage, books.size)
                var y = margin + 80f
                for (i in start until end) {
                    val book = books[i]
                    val statusLabel = ReadingStatus.fromStorageValue(book.readingStatus).displayLabel
                    val ratingLabel = if (book.rating > 0f) "★ %.1f".format(book.rating) else "Unrated"
                    val dateLabel = book.dateAdded.formatDate("MMM d, yyyy")

                    canvas.drawText("${i + 1}. ${book.title}", margin, y, rowTitlePaint)
                    canvas.drawText("by ${book.author}", margin, y + 16f, rowMetaPaint)
                    val tail = "${book.category} · $statusLabel · $ratingLabel · Added $dateLabel"
                    canvas.drawText(tail, margin, y + 32f, rowMetaPaint)
                    canvas.drawLine(margin, y + 44f, pageWidth - margin, y + 44f, dividerPaint)
                    y += rowHeight
                }
            }

            // Footer
            val footer = "Page ${pageIndex + 1} of $totalPages"
            canvas.drawText(footer, pageWidth - margin - 80f, pageHeight - margin / 2f, subtitlePaint)
            document.finishPage(page)
        }
    }

    private fun writeToDownloads(fileName: String, document: PdfDocument): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
            resolver.openOutputStream(uri)?.use { stream ->
                document.writeTo(stream)
            }
            uri
        } else {
            @Suppress("DEPRECATION")
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadDir.exists()) downloadDir.mkdirs()
            val file = File(downloadDir, fileName)
            FileOutputStream(file).use { stream -> document.writeTo(stream) }
            androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        }
    }
}
