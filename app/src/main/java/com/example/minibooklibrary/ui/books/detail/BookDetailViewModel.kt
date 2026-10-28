package com.example.minibooklibrary.ui.books.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minibooklibrary.data.local.entity.BookEntity
import com.example.minibooklibrary.data.repository.BookRepository
import com.example.minibooklibrary.util.ImageStorage
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Backs the detail screen. Observes the book rather than loading it once, so coming back
 * from Edit or logging progress shows the new values without a manual refresh.
 */
class BookDetailViewModel(
    private val bookRepository: BookRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BookDetailUiState())
    val state: StateFlow<BookDetailUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<DetailEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<DetailEvent> = _events.asSharedFlow()

    private var observeJob: Job? = null
    private var observedId = 0L
    private var deleting = false

    fun load(bookId: Long) {
        // Called from onViewCreated, so it runs again after returning from Edit. The
        // observation is already live; don't restart it.
        if (bookId == observedId && observeJob != null) return
        observedId = bookId
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            bookRepository.observeBook(bookId).collect { book ->
                // Our own delete also makes the row disappear; that's handled by the
                // Deleted event, not reported as "not found".
                if (book == null && deleting) return@collect
                _state.value = if (book != null) {
                    BookDetailUiState(book = book)
                } else {
                    BookDetailUiState(notFound = true)
                }
            }
        }
    }

    /** Log a new current page. Values outside 0..totalPages are clamped. */
    fun updateProgress(page: Int) {
        val current = _state.value.book ?: return
        val clamped = if (current.totalPages > 0) page.coerceIn(0, current.totalPages) else maxOf(0, page)
        if (clamped == current.currentPage) return
        viewModelScope.launch {
            bookRepository.updateProgress(current, clamped)
            _events.emit(DetailEvent.ProgressUpdated)
        }
    }

    /**
     * Confirm-and-delete handler used by the detail screen. Cleans up the cover image
     * file too so we don't leak storage when a book is removed.
     */
    fun delete() {
        val current = _state.value.book ?: return
        deleting = true
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
    data object ProgressUpdated : DetailEvent()
}
