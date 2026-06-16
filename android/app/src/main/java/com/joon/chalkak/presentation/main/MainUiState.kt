package com.joon.chalkak.presentation.main

import com.joon.chalkak.domain.DrivingStatus
import com.joon.chalkak.model.DriveRecordGroup
import com.joon.chalkak.model.HistorySummary
import com.joon.chalkak.model.NearbyCamera
import com.joon.chalkak.model.RecentRecord

data class MainUiState(
    val selectedTab: MainTab = MainTab.HOME,
    val currentSpeedKmh: Int = 0,
    val drivingStatus: DrivingStatus = DrivingStatus.UNKNOWN,
    val nearbyCamera: NearbyCamera? = null,
    val recentRecords: List<RecentRecord> = emptyList(),
    val historySummary: HistorySummary = HistorySummary(
        totalDriveCount = "0회",
        safePassRate = "0%",
        warningCount = "0건"
    ),
    val driveRecordGroups: List<DriveRecordGroup> = emptyList()
)
