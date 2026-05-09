package com.example.minibooklibrary.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.minibooklibrary.data.local.dao.BookDao
import com.example.minibooklibrary.data.local.dao.UserDao
import com.example.minibooklibrary.data.local.entity.BookEntity
import com.example.minibooklibrary.data.local.entity.UserEntity

/**
 * Room database for the app. Singleton-managed via [getInstance].
 *
 * Schema migrations: we ship as v1. The legacy SQLite DB ([DATABASE_LEGACY_NAME]) is
 * abandoned rather than migrated — it lived in a different namespace, and the v2 schema
 * has too many shape changes for an ALTER TABLE migration to be a clean win. Users
 * upgrading will see an empty library; the Settings screen offers Backup/Restore for
 * anyone who needs to bring data forward.
 */
@Database(
    entities = [UserEntity::class, BookEntity::class],
    version = 1,
    // Schema export emits CI-time JSON snapshots to track migrations. Off for now to keep
    // the build noise-free; flip on (and configure room.schemaLocation) when you need to
    // ship a v2 schema with a Migration.
    exportSchema = false
)
abstract class BookDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun bookDao(): BookDao

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
                .fallbackToDestructiveMigration()
                .build()
    }
}
