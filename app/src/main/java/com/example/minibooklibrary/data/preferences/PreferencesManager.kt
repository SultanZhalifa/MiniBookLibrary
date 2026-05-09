package com.example.minibooklibrary.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import com.example.minibooklibrary.domain.SortOrder
import com.example.minibooklibrary.domain.StatusFilter

/**
 * Strongly-typed wrapper around [SharedPreferences].
 *
 * Reasons to keep the wrapper instead of touching SharedPreferences directly:
 *  - all key strings live in one place (so a typo is a compile error, not a silent miss),
 *  - migrations across versions can happen here without leaking into UI code,
 *  - app-wide settings like theme and last sort order share the same backing file.
 */
class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Whether the user finished onboarding (false = first launch). */
    var onboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        set(value) = prefs.edit { putBoolean(KEY_ONBOARDING_DONE, value) }

    /** Whether a user is currently signed in. */
    val isLoggedIn: Boolean
        get() = currentUserId > 0

    /** Currently signed-in user's id, or 0 when nobody is signed in. */
    var currentUserId: Long
        get() = prefs.getLong(KEY_USER_ID, 0L)
        set(value) = prefs.edit { putLong(KEY_USER_ID, value) }

    var currentUsername: String?
        get() = prefs.getString(KEY_USERNAME, null)
        set(value) = prefs.edit { putString(KEY_USERNAME, value) }

    /**
     * App theme — one of [AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM],
     * [AppCompatDelegate.MODE_NIGHT_NO], [AppCompatDelegate.MODE_NIGHT_YES].
     */
    var themeMode: Int
        get() = prefs.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        set(value) = prefs.edit { putInt(KEY_THEME_MODE, value) }

    var lastSortOrder: SortOrder
        get() = SortOrder.fromStorageValue(prefs.getString(KEY_SORT_ORDER, null))
        set(value) = prefs.edit { putString(KEY_SORT_ORDER, value.storageValue) }

    var lastStatusFilter: StatusFilter
        get() = StatusFilter.fromStorageValue(prefs.getString(KEY_STATUS_FILTER, null))
        set(value) = prefs.edit { putString(KEY_STATUS_FILTER, value.storageValue) }

    var lastCategoryFilter: String?
        get() = prefs.getString(KEY_CATEGORY_FILTER, null)
        set(value) = prefs.edit { putString(KEY_CATEGORY_FILTER, value) }

    /**
     * Clear session-scoped state on logout but preserve onboarding/theme — the user
     * shouldn't have to re-do those after signing out.
     */
    fun clearSession() {
        prefs.edit {
            remove(KEY_USER_ID)
            remove(KEY_USERNAME)
        }
    }

    companion object {
        private const val PREFS_NAME = "mini_book_library_prefs"

        private const val KEY_ONBOARDING_DONE = "onboarding_completed"
        private const val KEY_USER_ID = "current_user_id"
        private const val KEY_USERNAME = "current_username"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_SORT_ORDER = "sort_order"
        private const val KEY_STATUS_FILTER = "status_filter"
        private const val KEY_CATEGORY_FILTER = "category_filter"
    }
}
