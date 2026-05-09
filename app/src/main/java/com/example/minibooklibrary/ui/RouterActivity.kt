package com.example.minibooklibrary.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.minibooklibrary.di.ServiceLocator
import com.example.minibooklibrary.ui.auth.AuthActivity
import com.example.minibooklibrary.ui.main.MainActivity
import com.example.minibooklibrary.ui.onboarding.OnboardingActivity

/**
 * Front-door activity that decides whether the user should land on Onboarding, Auth, or
 * straight into the Main app shell based on persisted state.
 *
 * Kept separate from the splash screen so that the launcher icon flow is one
 * discoverable place — much easier to reason about than branching in `onCreate` of a
 * conditional first screen.
 */
class RouterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = ServiceLocator.providePreferences(this)
        val target = when {
            !prefs.onboardingCompleted -> OnboardingActivity::class.java
            !prefs.isLoggedIn -> AuthActivity::class.java
            else -> MainActivity::class.java
        }
        startActivity(Intent(this, target))
        finish()
    }
}
