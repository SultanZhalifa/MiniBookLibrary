package com.example.minibooklibrary.domain

/**
 * Result of a successful ISBN lookup. Field-for-field matches the editable parts of the
 * Add/Edit form so the ViewModel can splat it onto [com.example.minibooklibrary.ui.books.edit.BookForm]
 * with a single `copy(...)`.
 */
data class BookLookup(
    val title: String,
    val author: String,
    val category: String,
    val year: Int,
    val totalPages: Int
)
