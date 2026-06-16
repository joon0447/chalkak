package com.joon.chalkak.presentation.main

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joon.chalkak.BuildConfig
import com.joon.chalkak.data.camera.remote.PublicDataCameraApiClient
import com.joon.chalkak.ui.theme.ChalkakTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        logCameraApiSmokeCheck()
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

    private fun logCameraApiSmokeCheck() {
        if (!BuildConfig.DEBUG) return

        lifecycleScope.launch(Dispatchers.IO) {
            runCatching {
                Log.d(TAG, "Smoke check started.")
                val cameras = PublicDataCameraApiClient().fetchCameras(
                    pageNo = 1,
                    numOfRows = 5
                )
                Log.d(
                    TAG,
                    "Smoke check success: count=${cameras.size}, first=${cameras.firstOrNull()?.id}"
                )
            }.onFailure { throwable ->
                Log.e(TAG, "Smoke check failed: ${throwable.message}", throwable)
            }
        }
    }

    private companion object {
        const val TAG = "CameraApi"
    }
}
