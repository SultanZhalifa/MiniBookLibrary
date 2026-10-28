package com.example.minibooklibrary.util

import com.example.minibooklibrary.data.local.entity.BookEntity
import com.example.minibooklibrary.domain.DailyPages
import com.example.minibooklibrary.domain.ReadingStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupJsonTest {

    private val book = BookEntity(
        id = 42L,
        userId = 1L,
        title = "Deep Work",
        author = "Cal Newport",
        category = "Self-Help",
        year = 2016,
        readingStatus = ReadingStatus.FINISHED.storageValue,
        rating = 4.5f,
        coverImageUri = "/data/covers/x.jpg",
        currentPage = 296,
        totalPages = 296,
        notes = "Re-read chapter 2",
        dateAdded = 1_000L,
        lastModified = 2_000L,
        finishedAt = 3_000L
    )

    @Test
    fun `round trip keeps book fields and reading log`() {
        val json = BackupJson.encode(listOf(book), listOf(DailyPages(20_000L, 25)))
        val restored = BackupJson.decode(json, userId = 9L)

        val copy = restored.books.single()
        assertEquals(0L, copy.id) // fresh insert
        assertEquals(9L, copy.userId) // belongs to whoever restores it
        assertNull(copy.coverImageUri) // local paths don't travel
        assertEquals(book.copy(id = 0L, userId = 9L, coverImageUri = null), copy)

        val log = restored.readingLog.single()
        assertEquals(20_000L, log.epochDay)
        assertEquals(25, log.pages)
        assertEquals(9L, log.userId)
        assertNull(log.bookId)
    }

    @Test
    fun `schema 1 files without reading log still import`() {
        val v1 = """
            {"schema":1,"exportedAt":0,"books":[
              {"title":"Old","author":"A","category":"History","year":1999,
               "readingStatus":"want_to_read","rating":3.0,"currentPage":0,"totalPages":0,
               "notes":"","dateAdded":10,"lastModified":10}
            ]}
        """.trimIndent()
        val restored = BackupJson.decode(v1, userId = 1L)
        assertEquals("Old", restored.books.single().title)
        assertNull(restored.books.single().finishedAt)
        assertTrue(restored.readingLog.isEmpty())
    }

    @Test
    fun `hand-edited junk values are clamped to something the app can show`() {
        val edited = """
            {"books":[{"title":"","readingStatus":"reading??","rating":11,
                       "currentPage":900,"totalPages":300}]}
        """.trimIndent()
        val restored = BackupJson.decode(edited, userId = 1L).books.single()
        assertEquals("Untitled", restored.title)
        assertEquals(ReadingStatus.WANT_TO_READ.storageValue, restored.readingStatus)
        assertEquals(5f, restored.rating, 0f)
        assertEquals(300, restored.currentPage)
    }

    @Test(expected = Exception::class)
    fun `a file without a books array is rejected`() {
        BackupJson.decode("""{"hello":"world"}""", userId = 1L)
    }
}
