package com.joon.chalkak.presentation.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.joon.chalkak.presentation.common.AppBackground
import com.joon.chalkak.presentation.common.BottomNavigationBar
import com.joon.chalkak.presentation.history.HistoryScreen
import com.joon.chalkak.presentation.home.HomeScreen
import com.joon.chalkak.presentation.settings.SettingsScreen
import com.joon.chalkak.ui.theme.ChalkakTheme

@Composable
fun MainScreen(
    uiState: MainUiState,
    onDrivingActionClick: () -> Unit,
    onLocationPermissionClick: () -> Unit,
    onCameraDataUpdateClick: () -> Unit,
    onGpsAccuracyClick: () -> Unit,
    onAutoDrivingDetectionClick: () -> Unit,
    onClearRecordsClick: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val selectedTab = MainTab.fromRoute(navBackStackEntry?.destination?.route)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AppBackground
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                NavHost(
                    navController = navController,
                    startDestination = MainTab.HOME.route,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(MainTab.HOME.route) {
                        HomeScreen(
                            uiState = uiState,
                            onDrivingActionClick = onDrivingActionClick
                        )
                    }
                    composable(MainTab.HISTORY.route) {
                        HistoryScreen(uiState)
                    }
                    composable(MainTab.SETTINGS.route) {
                        SettingsScreen(
                            uiState = uiState,
                            onLocationPermissionClick = onLocationPermissionClick,
                            onCameraDataUpdateClick = onCameraDataUpdateClick,
                            onGpsAccuracyClick = onGpsAccuracyClick,
                            onAutoDrivingDetectionClick = onAutoDrivingDetectionClick,
                            onClearRecordsClick = onClearRecordsClick
                        )
                    }
                }
            }
            BottomNavigationBar(
                selectedTab = selectedTab,
                modifier = Modifier.navigationBarsPadding(),
                onTabSelected = { tab ->
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B1016)
@Composable
private fun MainScreenPreview() {
    ChalkakTheme {
        MainScreen(
            uiState = MainUiState(),
            onDrivingActionClick = {},
            onLocationPermissionClick = {},
            onCameraDataUpdateClick = {},
            onGpsAccuracyClick = {},
            onAutoDrivingDetectionClick = {},
            onClearRecordsClick = {}
        )
    }
}
