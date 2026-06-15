package com.joon.chalkak.presentation.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joon.chalkak.ui.theme.ChalkakTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChalkakTheme {
                val viewModel: MainViewModel = viewModel()
                MainScreen(
                    uiState = viewModel.uiState,
                    onTabSelected = viewModel::selectTab
                )
            }
        }
    }
}
