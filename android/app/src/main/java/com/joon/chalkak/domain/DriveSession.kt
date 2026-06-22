package com.joon.chalkak.domain

data class DriveSession(
    val id: String,
    val startedAtMillis: Long,
    val endedAtMillis: Long? = null,
    val source: DriveSessionSource = DriveSessionSource.MANUAL,
    val passRecords: List<CameraPassRecord> = emptyList()
) {
    val isActive: Boolean
        get() = endedAtMillis == null
}

enum class DriveSessionSource {
    MANUAL,
    AUTO
}

data class CameraPassRecord(
    val id: String,
    val sessionId: String,
    val camera: SpeedCamera,
    val passedAtMillis: Long,
    val measuredSpeedKmh: Int,
    val locationAccuracyMeters: Float?,
    val distanceToCameraMeters: Double?,
    val judgement: SpeedJudgement
)
