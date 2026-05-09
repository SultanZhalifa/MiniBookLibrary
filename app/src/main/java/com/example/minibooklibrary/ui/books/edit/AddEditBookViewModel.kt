package com.example.minibooklibrary.ui.books.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minibooklibrary.data.local.entity.BookEntity
import com.example.minibooklibrary.data.preferences.PreferencesManager
import com.example.minibooklibrary.data.remote.GoogleBooksService
import com.example.minibooklibrary.data.repository.BookRepository
import com.example.minibooklibrary.domain.ReadingStatus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Drives the Add/Edit Book screen. Holds the in-progress form state and runs validation
 * before round-tripping to [BookRepository]. Also brokers ISBN lookups against the
 * Google Books service so the user can auto-populate fields.
 */
class AddEditBookViewModel(
    private val bookRepository: BookRepository,
    private val preferences: PreferencesManager,
    private val googleBooksService: GoogleBooksService
) : ViewModel() {

    private val _form = MutableStateFlow(BookForm())
    val form: StateFlow<BookForm> = _form.asStateFlow()

    private val _validation = MutableStateFlow(ValidationErrors())
    val validation: StateFlow<ValidationErrors> = _validation.asStateFlow()

    private val _events = MutableSharedFlow<EditEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<EditEvent> = _events.asSharedFlow()

    private var editingId: Long = 0L
    private var existingDateAdded: Long = System.currentTimeMillis()

    /** Hydrate the form for an "edit existing book" flow. No-op if [bookId] is invalid. */
    fun loadForEdit(bookId: Long) {
        if (bookId <= 0L) return
        viewModelScope.launch {
            val existing = bookRepository.findById(bookId) ?: return@launch
            editingId = existing.id
            existingDateAdded = existing.dateAdded
            _form.value = BookForm(
                title = existing.title,
                author = existing.author,
                category = existing.category,
                year = existing.year.takeIf { it != 0 }?.toString().orEmpty(),
                readingStatus = ReadingStatus.fromStorageValue(existing.readingStatus),
                rating = existing.rating,
                currentPage = existing.currentPage.takeIf { it != 0 }?.toString().orEmpty(),
                totalPages = existing.totalPages.takeIf { it != 0 }?.toString().orEmpty(),
                notes = existing.notes,
                coverImageUri = existing.coverImageUri,
                isEditing = true
            )
        }
    }

    fun updateField(transform: BookForm.() -> BookForm) {
        _form.value = _form.value.transform()
    }

    /**
     * Look up [isbn] via Google Books and merge the result into the current form. We
     * prefer remote-supplied values for fields the user hasn't typed yet — never
     * overwrite a non-empty local value, since the user's typed input is intentional.
     */
    fun lookupIsbn(isbn: String) {
        if (_form.value.isLooking) return
        val cleaned = isbn.trim()
        if (cleaned.length < 10) {
            _events.tryEmit(EditEvent.LookupFailed("Enter a 10 or 13 digit ISBN"))
            return
        }
        _form.value = _form.value.copy(isLooking = true)
        viewModelScope.launch {
            val result = googleBooksService.lookupByIsbn(cleaned)
            if (result == null) {
                _form.value = _form.value.copy(isLooking = false)
                _events.tryEmit(EditEvent.LookupFailed("No book found for that ISBN"))
                return@launch
            }
            val current = _form.value
            _form.value = current.copy(
                isLooking = false,
                title = current.title.ifBlank { result.title },
                author = current.author.ifBlank { result.author },
                category = if (current.category == DEFAULT_CATEGORY && result.category.isNotBlank())
                    result.category
                else current.category,
                year = current.year.ifBlank { result.year.takeIf { it > 0 }?.toString().orEmpty() },
                totalPages = current.totalPages.ifBlank {
                    result.totalPages.takeIf { it > 0 }?.toString().orEmpty()
                }
            )
            _events.tryEmit(EditEvent.LookupSucceeded(result.title))
        }
    }

    /** Validate and persist. Emits [EditEvent.Saved] on success. */
    fun save() {
        val form = _form.value
        val errors = validate(form)
        _validation.value = errors
        if (!errors.isValid) return

        viewModelScope.launch {
            val userId = preferences.currentUserId
            val entity = BookEntity(
                id = editingId,
                userId = userId,
                title = form.title.trim(),
                author = form.author.trim(),
                category = form.category,
                year = form.year.toIntOrNull() ?: 0,
                readingStatus = form.readingStatus.storageValue,
                rating = form.rating,
                coverImageUri = form.coverImageUri,
                currentPage = form.currentPage.toIntOrNull() ?: 0,
                totalPages = form.totalPages.toIntOrNull() ?: 0,
                notes = form.notes.trim(),
                dateAdded = if (form.isEditing) existingDateAdded else System.currentTimeMillis(),
                lastModified = System.currentTimeMillis()
            )
            if (form.isEditing) bookRepository.update(entity) else bookRepository.add(entity)
            _events.emit(EditEvent.Saved(form.isEditing))
        }
    }

    private fun validate(form: BookForm): ValidationErrors {
        val titleError = if (form.title.isBlank()) "Title is required" else null
        val authorError = if (form.author.isBlank()) "Author is required" else null
        val yearError = when {
            form.year.isBlank() -> "Year is required"
            form.year.toIntOrNull() == null -> "Year must be a number"
            (form.year.toInt() !in 1000..(Calendar.getInstance().get(Calendar.YEAR) + 1)) ->
                "Enter a realistic year"
            else -> null
        }
        val pagesError = run {
            val current = form.currentPage.toIntOrNull() ?: 0
            val total = form.totalPages.toIntOrNull() ?: 0
            when {
                form.readingStatus == ReadingStatus.CURRENTLY_READING && total == 0 ->
                    "Set a total page count to track progress"
                current > total && total > 0 -> "Current page can't exceed total pages"
                else -> null
            }
        }
        return ValidationErrors(
            titleError = titleError,
            authorError = authorError,
            yearError = yearError,
            pagesError = pagesError
        )
    }
}

/**
 * The Add/Edit form state. Strings are kept raw so the UI can show partial input
 * (e.g. "20" while the user is still typing a year).
 */
/** Default category used when the user hasn't picked one — matches strings.xml order. */
internal const val DEFAULT_CATEGORY = "Fiction"

data class BookForm(
    val title: String = "",
    val author: String = "",
    val category: String = DEFAULT_CATEGORY,
    val year: String = "",
    val readingStatus: ReadingStatus = ReadingStatus.WANT_TO_READ,
    val rating: Float = 0f,
    val currentPage: String = "",
    val totalPages: String = "",
    val notes: String = "",
    val coverImageUri: String? = null,
    val isEditing: Boolean = false,
    val isLooking: Boolean = false
)

data class ValidationErrors(
    val titleError: String? = null,
    val authorError: String? = null,
    val yearError: String? = null,
    val pagesError: String? = null
) {
    val isValid: Boolean get() = listOfNotNull(
        titleError, authorError, yearError, pagesError
    ).isEmpty()
}

sealed class EditEvent {
    data class Saved(val wasEdit: Boolean) : EditEvent()
    data class LookupSucceeded(val title: String) : EditEvent()
    data class LookupFailed(val message: String) : EditEvent()
}
