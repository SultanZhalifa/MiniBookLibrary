package com.example.minibooklibrary.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.minibooklibrary.data.local.entity.BookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(book: BookEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(books: List<BookEntity>)

    @Update
    suspend fun update(book: BookEntity): Int

    @Delete
    suspend fun delete(book: BookEntity): Int

    @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): BookEntity?

    @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<BookEntity?>

    @Query("SELECT * FROM books WHERE userId = :userId ORDER BY dateAdded DESC")
    fun observeAllForUser(userId: Long): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE userId = :userId ORDER BY dateAdded DESC")
    suspend fun getAllForUser(userId: Long): List<BookEntity>

    @Query("SELECT * FROM books WHERE userId = :userId AND readingStatus = 'currently_reading' ORDER BY lastModified DESC LIMIT 1")
    suspend fun getCurrentlyReadingBook(userId: Long): BookEntity?
}
