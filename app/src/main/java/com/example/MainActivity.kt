package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.data.database.TabunganDatabase
import com.example.data.repository.TabunganRepository
import com.example.data.FirebaseSyncManager
import com.example.ui.TabunganViewModel
import com.example.ui.TabunganViewModelFactory
import com.example.ui.components.MainAppScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Database and Repository
        val database = TabunganDatabase.getDatabase(applicationContext)
        val repository = TabunganRepository(database.dao())
        
        // Initialize and start Firebase Synchronization Manager
        val syncManager = FirebaseSyncManager(applicationContext, repository)
        syncManager.startSyncing()

        // Initialize ViewModel via Factory
        val factory = TabunganViewModelFactory(application, repository, syncManager)
        val viewModel = ViewModelProvider(this, factory)[TabunganViewModel::class.java]

        setContent {
            val themePrefState by viewModel.themePreference.collectAsState()
            val isDark = when (themePrefState) {
                "LIGHT" -> false
                "DARK" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            MyApplicationTheme(
                darkTheme = isDark,
                dynamicColor = false // Use curated brand colors
            ) {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}
