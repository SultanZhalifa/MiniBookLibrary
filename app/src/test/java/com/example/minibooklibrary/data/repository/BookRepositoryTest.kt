package com.example.minibooklibrary.data.repository

import com.example.minibooklibrary.data.local.dao.BookDao
import com.example.minibooklibrary.data.local.entity.BookEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BookRepositoryTest {

    private val dao: BookDao = mockk(relaxed = true)
    private val repo = BookRepository(dao)

    @Test
    fun `add stamps fresh dateAdded and lastModified`() = runTest {
        val captured = slot<BookEntity>()
        coEvery { dao.insert(capture(captured)) } returns 1L
        val before = System.currentTimeMillis()
        repo.add(
            BookEntity(
                userId = 1L,
                title = "Test",
                author = "Me",
                category = "Fiction",
                year = 2024,
                dateAdded = 0L,
                lastModified = 0L
            )
        )
        val saved = captured.captured
        assertNotNull(saved)
        // The repo refreshes both timestamps to "now". Allow a small delta.
        assertTrue(saved.dateAdded >= before)
        assertTrue(saved.lastModified >= before)
        assertEquals(saved.dateAdded, saved.lastModified)
    }

    @Test
    fun `update refreshes lastModified but keeps dateAdded`() = runTest {
        val original = BookEntity(
            id = 5L,
            userId = 1L,
            title = "Test",
            author = "Me",
            category = "Fiction",
            year = 2024,
            dateAdded = 1_000L,
            lastModified = 1_000L
        )
        val captured = slot<BookEntity>()
        coEvery { dao.update(capture(captured)) } returns 1
        repo.update(original)
        assertEquals(1_000L, captured.captured.dateAdded)
        assertTrue(captured.captured.lastModified >= 1_000L)
    }

    @Test
    fun `delete forwards to dao`() = runTest {
        val book = BookEntity(
            id = 1L,
            userId = 1L,
            title = "T",
            author = "A",
            category = "Fiction",
            year = 2024
        )
        coEvery { dao.delete(book) } returns 1
        repo.delete(book)
        coVerify { dao.delete(book) }
    }
}
