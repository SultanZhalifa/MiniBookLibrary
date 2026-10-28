package com.example.minibooklibrary.ui.dashboard

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minibooklibrary.R
import com.example.minibooklibrary.data.local.entity.BookEntity
import com.example.minibooklibrary.data.preferences.PreferencesManager
import com.example.minibooklibrary.data.repository.BookRepository
import com.example.minibooklibrary.domain.ReadingStats
import com.example.minibooklibrary.domain.ReadingStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Surfaces the data shown on the Dashboard tab: totals, status breakdown, reading streak,
 * and the recently added strip. Everything is derived from two Room flows (books and the
 * daily reading log) and recomputed when either changes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val bookRepository: BookRepository,
    private val preferences: PreferencesManager,
    private val today: () -> LocalDate = LocalDate::now,
    private val zone: () -> ZoneId = ZoneId::systemDefault
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
        bookRepository.observeDailyPages(userId)
    ) { books, dailyPages ->
        val date = today()
        val ratedBooks = books.filter { it.rating > 0f }
        DashboardUiState(
            isLoading = false,
            totalBooks = books.size,
            wantToReadCount = books.countStatus(ReadingStatus.WANT_TO_READ),
            currentlyReadingCount = books.countStatus(ReadingStatus.CURRENTLY_READING),
            finishedCount = books.countStatus(ReadingStatus.FINISHED),
            averageRating = if (ratedBooks.isEmpty()) 0f else ratedBooks.map { it.rating }.average().toFloat(),
            // observeBooks is already newest-first.
            recentBooks = books.take(RECENT_LIMIT),
            username = preferences.currentUsername.orEmpty(),
            reading = ReadingStats.from(dailyPages, date),
            year = date.year,
            finishedThisYear = ReadingStats.finishedInYear(books.map { it.finishedAt }, date.year, zone())
        )
    }

    /** Greeting for the header, picked by hour of day. */
    @get:StringRes
    val greetingRes: Int
        get() = when (LocalTime.now().hour) {
            in 5..11 -> R.string.greeting_morning
            in 12..16 -> R.string.greeting_afternoon
            in 17..20 -> R.string.greeting_evening
            else -> R.string.greeting_night
        }

    private fun List<BookEntity>.countStatus(status: ReadingStatus) =
        count { it.readingStatus == status.storageValue }

    private companion object {
        const val RECENT_LIMIT = 5
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
    val recentBooks: List<BookEntity> = emptyList(),
    val reading: ReadingStats = ReadingStats(),
    val year: Int = 0,
    val finishedThisYear: Int = 0
) {
    /** Percentages for the progress bars; sum may be < 100 when there are no books. */
    val wantToReadPercent: Int
        get() = if (totalBooks == 0) 0 else (wantToReadCount * 100 / totalBooks)
    val currentlyReadingPercent: Int
        get() = if (totalBooks == 0) 0 else (currentlyReadingCount * 100 / totalBooks)
    val finishedPercent: Int
        get() = if (totalBooks == 0) 0 else (finishedCount * 100 / totalBooks)
}
