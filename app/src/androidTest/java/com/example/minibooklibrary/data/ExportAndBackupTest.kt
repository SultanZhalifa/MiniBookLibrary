package com.example.minibooklibrary.data

import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.minibooklibrary.data.backup.BackupManager
import com.example.minibooklibrary.data.export.PdfExporter
import com.example.minibooklibrary.data.local.entity.BookEntity
import com.example.minibooklibrary.domain.DailyPages
import com.example.minibooklibrary.domain.ReadingStatus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Exercises the real export and backup I/O on a device: content-resolver streams, the
 * platform PdfDocument, and org.json as Android ships it. The JSON format itself is unit
 * tested in BackupJsonTest; this covers the parts a JVM test can't.
 *
 * The system file picker is out of scope. A file:// Uri goes through the same
 * ContentResolver calls the picker's content:// Uri does.
 */
@RunWith(AndroidJUnit4::class)
class ExportAndBackupTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val dir = File(context.cacheDir, "export-test").apply { mkdirs() }

    @After
    fun cleanUp() {
        dir.deleteRecursively()
    }

    @Test
    fun backupWrittenToAUriRestoresTheSameBooksAndReadingLog() {
        val target = Uri.fromFile(File(dir, "backup.json"))
        val books = sampleBooks(3)
        val log = listOf(DailyPages(epochDay = 20_000L, pages = 25), DailyPages(20_001L, 40))

        assertTrue(BackupManager(context).exportToJson(target, books, log))
        val restored = BackupManager(context).importFromJson(target, userId = 7L)

        assertNotNull(restored)
        assertEquals(books.map { it.copy(id = 0L, userId = 7L) }, restored!!.books)
        assertEquals(listOf(25, 40), restored.readingLog.map { it.pages })
    }

    @Test
    fun importingSomethingThatIsNotABackupReturnsNull() {
        val file = File(dir, "notes.json").apply { writeText("""{"hello":"world"}""") }
        assertEquals(null, BackupManager(context).importFromJson(Uri.fromFile(file), userId = 1L))
    }

    @Test
    fun pdfExportPaginatesNineBooksPerPage() {
        val file = File(dir, "library.pdf")

        assertTrue(PdfExporter(context).export(Uri.fromFile(file), sampleBooks(20)))

        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { pdf -> assertEquals(3, pdf.pageCount) }
        }
    }

    @Test
    fun pdfExportOfAnEmptyLibraryStillProducesOnePage() {
        val file = File(dir, "empty.pdf")

        assertTrue(PdfExporter(context).export(Uri.fromFile(file), emptyList()))

        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { pdf -> assertEquals(1, pdf.pageCount) }
        }
    }

    private fun sampleBooks(count: Int) = (1..count).map { n ->
        BookEntity(
            id = n.toLong(),
            userId = 1L,
            // Long enough to need ellipsizing in the PDF.
            title = "Book $n with a title long enough to run past the right edge of the page",
            author = "Author $n",
            category = "Fiction",
            year = 2000 + n,
            readingStatus = ReadingStatus.FINISHED.storageValue,
            rating = 4f,
            currentPage = 100,
            totalPages = 100,
            notes = "",
            dateAdded = 1_000L * n,
            lastModified = 2_000L * n,
            finishedAt = 3_000L * n
        )
    }
}
