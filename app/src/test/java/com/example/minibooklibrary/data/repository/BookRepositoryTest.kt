package com.example.minibooklibrary.data.repository

import com.example.minibooklibrary.data.local.dao.BookDao
import com.example.minibooklibrary.data.local.dao.ReadingLogDao
import com.example.minibooklibrary.data.local.entity.BookEntity
import com.example.minibooklibrary.data.local.entity.ReadingLogEntity
import com.example.minibooklibrary.domain.ReadingStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class BookRepositoryTest {

    private val dao: BookDao = mockk(relaxed = true)
    private val logDao: ReadingLogDao = mockk(relaxed = true)
    private val today = LocalDate.of(2026, 10, 28)
    private val repo = BookRepository(dao, logDao, now = { NOW }, today = { today })

    private fun book(
        id: Long = 5L,
        status: ReadingStatus = ReadingStatus.CURRENTLY_READING,
        currentPage: Int = 0,
        finishedAt: Long? = null
    ) = BookEntity(
        id = id,
        userId = 1L,
        title = "Test",
        author = "Me",
        category = "Fiction",
        year = 2024,
        readingStatus = status.storageValue,
        currentPage = currentPage,
        totalPages = 300,
        dateAdded = 1_000L,
        lastModified = 1_000L,
        finishedAt = finishedAt
    )

    @Test
    fun `add stamps fresh dateAdded and lastModified`() = runTest {
        val captured = slot<BookEntity>()
        coEvery { dao.insert(capture(captured)) } returns 1L
        repo.add(book(id = 0L).copy(dateAdded = 0L, lastModified = 0L))
        assertEquals(NOW, captured.captured.dateAdded)
        assertEquals(NOW, captured.captured.lastModified)
        assertNull(captured.captured.finishedAt)
    }

    @Test
    fun `adding a book that is already finished stamps finishedAt`() = runTest {
        val captured = slot<BookEntity>()
        coEvery { dao.insert(capture(captured)) } returns 1L
        repo.add(book(id = 0L, status = ReadingStatus.FINISHED))
        assertEquals(NOW, captured.captured.finishedAt)
    }

    @Test
    fun `adding a book never logs its starting page as reading`() = runTest {
        repo.add(book(id = 0L, currentPage = 150))
        coVerify(exactly = 0) { logDao.insert(any()) }
    }

    @Test
    fun `restore inserts the book unchanged`() = runTest {
        val original = book(currentPage = 40)
        repo.restore(original)
        coVerify { dao.insert(original) }
    }

    @Test
    fun `update refreshes lastModified but keeps dateAdded`() = runTest {
        coEvery { dao.findById(5L) } returns book()
        val captured = slot<BookEntity>()
        coEvery { dao.update(capture(captured)) } returns 1
        repo.update(book())
        assertEquals(1_000L, captured.captured.dateAdded)
        assertEquals(NOW, captured.captured.lastModified)
    }

    @Test
    fun `moving the current page forward logs the difference for today`() = runTest {
        coEvery { dao.findById(5L) } returns book(currentPage = 40)
        val entry = slot<ReadingLogEntity>()
        coEvery { logDao.insert(capture(entry)) } returns 1L

        repo.updateProgress(book(currentPage = 40), 72)

        assertEquals(32, entry.captured.pages)
        assertEquals(today.toEpochDay(), entry.captured.epochDay)
        assertEquals(5L, entry.captured.bookId)
        assertEquals(1L, entry.captured.userId)
    }

    @Test
    fun `moving the current page backwards logs nothing`() = runTest {
        coEvery { dao.findById(5L) } returns book(currentPage = 80)
        repo.updateProgress(book(currentPage = 80), 60)
        coVerify(exactly = 0) { logDao.insert(any()) }
    }

    @Test
    fun `finishing a book stamps finishedAt once and keeps it on later edits`() = runTest {
        val captured = slot<BookEntity>()
        coEvery { dao.update(capture(captured)) } returns 1

        coEvery { dao.findById(5L) } returns book()
        repo.update(book(status = ReadingStatus.FINISHED))
        assertEquals(NOW, captured.captured.finishedAt)

        coEvery { dao.findById(5L) } returns book(status = ReadingStatus.FINISHED, finishedAt = 500L)
        repo.update(book(status = ReadingStatus.FINISHED, finishedAt = 500L))
        assertEquals(500L, captured.captured.finishedAt)
    }

    @Test
    fun `moving a book out of finished clears finishedAt`() = runTest {
        coEvery { dao.findById(5L) } returns book(status = ReadingStatus.FINISHED, finishedAt = 500L)
        val captured = slot<BookEntity>()
        coEvery { dao.update(capture(captured)) } returns 1
        repo.update(book(status = ReadingStatus.CURRENTLY_READING, finishedAt = 500L))
        assertNull(captured.captured.finishedAt)
    }

    @Test
    fun `delete forwards to dao`() = runTest {
        val target = book()
        coEvery { dao.delete(target) } returns 1
        repo.delete(target)
        coVerify { dao.delete(target) }
    }

    private companion object {
        const val NOW = 1_700_000_000_000L
    }
}
