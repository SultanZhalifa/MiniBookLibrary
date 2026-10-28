package com.example.minibooklibrary.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Opens a database at every exported schema version, fills it with data, and migrates it
 * to the current version. Room validates the result against the latest snapshot, so a
 * migration that leaves the schema even slightly off fails here instead of on a phone.
 *
 * Runs on Robolectric (real SQLite on the JVM) because CI only runs unit tests.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class BookDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        BookDatabase::class.java
    )

    @Test
    fun `v1 to v2 keeps existing books and adds reading stats columns`() {
        helper.createDatabase(DB_NAME, 1).apply {
            execSQL(
                "INSERT INTO users (id, username, email, passwordHash, passwordSalt, createdAt) " +
                    "VALUES (1, 'alice', 'a@example.com', 'hash', 'salt', 0)"
            )
            execSQL(
                "INSERT INTO books (id, userId, title, author, category, year, readingStatus, " +
                    "rating, coverImageUri, currentPage, totalPages, notes, dateAdded, lastModified) " +
                    "VALUES (10, 1, 'Dune', 'Frank Herbert', 'Fiction', 1965, 'finished', " +
                    "4.5, NULL, 0, 412, '', 100, 200)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(DB_NAME, 2, true)

        db.query("SELECT title, finishedAt FROM books WHERE id = 10").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Dune", cursor.getString(0))
            assertTrue(cursor.isNull(1))
        }
        db.execSQL("INSERT INTO reading_log (userId, bookId, epochDay, pages) VALUES (1, 10, 20000, 30)")
        db.query("SELECT COUNT(*) FROM reading_log").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
        }
    }

    private companion object {
        const val DB_NAME = "migration-test.db"
    }
}
