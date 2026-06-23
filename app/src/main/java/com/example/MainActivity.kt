package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.database.CryptoDatabase
import com.example.data.repository.CryptoRepository
import com.example.ui.components.DashboardMainView
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MarketViewModel
import com.example.ui.viewmodel.MarketViewModelFactory

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    // Initialize Room Database, DAOs, and repository cleanly on startup
    val database = CryptoDatabase.getDatabase(applicationContext)
    val repository = CryptoRepository(
        marketAlertDao = database.marketAlertDao(),
        alertLogDao = database.alertLogDao(),
        aiPredictionDao = database.aiPredictionDao()
    )
    val viewModelFactory = MarketViewModelFactory(repository)

    setContent {
      MyApplicationTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          DashboardMainView(
              viewModel = viewModel(factory = viewModelFactory),
              modifier = Modifier.padding(innerPadding)
          )
        }
      }
    }
  }
}

