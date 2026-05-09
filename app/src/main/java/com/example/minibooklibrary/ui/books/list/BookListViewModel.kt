package com.example.minibooklibrary.ui.books.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minibooklibrary.data.local.entity.BookEntity
import com.example.minibooklibrary.data.preferences.PreferencesManager
import com.example.minibooklibrary.data.repository.BookRepository
import com.example.minibooklibrary.domain.ReadingStatus
import com.example.minibooklibrary.domain.SortOrder
import com.example.minibooklibrary.domain.StatusFilter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The library list ViewModel. Combines:
 *
 * 1. The reactive list of books from Room.
 * 2. The user's current search query (held in-memory; not persisted).
 * 3. The user's last-used sort order and status filter (persisted in [PreferencesManager]).
 *
 * Filtering and sorting are done in-memory rather than in SQL — the dataset is small
 * (personal library, expected < 1000 rows), and this keeps DAO surface area minimal
 * and the logic trivially unit-testable.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BookListViewModel(
    private val bookRepository: BookRepository,
    private val preferences: PreferencesManager
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _sortOrder = MutableStateFlow(preferences.lastSortOrder)
    val sortOrder: StateFlow<SortOrder> = _sortOrder

    private val _statusFilter = MutableStateFlow(preferences.lastStatusFilter)
    val statusFilter: StateFlow<StatusFilter> = _statusFilter

    private val userIdFlow = flowOf(preferences.currentUserId)

    val uiState: StateFlow<BookListUiState> = userIdFlow
        .flatMapLatest { userId ->
            if (userId <= 0L) flowOf(BookListUiState())
            else combine(
                bookRepository.observeBooks(userId),
                _query,
                _sortOrder,
                _statusFilter
            ) { allBooks, q, sort, filter ->
                val filtered = applyFilter(allBooks, filter)
                val searched = applySearch(filtered, q)
                val sorted = applySort(searched, sort)
                BookListUiState(
                    isLoading = false,
                    books = sorted,
                    totalUnfiltered = allBooks.size,
                    query = q,
                    sortOrder = sort,
                    statusFilter = filter
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = BookListUiState(isLoading = true)
        )

    fun setQuery(text: String) { _query.value = text }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
        preferences.lastSortOrder = order
    }

    fun setStatusFilter(filter: StatusFilter) {
        _statusFilter.value = filter
        preferences.lastStatusFilter = filter
    }

    /**
     * Soft delete used by swipe-to-dismiss in the list. We delete immediately and let the
     * Snackbar UNDO call back into [restore] — Room's reactive flow re-emits both ways.
     */
    fun delete(book: BookEntity) {
        viewModelScope.launch { bookRepository.delete(book) }
    }

    fun restore(book: BookEntity) {
        viewModelScope.launch { bookRepository.add(book) }
    }

    private fun applyFilter(
        books: List<BookEntity>,
        filter: StatusFilter
    ): List<BookEntity> = when (filter) {
        StatusFilter.ALL -> books
        else -> books.filter { it.readingStatus == filter.storageValue }
    }

    private fun applySearch(books: List<BookEntity>, q: String): List<BookEntity> {
        if (q.isBlank()) return books
        val needle = q.trim().lowercase()
        return books.filter {
            it.title.lowercase().contains(needle) ||
                it.author.lowercase().contains(needle) ||
                it.category.lowercase().contains(needle)
        }
    }

    private fun applySort(books: List<BookEntity>, sort: SortOrder): List<BookEntity> = when (sort) {
        SortOrder.DATE_ADDED_DESC -> books.sortedByDescending { it.dateAdded }
        SortOrder.DATE_ADDED_ASC -> books.sortedBy { it.dateAdded }
        SortOrder.TITLE_ASC -> books.sortedBy { it.title.lowercase() }
        SortOrder.TITLE_DESC -> books.sortedByDescending { it.title.lowercase() }
        SortOrder.RATING_DESC -> books.sortedByDescending { it.rating }
        SortOrder.RATING_ASC -> books.sortedBy { it.rating }
    }
}

data class BookListUiState(
    val isLoading: Boolean = false,
    val books: List<BookEntity> = emptyList(),
    val totalUnfiltered: Int = 0,
    val query: String = "",
    val sortOrder: SortOrder = SortOrder.DATE_ADDED_DESC,
    val statusFilter: StatusFilter = StatusFilter.ALL
) {
    val isEmpty: Boolean get() = !isLoading && books.isEmpty()
    val hasNoBooksAtAll: Boolean get() = isEmpty && totalUnfiltered == 0
    val hasNoMatchingResults: Boolean get() = isEmpty && totalUnfiltered > 0
    val statusForReading: ReadingStatus
        get() = ReadingStatus.fromStorageValue(statusFilter.storageValue)
}
