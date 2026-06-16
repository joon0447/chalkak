package com.joon.chalkak.presentation.main

import com.joon.chalkak.domain.DrivingStatus
import com.joon.chalkak.model.DriveRecordGroup
import com.joon.chalkak.model.HistorySummary
import com.joon.chalkak.model.NearbyCamera
import com.joon.chalkak.model.RecentRecord

data class MainUiState(
    val currentSpeedKmh: Int = 0,
    val drivingStatus: DrivingStatus = DrivingStatus.UNKNOWN,
    val isSpeedTracking: Boolean = false,
    val locationPermissionSubtitle: String = "설정 필요",
    val cameraDataSubtitle: String = "업데이트 필요",
    val gpsAccuracySubtitle: String = "높은 정확도 모드",
    val autoDrivingDetectionSubtitle: String = "꺼짐",
    val isAutoDrivingDetectionEnabled: Boolean = false,
    val recordRetentionSubtitle: String = "최근 90일",
    val nearbyCamera: NearbyCamera? = null,
    val recentRecords: List<RecentRecord> = emptyList(),
    val historySummary: HistorySummary = HistorySummary(
        totalDriveCount = "0회",
        safePassRate = "0%",
        warningCount = "0건"
    ),
    val driveRecordGroups: List<DriveRecordGroup> = emptyList()
)
