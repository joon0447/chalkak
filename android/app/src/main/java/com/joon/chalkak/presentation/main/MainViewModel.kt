package com.joon.chalkak.presentation.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {
    var uiState by mutableStateOf(MainUiState())
        private set

    fun selectTab(tab: MainTab) {
        uiState = uiState.copy(selectedTab = tab)
    }
}
