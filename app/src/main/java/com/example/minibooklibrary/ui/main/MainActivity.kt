package com.example.minibooklibrary.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.minibooklibrary.R
import com.example.minibooklibrary.databinding.ActivityMainBinding
import com.example.minibooklibrary.ui.RouterActivity

/**
 * Single-Activity host for the post-login app. Holds three top-level destinations
 * (Dashboard, Library, Settings) wired through a [com.google.android.material.bottomnavigation.BottomNavigationView]
 * and a NavController. Detail / Add-Edit screens push onto the same stack but hide
 * the bottom nav so the user feels like they descended to a sub-screen.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHost = supportFragmentManager
            .findFragmentById(R.id.main_nav_host) as NavHostFragment
        navController = navHost.navController
        binding.bottomNav.setupWithNavController(navController)

        // Hide bottom nav on inner destinations so detail / edit screens get full height.
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val showBottomNav = destination.id in TOP_LEVEL_DESTINATIONS
            binding.bottomNav.visibility = if (showBottomNav) android.view.View.VISIBLE
            else android.view.View.GONE
        }
    }

    /** Convenience used by the Settings screen after sign-out. */
    fun routeBackToAuth() {
        startActivity(Intent(this, RouterActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
        finish()
    }

    companion object {
        private val TOP_LEVEL_DESTINATIONS = setOf(
            R.id.dashboardFragment,
            R.id.bookListFragment,
            R.id.settingsFragment
        )
    }
}
