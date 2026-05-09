package com.example.minibooklibrary.ui.books.edit

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.example.minibooklibrary.data.local.entity.BookEntity
import com.example.minibooklibrary.data.preferences.PreferencesManager
import com.example.minibooklibrary.data.remote.GoogleBooksService
import com.example.minibooklibrary.data.repository.BookRepository
import com.example.minibooklibrary.domain.BookLookup
import com.example.minibooklibrary.domain.ReadingStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class AddEditBookViewModelTest {

    @get:Rule
    val instantExecutor = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    @Before fun setUp() = Dispatchers.setMain(testDispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private val repo: BookRepository = mockk(relaxed = true)
    private val prefs: PreferencesManager = mockk {
        every { currentUserId } returns 1L
    }
    private val googleBooks: GoogleBooksService = mockk(relaxed = true)

    private fun newVm() = AddEditBookViewModel(repo, prefs, googleBooks)

    @Test
    fun `validation fails when title is blank`() = runTest(testDispatcher) {
        val vm = newVm()
        vm.updateField { copy(title = "", author = "Me", year = "2024") }
        vm.save()
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(vm.validation.value.titleError)
        assertFalse(vm.validation.value.isValid)
    }

    @Test
    fun `validation fails for year out of range`() = runTest(testDispatcher) {
        val futureYear = Calendar.getInstance().get(Calendar.YEAR) + 5
        val vm = newVm()
        vm.updateField { copy(title = "T", author = "A", year = futureYear.toString()) }
        vm.save()
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(vm.validation.value.yearError)
    }

    @Test
    fun `progress validation requires total pages when currently reading`() = runTest(testDispatcher) {
        val vm = newVm()
        vm.updateField {
            copy(
                title = "T",
                author = "A",
                year = "2024",
                readingStatus = ReadingStatus.CURRENTLY_READING,
                currentPage = "10",
                totalPages = "0"
            )
        }
        vm.save()
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(vm.validation.value.pagesError)
    }

    @Test
    fun `progress validation rejects current greater than total`() = runTest(testDispatcher) {
        val vm = newVm()
        vm.updateField {
            copy(
                title = "T",
                author = "A",
                year = "2024",
                readingStatus = ReadingStatus.CURRENTLY_READING,
                currentPage = "300",
                totalPages = "200"
            )
        }
        vm.save()
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(vm.validation.value.pagesError)
    }

    @Test
    fun `valid form persists a new book and emits Saved event`() = runTest(testDispatcher) {
        coEvery { repo.add(any()) } returns 1L
        val vm = newVm()
        vm.updateField {
            copy(
                title = "Pragmatic Programmer",
                author = "Andy Hunt",
                category = "Tech",
                year = "1999",
                readingStatus = ReadingStatus.FINISHED,
                rating = 4.5f
            )
        }

        vm.events.test {
            vm.save()
            testDispatcher.scheduler.advanceUntilIdle()
            val event = awaitItem()
            assertTrue(event is EditEvent.Saved)
            assertFalse((event as EditEvent.Saved).wasEdit)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify {
            repo.add(match<BookEntity> {
                it.title == "Pragmatic Programmer" &&
                    it.author == "Andy Hunt" &&
                    it.userId == 1L &&
                    it.readingStatus == ReadingStatus.FINISHED.storageValue &&
                    it.rating == 4.5f
            })
        }
    }

    @Test
    fun `loadForEdit hydrates form and marks isEditing true`() = runTest(testDispatcher) {
        val existing = BookEntity(
            id = 7L,
            userId = 1L,
            title = "Existing",
            author = "Author",
            category = "History",
            year = 2010,
            readingStatus = ReadingStatus.FINISHED.storageValue,
            rating = 3.0f
        )
        coEvery { repo.findById(7L) } returns existing
        val vm = newVm()
        vm.loadForEdit(7L)
        testDispatcher.scheduler.advanceUntilIdle()
        val form = vm.form.value
        assertTrue(form.isEditing)
        assertEquals("Existing", form.title)
        assertEquals(ReadingStatus.FINISHED, form.readingStatus)
        assertEquals(3.0f, form.rating, 0.0f)
    }

    @Test
    fun `lookupIsbn populates blank fields without overwriting user input`() = runTest(testDispatcher) {
        coEvery { googleBooks.lookupByIsbn("9780201616224") } returns BookLookup(
            title = "Remote Title",
            author = "Remote Author",
            category = "Tech",
            year = 1999,
            totalPages = 320
        )
        val vm = newVm()
        // User has already typed a custom title — must NOT be overwritten.
        vm.updateField { copy(title = "My Custom Title") }
        vm.lookupIsbn("9780201616224")
        testDispatcher.scheduler.advanceUntilIdle()

        val form = vm.form.value
        assertEquals("My Custom Title", form.title) // preserved
        assertEquals("Remote Author", form.author)  // populated
        assertEquals("Tech", form.category)         // overrode the default-only "Fiction"
        assertEquals("1999", form.year)
        assertEquals("320", form.totalPages)
        assertFalse(form.isLooking)
    }

    @Test
    fun `lookupIsbn surfaces a failure event when service returns null`() = runTest(testDispatcher) {
        coEvery { googleBooks.lookupByIsbn(any()) } returns null
        val vm = newVm()
        vm.events.test {
            vm.lookupIsbn("9780000000000")
            testDispatcher.scheduler.advanceUntilIdle()
            val event = awaitItem()
            assertTrue(event is EditEvent.LookupFailed)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `lookupIsbn rejects too-short input without hitting the network`() = runTest(testDispatcher) {
        val vm = newVm()
        vm.events.test {
            vm.lookupIsbn("123")
            val event = awaitItem()
            assertTrue(event is EditEvent.LookupFailed)
            cancelAndIgnoreRemainingEvents()
        }
        // Service must not have been called for an obviously invalid ISBN.
        coVerify(exactly = 0) { googleBooks.lookupByIsbn(any()) }
    }
}
