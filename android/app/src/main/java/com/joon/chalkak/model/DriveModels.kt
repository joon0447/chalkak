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
    val time: String,
    val route: String,
    val cameraCount: String,
    val safeCount: String,
    val warningText: String = "",
    val status: DrivingStatus
)
