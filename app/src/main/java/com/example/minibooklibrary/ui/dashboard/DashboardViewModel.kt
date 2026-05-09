package com.example.minibooklibrary.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minibooklibrary.data.local.entity.BookEntity
import com.example.minibooklibrary.data.preferences.PreferencesManager
import com.example.minibooklibrary.data.repository.BookRepository
import com.example.minibooklibrary.domain.ReadingStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Surfaces the data shown on the Dashboard tab: total count, status breakdown, recent
 * book strip, and the highlight rating average. All values are derived from a single
 * [BookRepository] [kotlinx.coroutines.flow.Flow] and updated reactively.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val bookRepository: BookRepository,
    private val preferences: PreferencesManager
) : ViewModel() {

    private val userIdFlow = flowOf(preferences.currentUserId)

    val uiState: StateFlow<DashboardUiState> = userIdFlow
        .flatMapLatest { userId ->
            if (userId <= 0L) flowOf(DashboardUiState())
            else combineDashboard(userId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = DashboardUiState(isLoading = true)
        )

    private fun combineDashboard(userId: Long) = combine(
        bookRepository.observeBooks(userId),
        bookRepository.observeRecentBooks(userId, limit = 5),
        bookRepository.observeStatusCount(userId, ReadingStatus.WANT_TO_READ),
        bookRepository.observeStatusCount(userId, ReadingStatus.CURRENTLY_READING),
        bookRepository.observeStatusCount(userId, ReadingStatus.FINISHED)
    ) { books, recent, wantToRead, reading, finished ->
        DashboardUiState(
            isLoading = false,
            totalBooks = books.size,
            wantToReadCount = wantToRead,
            currentlyReadingCount = reading,
            finishedCount = finished,
            averageRating = books.filter { it.rating > 0f }
                .map { it.rating }
                .average()
                .takeIf { !it.isNaN() }?.toFloat() ?: 0f,
            recentBooks = recent,
            username = preferences.currentUsername.orEmpty()
        )
    }

    /** Helper used by the welcome card to greet by time of day. */
    val timeOfDayGreeting: String
        get() {
            val hour = java.util.Calendar.getInstance()
                .get(java.util.Calendar.HOUR_OF_DAY)
            return when (hour) {
                in 5..11 -> "Good morning"
                in 12..16 -> "Good afternoon"
                in 17..20 -> "Good evening"
                else -> "Good night"
            }
        }
}

data class DashboardUiState(
    val isLoading: Boolean = false,
    val username: String = "",
    val totalBooks: Int = 0,
    val wantToReadCount: Int = 0,
    val currentlyReadingCount: Int = 0,
    val finishedCount: Int = 0,
    val averageRating: Float = 0f,
    val recentBooks: List<BookEntity> = emptyList()
) {
    /** Percentages for the progress bars; sum may be < 100 when there are no books. */
    val wantToReadPercent: Int
        get() = if (totalBooks == 0) 0 else (wantToReadCount * 100 / totalBooks)
    val currentlyReadingPercent: Int
        get() = if (totalBooks == 0) 0 else (currentlyReadingCount * 100 / totalBooks)
    val finishedPercent: Int
        get() = if (totalBooks == 0) 0 else (finishedCount * 100 / totalBooks)
}

private fun List<Float>.average(): Double = if (isEmpty()) Double.NaN
else sum() / size.toDouble()
