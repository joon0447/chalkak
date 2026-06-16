package com.joon.chalkak.model

import com.joon.chalkak.domain.DrivingStatus

data class NearbyCamera(
    val distanceText: String,
    val title: String,
    val subtitle: String
)

data class RecentRecord(
    val title: String,
    val subtitle: String
)

data class HistorySummary(
    val totalDriveCount: String,
    val safePassRate: String,
    val warningCount: String
)

data class DriveRecordGroup(
    val date: String,
    val records: List<DriveRecord>
)

data class DriveRecord(
    val id: String,
    val time: String,
    val route: String,
    val cameraCount: String,
    val safeCount: String,
    val warningText: String = "",
    val status: DrivingStatus,
    val cameraPasses: List<CameraPassDetail> = emptyList()
)

data class CameraPassDetail(
    val passedTime: String,
    val location: String,
    val roadName: String?,
    val measuredSpeedText: String,
    val speedLimitText: String,
    val enforcementThresholdText: String,
    val distanceText: String,
    val status: DrivingStatus
)
