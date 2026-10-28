package com.example.minibooklibrary.ui.settings

import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minibooklibrary.R
import com.example.minibooklibrary.data.backup.BackupManager
import com.example.minibooklibrary.data.export.PdfExporter
import com.example.minibooklibrary.data.preferences.PreferencesManager
import com.example.minibooklibrary.data.repository.BookRepository
import com.example.minibooklibrary.data.repository.UserRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Settings tab ViewModel. Owns theme switching, PDF export, JSON backup/restore, and
 * account sign-out. Long-running disk work runs on [Dispatchers.IO].
 *
 * Export targets come from the Storage Access Framework (the fragment launches
 * CreateDocument with [suggestedFileName]), so nothing here needs a storage permission.
 */
class SettingsViewModel(
    private val bookRepository: BookRepository,
    private val userRepository: UserRepository,
    private val preferences: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            themeMode = preferences.themeMode,
            username = preferences.currentUsername.orEmpty()
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    fun setThemeMode(mode: Int) {
        preferences.themeMode = mode
        _uiState.value = _uiState.value.copy(themeMode = mode)
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    /** Default name offered in the system save dialog, e.g. `MiniBookLibrary_20261028_1412.pdf`. */
    fun suggestedFileName(extension: String, prefix: String = "MiniBookLibrary"): String =
        "${prefix}_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())}.$extension"

    fun exportPdf(context: Context, target: Uri) = runWork {
        val books = bookRepository.getBooks(preferences.currentUserId)
        val ok = withContext(Dispatchers.IO) {
            PdfExporter(context.applicationContext).export(target, books)
        }
        if (ok) SettingsEvent.Exported(target, MIME_PDF, R.string.snackbar_pdf_exported)
        else SettingsEvent.Error(R.string.settings_error_pdf)
    }

    fun exportBackup(context: Context, target: Uri) = runWork {
        val userId = preferences.currentUserId
        val books = bookRepository.getBooks(userId)
        val log = bookRepository.getDailyPages(userId)
        val ok = withContext(Dispatchers.IO) {
            BackupManager(context.applicationContext).exportToJson(target, books, log)
        }
        if (ok) SettingsEvent.Exported(target, MIME_JSON, R.string.snackbar_backup_exported)
        else SettingsEvent.Error(R.string.settings_error_backup)
    }

    fun importBackup(context: Context, source: Uri) = runWork {
        val userId = preferences.currentUserId
        val restored = withContext(Dispatchers.IO) {
            BackupManager(context.applicationContext).importFromJson(source, userId)
        }
        if (restored == null) {
            SettingsEvent.Error(R.string.settings_error_restore)
        } else {
            bookRepository.addAll(restored.books, restored.readingLog)
            SettingsEvent.RestoreCompleted(restored.books.size)
        }
    }

    fun logout() {
        userRepository.logout()
        viewModelScope.launch { _events.emit(SettingsEvent.LoggedOut) }
    }

    /** Run one export/import at a time, keep the spinner honest, and emit what [block] returns. */
    private fun runWork(block: suspend () -> SettingsEvent) {
        if (_uiState.value.isWorking) return
        _uiState.value = _uiState.value.copy(isWorking = true)
        viewModelScope.launch {
            val event = try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Room or the content resolver threw; report it instead of crashing the app.
                SettingsEvent.Error(R.string.settings_error_generic)
            } finally {
                _uiState.value = _uiState.value.copy(isWorking = false)
            }
            _events.emit(event)
        }
    }

    companion object {
        const val MIME_PDF = "application/pdf"
        const val MIME_JSON = "application/json"
    }
}

data class SettingsUiState(
    val themeMode: Int = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
    val username: String = "",
    val isWorking: Boolean = false
)

sealed class SettingsEvent {
    data class Exported(val uri: Uri, val mimeType: String, @StringRes val messageRes: Int) : SettingsEvent()
    data class RestoreCompleted(val importedCount: Int) : SettingsEvent()
    data class Error(@StringRes val messageRes: Int) : SettingsEvent()
    data object LoggedOut : SettingsEvent()
}
