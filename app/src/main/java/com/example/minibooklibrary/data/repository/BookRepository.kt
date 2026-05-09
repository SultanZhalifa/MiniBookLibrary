package com.example.minibooklibrary.data.repository

import com.example.minibooklibrary.data.local.dao.BookDao
import com.example.minibooklibrary.data.local.entity.BookEntity
import com.example.minibooklibrary.domain.ReadingStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * The single point of entry for book persistence operations used by the ViewModels.
 *
 * The repository keeps reactive reads as cold [Flow]s emitted from Room and pushes
 * blocking writes onto [Dispatchers.IO]. Filtering, searching, and sorting happens in
 * the ViewModel layer so the SQL stays simple and Flow combinators stay testable.
 */
class BookRepository(private val bookDao: BookDao) {

    /** Reactive list of all books for [userId], sorted newest-first by `dateAdded`. */
    fun observeBooks(userId: Long): Flow<List<BookEntity>> =
        bookDao.observeAllForUser(userId)

    /** Reactive total count, used by the dashboard tile. */
    fun observeBookCount(userId: Long): Flow<Int> = bookDao.observeBookCount(userId)

    /** Reactive count of books in [status], used for dashboard progress bars. */
    fun observeStatusCount(userId: Long, status: ReadingStatus): Flow<Int> =
        bookDao.observeStatusCount(userId, status.storageValue)

    /** A small slice of recent books, used by the dashboard's "Recently added" rail. */
    fun observeRecentBooks(userId: Long, limit: Int = 5): Flow<List<BookEntity>> =
        bookDao.observeRecentForUser(userId, limit)

    /** One-shot list, useful for PDF/JSON export which doesn't need live updates. */
    suspend fun getBooks(userId: Long): List<BookEntity> = withContext(Dispatchers.IO) {
        bookDao.getAllForUser(userId)
    }

    suspend fun findById(id: Long): BookEntity? = withContext(Dispatchers.IO) {
        bookDao.findById(id)
    }

    suspend fun add(book: BookEntity): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        bookDao.insert(book.copy(dateAdded = now, lastModified = now))
    }

    suspend fun addAll(books: List<BookEntity>) = withContext(Dispatchers.IO) {
        bookDao.insertAll(books)
    }

    suspend fun update(book: BookEntity): Int = withContext(Dispatchers.IO) {
        bookDao.update(book.copy(lastModified = System.currentTimeMillis()))
    }

    suspend fun delete(book: BookEntity): Int = withContext(Dispatchers.IO) {
        bookDao.delete(book)
    }

    suspend fun deleteById(id: Long): Int = withContext(Dispatchers.IO) {
        bookDao.deleteById(id)
    }

    suspend fun deleteAllForUser(userId: Long): Int = withContext(Dispatchers.IO) {
        bookDao.deleteAllForUser(userId)
    }
}
