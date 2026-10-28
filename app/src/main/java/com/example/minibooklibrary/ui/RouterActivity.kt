package com.example.minibooklibrary.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.minibooklibrary.di.ServiceLocator
import com.example.minibooklibrary.ui.auth.AuthActivity
import com.example.minibooklibrary.ui.main.MainActivity
import com.example.minibooklibrary.ui.onboarding.OnboardingActivity
import kotlinx.coroutines.launch

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
        if (!ServiceLocator.providePreferences(this).onboardingCompleted) {
            open(OnboardingActivity::class.java)
            return
        }
        // One indexed lookup; the splash theme covers it.
        lifecycleScope.launch {
            val signedIn = ServiceLocator.provideUserRepository(this@RouterActivity).validateSession()
            open(if (signedIn) MainActivity::class.java else AuthActivity::class.java)
        }
    }

    private fun open(target: Class<out Activity>) {
        startActivity(Intent(this, target))
        finish()
    }
}
