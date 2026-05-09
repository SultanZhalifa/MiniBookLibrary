package com.example.minibooklibrary.ui.books.list

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.example.minibooklibrary.data.local.entity.BookEntity
import com.example.minibooklibrary.data.preferences.PreferencesManager
import com.example.minibooklibrary.data.repository.BookRepository
import com.example.minibooklibrary.domain.ReadingStatus
import com.example.minibooklibrary.domain.SortOrder
import com.example.minibooklibrary.domain.StatusFilter
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BookListViewModelTest {

    @get:Rule
    val instantExecutor = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(testDispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private val booksFlow = MutableStateFlow<List<BookEntity>>(emptyList())

    private val repo: BookRepository = mockk(relaxed = true) {
        every { observeBooks(any()) } returns booksFlow
    }

    // relaxed = true means property setters return Unit by default — no need to stub.
    private val prefs: PreferencesManager = mockk(relaxed = true) {
        every { currentUserId } returns 1L
        every { lastSortOrder } returns SortOrder.DATE_ADDED_DESC
        every { lastStatusFilter } returns StatusFilter.ALL
    }

    private fun newVm() = BookListViewModel(repo, prefs)

    private fun book(id: Long, title: String, status: ReadingStatus, rating: Float = 0f, added: Long = id) =
        BookEntity(
            id = id,
            userId = 1L,
            title = title,
            author = "A",
            category = "Fiction",
            year = 2024,
            readingStatus = status.storageValue,
            rating = rating,
            dateAdded = added,
            lastModified = added
        )

    @Test
    fun `empty state surfaces hasNoBooksAtAll`() = runTest(testDispatcher) {
        val vm = newVm()
        booksFlow.value = emptyList()
        vm.uiState.test {
            // Wait until non-loading state arrives.
            var state = awaitItem()
            while (state.isLoading) state = awaitItem()
            assertTrue(state.hasNoBooksAtAll)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `search filters by title and author`() = runTest(testDispatcher) {
        val vm = newVm()
        booksFlow.value = listOf(
            book(1L, "Atomic Habits", ReadingStatus.FINISHED),
            book(2L, "Deep Work", ReadingStatus.WANT_TO_READ)
        )
        vm.uiState.test {
            // Drain initial states.
            var state = awaitItem()
            while (state.isLoading) state = awaitItem()
            // Wait until the loaded books arrive (at least one item).
            while (state.books.size < 2) state = awaitItem()

            vm.setQuery("habit")
            // Look for the filtered emission.
            do { state = awaitItem() } while (state.query != "habit")
            assertEquals(1, state.books.size)
            assertEquals("Atomic Habits", state.books.first().title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `filter by reading status excludes others`() = runTest(testDispatcher) {
        val vm = newVm()
        booksFlow.value = listOf(
            book(1L, "Done", ReadingStatus.FINISHED),
            book(2L, "Reading", ReadingStatus.CURRENTLY_READING),
            book(3L, "Wishlist", ReadingStatus.WANT_TO_READ)
        )
        vm.uiState.test {
            var state = awaitItem()
            while (state.isLoading || state.books.size < 3) state = awaitItem()

            vm.setStatusFilter(StatusFilter.CURRENTLY_READING)
            do { state = awaitItem() } while (state.statusFilter != StatusFilter.CURRENTLY_READING)
            assertEquals(1, state.books.size)
            assertEquals("Reading", state.books.first().title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `sort by title A-Z is alphabetical case-insensitive`() = runTest(testDispatcher) {
        val vm = newVm()
        booksFlow.value = listOf(
            book(1L, "banana", ReadingStatus.WANT_TO_READ, added = 2L),
            book(2L, "Apple", ReadingStatus.WANT_TO_READ, added = 1L),
            book(3L, "cherry", ReadingStatus.WANT_TO_READ, added = 3L)
        )
        vm.uiState.test {
            var state = awaitItem()
            while (state.isLoading || state.books.size < 3) state = awaitItem()

            vm.setSortOrder(SortOrder.TITLE_ASC)
            do { state = awaitItem() } while (state.sortOrder != SortOrder.TITLE_ASC)
            assertEquals(listOf("Apple", "banana", "cherry"), state.books.map { it.title })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `sort and filter persist to preferences`() = runTest(testDispatcher) {
        val vm = newVm()
        vm.setSortOrder(SortOrder.RATING_DESC)
        vm.setStatusFilter(StatusFilter.FINISHED)
        verify { prefs.lastSortOrder = SortOrder.RATING_DESC }
        verify { prefs.lastStatusFilter = StatusFilter.FINISHED }
    }

    @Test
    fun `delete forwards to repository`() = runTest(testDispatcher) {
        val vm = newVm()
        val target = book(99L, "Doomed", ReadingStatus.FINISHED)
        coEvery { repo.delete(target) } returns 1
        vm.delete(target)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { repo.delete(target) }
    }

    @Test
    fun `restore re-adds previously deleted book`() = runTest(testDispatcher) {
        val vm = newVm()
        val target = book(99L, "Saved", ReadingStatus.FINISHED)
        coEvery { repo.add(target) } returns 99L
        vm.restore(target)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { repo.add(target) }
    }
}
