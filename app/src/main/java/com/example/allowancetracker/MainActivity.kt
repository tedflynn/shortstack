package com.example.allowancetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.allowancetracker.data.local.AppDatabase
import com.example.allowancetracker.data.local.DataStoreSelectedKidStore
import com.example.allowancetracker.repository.TransactionRepository
import com.example.allowancetracker.ui.screens.TrackerMainScreen
import com.example.allowancetracker.ui.theme.AllowanceTrackerTheme
import com.example.allowancetracker.ui.viewmodel.TrackerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getInstance(applicationContext)
        val repository = TransactionRepository(database)
        val viewModelFactory = TrackerViewModel.Factory(
            repository,
            DataStoreSelectedKidStore(applicationContext)
        )

        setContent {
            AllowanceTrackerTheme {
                val trackerViewModel: TrackerViewModel = viewModel(factory = viewModelFactory)
                TrackerMainScreen(viewModel = trackerViewModel)
            }
        }
    }
}
