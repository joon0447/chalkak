package com.joon.chalkak.presentation.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.joon.chalkak.presentation.common.AppBackground
import com.joon.chalkak.presentation.common.BottomNavigationBar
import com.joon.chalkak.presentation.history.HistoryScreen
import com.joon.chalkak.presentation.home.HomeScreen
import com.joon.chalkak.presentation.settings.SettingsScreen
import com.joon.chalkak.ui.theme.ChalkakTheme

@Composable
fun MainScreen(
    uiState: MainUiState,
    onTabSelected: (MainTab) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AppBackground
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (uiState.selectedTab) {
                    MainTab.HOME -> HomeScreen(uiState)
                    MainTab.HISTORY -> HistoryScreen(uiState)
                    MainTab.SETTINGS -> SettingsScreen()
                }
            }
            BottomNavigationBar(
                selectedTab = uiState.selectedTab,
                onTabSelected = onTabSelected
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
            onTabSelected = {}
        )
    }
}
