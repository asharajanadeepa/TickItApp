package com.example.tickitapp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.tickitapp.R
import com.example.tickitapp.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate started")
        
        try {
            // Initialize view binding
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
            Log.d(TAG, "View binding initialized")

            // Find the NavHostFragment
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            navController = navHostFragment.navController
            Log.d(TAG, "NavController initialized")

            // Define top-level destinations
            appBarConfiguration = AppBarConfiguration(
                setOf(
                    R.id.navigation_home,
                    R.id.navigation_analysis,
                    R.id.navigation_budget
                )
            )
            Log.d(TAG, "AppBarConfiguration initialized")

            // Set up the ActionBar with navigation
            setSupportActionBar(binding.toolbar)
            setupActionBarWithNavController(navController, appBarConfiguration)
            Log.d(TAG, "ActionBar setup complete")
            
            // Set up the bottom navigation
            binding.bottomNavigation.apply {
                setupWithNavController(navController)
                setOnItemReselectedListener { } // Prevent reselection handling
            }
            Log.d(TAG, "Bottom navigation setup complete")

            // Add navigation listener for error handling
            navController.addOnDestinationChangedListener { _, destination, _ ->
                try {
                    Log.d(TAG, "Navigating to: ${destination.label}")
                } catch (e: Exception) {
                    Log.e(TAG, "Navigation error: ${e.message}", e)
                    showError("Navigation failed: ${e.message}")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
            showError("Failed to initialize app: ${e.message}")
        }
    }

    private fun showError(message: String) {
        Log.e(TAG, "Showing error: $message")
        try {
            if (!isFinishing) {
                Snackbar.make(
                    binding.root,
                    message,
                    Snackbar.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing Snackbar: ${e.message}", e)
            try {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.e(TAG, "Error showing Toast: ${e.message}", e)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return try {
            navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onSupportNavigateUp: ${e.message}", e)
            false
        }
    }
}