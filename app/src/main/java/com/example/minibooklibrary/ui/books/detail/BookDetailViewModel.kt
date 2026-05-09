package com.example.minibooklibrary.ui.books.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minibooklibrary.data.local.entity.BookEntity
import com.example.minibooklibrary.data.repository.BookRepository
import com.example.minibooklibrary.util.ImageStorage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BookDetailViewModel(
    private val bookRepository: BookRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BookDetailUiState())
    val state: StateFlow<BookDetailUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<DetailEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<DetailEvent> = _events.asSharedFlow()

    fun load(bookId: Long) {
        viewModelScope.launch {
            val book = bookRepository.findById(bookId)
            _state.value = if (book != null) {
                BookDetailUiState(book = book)
            } else {
                BookDetailUiState(notFound = true)
            }
        }
    }

    /**
     * Confirm-and-delete handler used by the detail screen. Cleans up the cover image
     * file too so we don't leak storage when a book is removed.
     */
    fun delete() {
        val current = _state.value.book ?: return
        viewModelScope.launch {
            ImageStorage.deleteCoverImage(current.coverImageUri)
            bookRepository.delete(current)
            _events.emit(DetailEvent.Deleted)
        }
    }
}

data class BookDetailUiState(
    val book: BookEntity? = null,
    val notFound: Boolean = false
)

sealed class DetailEvent {
    data object Deleted : DetailEvent()
}
