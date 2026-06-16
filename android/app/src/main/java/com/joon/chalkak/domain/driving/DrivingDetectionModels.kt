package com.joon.chalkak.domain.driving

data class DrivingDetectionConfig(
    val startSpeedThresholdKmh: Double = 20.0,
    val startDetectionWindowMillis: Long = 60_000L,
    val startRequiredSampleCount: Int = 3,
    val startMinDistanceMeters: Double = 200.0,
    val stopSpeedThresholdKmh: Double = 5.0,
    val stopDetectionWindowMillis: Long = 180_000L,
    val maxAcceptableAccuracyMeters: Float = 50f
) {
    init {
        require(startSpeedThresholdKmh > stopSpeedThresholdKmh) {
            "startSpeedThresholdKmh must be greater than stopSpeedThresholdKmh."
        }
        require(startDetectionWindowMillis > 0L) { "startDetectionWindowMillis must be positive." }
        require(startRequiredSampleCount > 0) { "startRequiredSampleCount must be positive." }
        require(startMinDistanceMeters >= 0.0) { "startMinDistanceMeters must be 0 or greater." }
        require(stopDetectionWindowMillis > 0L) { "stopDetectionWindowMillis must be positive." }
        require(maxAcceptableAccuracyMeters > 0f) { "maxAcceptableAccuracyMeters must be positive." }
    }
}

enum class DrivingDetectionStatus {
    OFF,
    MONITORING_LOW_POWER,
    DRIVING_DETECTED,
    RECORDING_HIGH_ACCURACY,
    STOPPING_PENDING,
    ERROR
}

data class DrivingDetectionState(
    val status: DrivingDetectionStatus = DrivingDetectionStatus.OFF,
    val config: DrivingDetectionConfig = DrivingDetectionConfig(),
    val activeSessionId: String? = null,
    val lastStatusChangedAtMillis: Long? = null,
    val errorMessage: String? = null
) {
    val isServiceActive: Boolean
        get() = status != DrivingDetectionStatus.OFF

    val isRecording: Boolean
        get() = status == DrivingDetectionStatus.RECORDING_HIGH_ACCURACY ||
            status == DrivingDetectionStatus.STOPPING_PENDING
}
