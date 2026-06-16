package com.joon.chalkak.domain.driving

import com.joon.chalkak.domain.CameraPassRecord
import com.joon.chalkak.domain.LocationSpeedSample
import com.joon.chalkak.domain.NearbySpeedCamera
import com.joon.chalkak.domain.angularDistanceDegrees
import com.joon.chalkak.domain.judgeSpeed

class CameraPassDetector(
    private val passRadiusMeters: Double = DEFAULT_PASS_RADIUS_METERS
) {
    private val recordedCameraIds = mutableSetOf<String>()

    fun detectPasses(
        sessionId: String,
        sample: LocationSpeedSample,
        nearbyCameras: List<NearbySpeedCamera>
    ): List<CameraPassRecord> =
        nearbyCameras
            .filter { it.distanceMeters <= passRadiusMeters }
            .filter { it.isAheadOf(sample) }
            .filterNot { it.camera.id in recordedCameraIds }
            .map { nearbyCamera ->
                recordedCameraIds += nearbyCamera.camera.id
                CameraPassRecord(
                    id = "${sessionId}_${nearbyCamera.camera.id}",
                    sessionId = sessionId,
                    camera = nearbyCamera.camera,
                    passedAtMillis = System.currentTimeMillis(),
                    measuredSpeedKmh = sample.roundedSpeedKmh,
                    locationAccuracyMeters = sample.accuracyMeters,
                    distanceToCameraMeters = nearbyCamera.distanceMeters,
                    judgement = judgeSpeed(
                        measuredSpeedKmh = sample.roundedSpeedKmh,
                        speedLimitKmh = nearbyCamera.camera.speedLimitKmh
                    )
                )
            }

    fun reset() {
        recordedCameraIds.clear()
    }

    private fun NearbySpeedCamera.isAheadOf(sample: LocationSpeedSample): Boolean {
        val bearing = sample.bearingDegrees ?: return true
        if (sample.speedKmh < MIN_DIRECTION_FILTER_SPEED_KMH) return true

        return angularDistanceDegrees(bearing, bearingToCameraDegrees) <= MAX_BEARING_DIFF_DEGREES
    }

    private companion object {
        const val DEFAULT_PASS_RADIUS_METERS = 80.0
        const val MIN_DIRECTION_FILTER_SPEED_KMH = 10.0
        const val MAX_BEARING_DIFF_DEGREES = 80.0
    }
}
