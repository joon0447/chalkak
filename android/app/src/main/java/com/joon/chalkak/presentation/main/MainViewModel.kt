package com.joon.chalkak.presentation.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.joon.chalkak.domain.DrivingStatus
import com.joon.chalkak.domain.LocationSpeedSample
import com.joon.chalkak.model.DriveRecordGroup
import com.joon.chalkak.model.HistorySummary
import com.joon.chalkak.model.NearbyCamera
import com.joon.chalkak.model.RecentRecord

class MainViewModel : ViewModel() {
    var uiState by mutableStateOf(MainUiState())
        private set

    fun selectTab(tab: MainTab) {
        uiState = uiState.copy(selectedTab = tab)
    }

    fun setSpeedTracking(isTracking: Boolean) {
        uiState = uiState.copy(isSpeedTracking = isTracking)
    }

    fun updateSpeed(sample: LocationSpeedSample) {
        uiState = uiState.copy(currentSpeedKmh = sample.roundedSpeedKmh)
    }

    fun updateNearbyCamera(camera: NearbyCamera?) {
        uiState = uiState.copy(nearbyCamera = camera)
    }

    fun updateDrivingStatus(status: DrivingStatus) {
        uiState = uiState.copy(drivingStatus = status)
    }

    fun updateDriveRecords(
        historySummary: HistorySummary,
        recentRecords: List<RecentRecord>,
        driveRecordGroups: List<DriveRecordGroup>
    ) {
        uiState = uiState.copy(
            historySummary = historySummary,
            recentRecords = recentRecords,
            driveRecordGroups = driveRecordGroups
        )
    }

    fun updateLocationPermissionSubtitle(subtitle: String) {
        uiState = uiState.copy(locationPermissionSubtitle = subtitle)
    }

    fun updateCameraDataSubtitle(subtitle: String) {
        uiState = uiState.copy(cameraDataSubtitle = subtitle)
    }

    fun toggleGpsAccuracyMode() {
        val nextSubtitle = if (uiState.gpsAccuracySubtitle == "높은 정확도 모드") {
            "배터리 절약 모드"
        } else {
            "높은 정확도 모드"
        }
        uiState = uiState.copy(gpsAccuracySubtitle = nextSubtitle)
    }

    fun stopSpeedTracking() {
        uiState = uiState.copy(
            isSpeedTracking = false,
            currentSpeedKmh = 0,
            drivingStatus = DrivingStatus.UNKNOWN,
            nearbyCamera = null
        )
    }
}
