package com.example.allowancetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.allowancetracker.data.local.AppDatabase
import com.example.allowancetracker.data.local.SharedPrefsSelectedKidStore
import com.example.allowancetracker.repository.TransactionRepository
import com.example.allowancetracker.ui.screens.TrackerMainScreen
import com.example.allowancetracker.ui.viewmodel.TrackerViewModel
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getInstance(applicationContext)
        val repository = TransactionRepository(database)
        val viewModelFactory = TrackerViewModel.Factory(
            repository,
            SharedPrefsSelectedKidStore(applicationContext)
        )

        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                val trackerViewModel: TrackerViewModel = viewModel(factory = viewModelFactory)
                TrackerMainScreen(viewModel = trackerViewModel)
            }
        }
    }
}
