package com.example.minibooklibrary.ui.common

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.minibooklibrary.di.ServiceLocator
import com.example.minibooklibrary.ui.auth.AuthViewModel
import com.example.minibooklibrary.ui.books.detail.BookDetailViewModel
import com.example.minibooklibrary.ui.books.edit.AddEditBookViewModel
import com.example.minibooklibrary.ui.books.list.BookListViewModel
import com.example.minibooklibrary.ui.dashboard.DashboardViewModel
import com.example.minibooklibrary.ui.settings.SettingsViewModel

/**
 * Hand-rolled [ViewModelProvider.Factory] backed by [ServiceLocator]. Lets every
 * ViewModel keep its constructor honest about its real dependencies, instead of pulling
 * a SavedStateHandle "blob".
 */
class ViewModelFactory(context: Context) : ViewModelProvider.Factory {

    private val app = context.applicationContext
    private val userRepo = ServiceLocator.provideUserRepository(app)
    private val bookRepo = ServiceLocator.provideBookRepository(app)
    private val prefs = ServiceLocator.providePreferences(app)
    private val googleBooks = ServiceLocator.provideGoogleBooksService()

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(AuthViewModel::class.java) ->
            AuthViewModel(userRepo) as T
        modelClass.isAssignableFrom(DashboardViewModel::class.java) ->
            DashboardViewModel(bookRepo, prefs) as T
        modelClass.isAssignableFrom(BookListViewModel::class.java) ->
            BookListViewModel(bookRepo, prefs) as T
        modelClass.isAssignableFrom(BookDetailViewModel::class.java) ->
            BookDetailViewModel(bookRepo) as T
        modelClass.isAssignableFrom(AddEditBookViewModel::class.java) ->
            AddEditBookViewModel(bookRepo, prefs, googleBooks) as T
        modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
            SettingsViewModel(bookRepo, userRepo, prefs) as T
        else -> error("Unknown ViewModel class: ${modelClass.name}")
    }
}
