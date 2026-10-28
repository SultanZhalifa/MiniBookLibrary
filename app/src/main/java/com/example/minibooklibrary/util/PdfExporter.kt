package com.example.minibooklibrary.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.TextPaint
import android.text.TextUtils
import android.util.Log
import com.example.minibooklibrary.R
import com.example.minibooklibrary.data.local.entity.BookEntity
import com.example.minibooklibrary.domain.ReadingStatus
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/**
 * Renders the user's library to a PDF using the platform [PdfDocument] API — no
 * third-party PDF library required.
 *
 * Layout: a single-column table per page with a header strip, padded rows, and a footer
 * showing page numbers. Pages are sized to US Letter at 72 DPI for predictable rendering.
 *
 * The destination is a URI the user picked with the Storage Access Framework, which works
 * the same on every API level without a storage permission.
 */
class PdfExporter(private val context: Context) {

    /** Render [books] into [target]. Returns false if the file couldn't be written. */
    fun export(target: Uri, books: List<BookEntity>): Boolean {
        val document = PdfDocument()
        return try {
            renderPages(document, books)
            context.contentResolver.openOutputStream(target, "wt")?.use { document.writeTo(it) }
                ?: error("No output stream for $target")
            true
        } catch (t: Exception) {
            Log.w(TAG, "PDF export failed", t)
            false
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
        val contentWidth = pageWidth - margin * 2
        val timestamp = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date())

        val totalPages = if (books.isEmpty()) 1 else (books.size + maxRowsPerPage - 1) / maxRowsPerPage

        for (pageIndex in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            // Header
            canvas.drawText(context.getString(R.string.app_name), margin, margin + 10f, titlePaint)
            val subtitle = context.getString(
                R.string.pdf_subtitle,
                timestamp,
                context.resources.getQuantityString(R.plurals.pdf_book_count, books.size, books.size)
            )
            canvas.drawText(subtitle, margin, margin + 30f, subtitlePaint)
            canvas.drawLine(margin, margin + 50f, pageWidth - margin, margin + 50f, dividerPaint)

            if (books.isEmpty()) {
                canvas.drawText(context.getString(R.string.pdf_empty), margin, margin + 90f, rowMetaPaint)
            } else {
                val start = pageIndex * maxRowsPerPage
                val end = minOf(start + maxRowsPerPage, books.size)
                var y = margin + 80f
                for (i in start until end) {
                    val book = books[i]
                    val statusLabel = context.getString(
                        ReadingStatus.fromStorageValue(book.readingStatus).labelRes
                    )
                    val ratingLabel = if (book.rating > 0f) {
                        String.format(Locale.getDefault(), "★ %.1f", book.rating)
                    } else {
                        context.getString(R.string.rating_unrated)
                    }
                    val dateLabel = context.getString(R.string.detail_added_on, book.dateAdded.formatDate())
                    val tail = "${book.category} · $statusLabel · $ratingLabel · $dateLabel"

                    // PdfDocument doesn't wrap or clip, so long titles would run off the page.
                    canvas.drawText(
                        fit("${i + 1}. ${book.title}", rowTitlePaint, contentWidth),
                        margin, y, rowTitlePaint
                    )
                    canvas.drawText(
                        fit(context.getString(R.string.book_by_author, book.author), rowMetaPaint, contentWidth),
                        margin, y + 16f, rowMetaPaint
                    )
                    canvas.drawText(fit(tail, rowMetaPaint, contentWidth), margin, y + 32f, rowMetaPaint)
                    canvas.drawLine(margin, y + 44f, pageWidth - margin, y + 44f, dividerPaint)
                    y += rowHeight
                }
            }

            // Footer
            val footer = context.getString(R.string.pdf_page_footer, pageIndex + 1, totalPages)
            val footerWidth = subtitlePaint.measureText(footer)
            canvas.drawText(footer, pageWidth - margin - footerWidth, pageHeight - margin / 2f, subtitlePaint)
            document.finishPage(page)
        }
    }

    private fun fit(text: String, paint: Paint, width: Float): String =
        TextUtils.ellipsize(text, TextPaint(paint), width, TextUtils.TruncateAt.END).toString()

    private companion object {
        const val TAG = "PdfExporter"
    }
}
