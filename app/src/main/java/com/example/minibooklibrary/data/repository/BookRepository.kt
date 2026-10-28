package com.example.minibooklibrary.data.repository

import com.example.minibooklibrary.data.local.dao.BookDao
import com.example.minibooklibrary.data.local.dao.ReadingLogDao
import com.example.minibooklibrary.data.local.entity.BookEntity
import com.example.minibooklibrary.data.local.entity.ReadingLogEntity
import com.example.minibooklibrary.domain.DailyPages
import com.example.minibooklibrary.domain.ReadingStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * The single point of entry for book persistence operations used by the ViewModels.
 *
 * The repository keeps reactive reads as cold [Flow]s emitted from Room and pushes
 * blocking writes onto [Dispatchers.IO]. Filtering, searching, and sorting happens in
 * the ViewModel layer so the SQL stays simple and Flow combinators stay testable.
 *
 * It also owns the reading log: any update that moves a book's current page forward is
 * recorded as pages read today, which is what the dashboard streak is built from.
 */
class BookRepository(
    private val bookDao: BookDao,
    private val readingLogDao: ReadingLogDao,
    private val now: () -> Long = System::currentTimeMillis,
    private val today: () -> LocalDate = LocalDate::now
) {

    /** Reactive list of all books for [userId], sorted newest-first by `dateAdded`. */
    fun observeBooks(userId: Long): Flow<List<BookEntity>> =
        bookDao.observeAllForUser(userId)

    /** A single book; emits null once it's deleted. */
    fun observeBook(id: Long): Flow<BookEntity?> = bookDao.observeById(id)

    /** Pages read per day, oldest first. Feeds the dashboard streak. */
    fun observeDailyPages(userId: Long): Flow<List<DailyPages>> =
        readingLogDao.observeDailyPages(userId)

    /** One-shot list, useful for PDF/JSON export which doesn't need live updates. */
    suspend fun getBooks(userId: Long): List<BookEntity> = withContext(Dispatchers.IO) {
        bookDao.getAllForUser(userId)
    }

    suspend fun getDailyPages(userId: Long): List<DailyPages> = withContext(Dispatchers.IO) {
        readingLogDao.getDailyPages(userId)
    }

    suspend fun findById(id: Long): BookEntity? = withContext(Dispatchers.IO) {
        bookDao.findById(id)
    }

    /**
     * Insert a new book. The starting page isn't logged as reading: someone adding a book
     * they're halfway through didn't read 200 pages today.
     */
    suspend fun add(book: BookEntity): Long = withContext(Dispatchers.IO) {
        val stamp = now()
        bookDao.insert(
            book.copy(
                dateAdded = stamp,
                lastModified = stamp,
                finishedAt = if (book.isFinished) stamp else null
            )
        )
    }

    /** Put a just-deleted book back exactly as it was (swipe-to-delete Undo). */
    suspend fun restore(book: BookEntity): Long = withContext(Dispatchers.IO) {
        bookDao.insert(book)
    }

    /** Bulk insert for backup restore. Timestamps from the file are kept as-is. */
    suspend fun addAll(books: List<BookEntity>, log: List<ReadingLogEntity> = emptyList()) =
        withContext(Dispatchers.IO) {
            bookDao.insertAll(books)
            if (log.isNotEmpty()) readingLogDao.insertAll(log)
        }

    /**
     * Save an edited book. Compares against what's stored to log forward page movement and
     * to stamp or clear [BookEntity.finishedAt] when the status changes.
     */
    suspend fun update(book: BookEntity): Int = withContext(Dispatchers.IO) {
        val previous = bookDao.findById(book.id)
        val stamp = now()
        val finishedAt = when {
            !book.isFinished -> null
            previous?.isFinished == true -> previous.finishedAt
            else -> stamp
        }
        val pagesRead = book.currentPage - (previous?.currentPage ?: book.currentPage)
        if (pagesRead > 0) {
            readingLogDao.insert(
                ReadingLogEntity(
                    userId = book.userId,
                    bookId = book.id,
                    epochDay = today().toEpochDay(),
                    pages = pagesRead
                )
            )
        }
        bookDao.update(book.copy(lastModified = stamp, finishedAt = finishedAt))
    }

    /** Quick progress update from the detail screen. */
    suspend fun updateProgress(book: BookEntity, currentPage: Int): Int =
        update(book.copy(currentPage = currentPage))

    suspend fun delete(book: BookEntity): Int = withContext(Dispatchers.IO) {
        bookDao.delete(book)
    }

    private val BookEntity.isFinished: Boolean
        get() = readingStatus == ReadingStatus.FINISHED.storageValue
}
