package com.example.minibooklibrary.ui.settings

import android.content.Context
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minibooklibrary.data.preferences.PreferencesManager
import com.example.minibooklibrary.data.repository.BookRepository
import com.example.minibooklibrary.data.repository.UserRepository
import com.example.minibooklibrary.util.BackupManager
import com.example.minibooklibrary.util.PdfExporter
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

    fun exportPdf(context: Context) {
        if (_uiState.value.isWorking) return
        _uiState.value = _uiState.value.copy(isWorking = true)
        viewModelScope.launch {
            val books = bookRepository.getBooks(preferences.currentUserId)
            val uri = withContext(Dispatchers.IO) {
                PdfExporter(context.applicationContext).export(books)
            }
            _uiState.value = _uiState.value.copy(isWorking = false)
            _events.emit(
                if (uri != null) SettingsEvent.PdfReady(uri)
                else SettingsEvent.Error("Couldn't export the PDF — please try again")
            )
        }
    }

    fun exportBackup(context: Context) {
        if (_uiState.value.isWorking) return
        _uiState.value = _uiState.value.copy(isWorking = true)
        viewModelScope.launch {
            val books = bookRepository.getBooks(preferences.currentUserId)
            val uri = withContext(Dispatchers.IO) {
                BackupManager(context.applicationContext).exportToJson(books)
            }
            _uiState.value = _uiState.value.copy(isWorking = false)
            _events.emit(
                if (uri != null) SettingsEvent.BackupReady(uri)
                else SettingsEvent.Error("Couldn't write backup file")
            )
        }
    }

    fun importBackup(context: Context, source: Uri) {
        if (_uiState.value.isWorking) return
        _uiState.value = _uiState.value.copy(isWorking = true)
        viewModelScope.launch {
            val userId = preferences.currentUserId
            val result = withContext(Dispatchers.IO) {
                BackupManager(context.applicationContext).importFromJson(source, userId)
            }
            _uiState.value = _uiState.value.copy(isWorking = false)
            if (result == null) {
                _events.emit(SettingsEvent.Error("Backup file looks invalid"))
                return@launch
            }
            bookRepository.addAll(result)
            _events.emit(SettingsEvent.RestoreCompleted(result.size))
        }
    }

    fun logout() {
        userRepository.logout()
        viewModelScope.launch { _events.emit(SettingsEvent.LoggedOut) }
    }
}

data class SettingsUiState(
    val themeMode: Int = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
    val username: String = "",
    val isWorking: Boolean = false
)

sealed class SettingsEvent {
    data class PdfReady(val uri: Uri) : SettingsEvent()
    data class BackupReady(val uri: Uri) : SettingsEvent()
    data class RestoreCompleted(val importedCount: Int) : SettingsEvent()
    data class Error(val message: String) : SettingsEvent()
    data object LoggedOut : SettingsEvent()
}
