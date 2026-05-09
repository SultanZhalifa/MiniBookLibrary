package com.example.minibooklibrary.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.minibooklibrary.databinding.ActivityAuthBinding
import com.example.minibooklibrary.ui.common.ViewModelFactory
import com.example.minibooklibrary.ui.main.MainActivity
import kotlinx.coroutines.launch

/**
 * Hosts the login + register Navigation graph. Owns the [AuthViewModel] so both
 * fragments share its state — the form is a single workflow split across two destinations.
 */
class AuthActivity : AppCompatActivity() {

    val viewModel: AuthViewModel by viewModels { ViewModelFactory(this) }

    private lateinit var binding: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Listen for session-level events: a successful login navigates to MainActivity
        // and tears down the auth back-stack so Back doesn't return here.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    if (event is AuthEvent.LoggedIn) {
                        startActivity(Intent(this@AuthActivity, MainActivity::class.java))
                        finish()
                    }
                }
            }
        }
    }
}
