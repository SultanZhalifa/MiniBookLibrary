package com.example.minibooklibrary.di

import android.content.Context
import com.example.minibooklibrary.data.local.BookDatabase
import com.example.minibooklibrary.data.preferences.PreferencesManager
import com.example.minibooklibrary.data.remote.GoogleBooksService
import com.example.minibooklibrary.data.repository.BookRepository
import com.example.minibooklibrary.data.repository.UserRepository

/**
 * Tiny hand-rolled DI container.
 *
 * Hilt would be a more conventional choice but is overkill for a single-module app — and
 * showing a clear, pluggable container in source is more interview-friendly than KAPT
 * generating it for us. All instances are lazy and process-singletons.
 */
object ServiceLocator {

    @Volatile
    private var preferences: PreferencesManager? = null

    @Volatile
    private var database: BookDatabase? = null

    @Volatile
    private var userRepository: UserRepository? = null

    @Volatile
    private var bookRepository: BookRepository? = null

    @Volatile
    private var googleBooksService: GoogleBooksService? = null

    fun providePreferences(context: Context): PreferencesManager =
        preferences ?: synchronized(this) {
            preferences ?: PreferencesManager(context.applicationContext).also { preferences = it }
        }

    fun provideDatabase(context: Context): BookDatabase =
        database ?: synchronized(this) {
            database ?: BookDatabase.getInstance(context.applicationContext).also { database = it }
        }

    fun provideUserRepository(context: Context): UserRepository =
        userRepository ?: synchronized(this) {
            userRepository ?: UserRepository(
                userDao = provideDatabase(context).userDao(),
                preferences = providePreferences(context)
            ).also { userRepository = it }
        }

    fun provideBookRepository(context: Context): BookRepository =
        bookRepository ?: synchronized(this) {
            bookRepository ?: BookRepository(provideDatabase(context).bookDao())
                .also { bookRepository = it }
        }

    fun provideGoogleBooksService(): GoogleBooksService =
        googleBooksService ?: synchronized(this) {
            googleBooksService ?: GoogleBooksService().also { googleBooksService = it }
        }

    /** Test-only escape hatch: swap in test-specific instances, reset between cases. */
    fun resetForTests(
        preferences: PreferencesManager? = null,
        userRepository: UserRepository? = null,
        bookRepository: BookRepository? = null,
        googleBooksService: GoogleBooksService? = null
    ) {
        synchronized(this) {
            this.preferences = preferences
            this.userRepository = userRepository
            this.bookRepository = bookRepository
            this.googleBooksService = googleBooksService
            this.database = null
        }
    }
}
