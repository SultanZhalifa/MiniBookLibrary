package com.example.minibooklibrary.data.local

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.minibooklibrary.data.local.dao.BookDao
import com.example.minibooklibrary.data.local.dao.ReadingLogDao
import com.example.minibooklibrary.data.local.dao.UserDao
import com.example.minibooklibrary.data.local.entity.BookEntity
import com.example.minibooklibrary.data.local.entity.ReadingLogEntity
import com.example.minibooklibrary.data.local.entity.UserEntity

/**
 * Room database for the app. Singleton-managed via [getInstance].
 *
 * Schema history (snapshots in `app/schemas`):
 *  - v1: users, books.
 *  - v2: `books.finishedAt` and the `reading_log` table for streaks. Purely additive, so
 *    Room generates the migration from the two snapshots.
 *
 * There is deliberately no `fallbackToDestructiveMigration()`. A missing migration should
 * crash in testing, not silently wipe someone's library on upgrade. `BookDatabaseMigrationTest`
 * opens every old version and migrates it forward.
 *
 * The pre-Room SQLite DB ([DATABASE_LEGACY_NAME]) is abandoned rather than migrated; it
 * lived in a different namespace and Backup/Restore covers anyone who needs that data.
 */
@Database(
    entities = [UserEntity::class, BookEntity::class, ReadingLogEntity::class],
    version = 2,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
    exportSchema = true
)
abstract class BookDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun bookDao(): BookDao
    abstract fun readingLogDao(): ReadingLogDao

    companion object {
        const val DATABASE_NAME = "mini_book_library.db"

        @Suppress("unused")
        const val DATABASE_LEGACY_NAME = "BookLibrary.db"

        @Volatile
        private var INSTANCE: BookDatabase? = null

        fun getInstance(context: Context): BookDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: build(context).also { INSTANCE = it }
            }

        private fun build(context: Context): BookDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                BookDatabase::class.java,
                DATABASE_NAME
            )
                .build()
    }
}
