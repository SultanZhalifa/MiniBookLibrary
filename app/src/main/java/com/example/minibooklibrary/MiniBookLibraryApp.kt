package com.example.minibooklibrary

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.minibooklibrary.di.ServiceLocator

/**
 * Application entry point. Used to apply the persisted theme mode early so that the
 * first activity doesn't flash the wrong palette.
 */
class MiniBookLibraryApp : Application() {

    override fun onCreate() {
        super.onCreate()
        applyPersistedTheme()
    }

    private fun applyPersistedTheme() {
        val mode = ServiceLocator.providePreferences(this).themeMode
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}
