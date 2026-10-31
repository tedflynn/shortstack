package com.vivificavit.shortstack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vivificavit.shortstack.data.local.AppDatabase
import com.vivificavit.shortstack.data.local.DataStoreSelectedKidStore
import com.vivificavit.shortstack.repository.TransactionRepository
import com.vivificavit.shortstack.ui.screens.TrackerMainScreen
import com.vivificavit.shortstack.ui.theme.AllowanceTrackerTheme
import com.vivificavit.shortstack.ui.viewmodel.TrackerViewModel

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
