package com.joon.chalkak.domain.driving

import com.joon.chalkak.domain.EnforcementType
import com.joon.chalkak.domain.GeoLocation
import com.joon.chalkak.domain.LocationSpeedSample
import com.joon.chalkak.domain.NearbySpeedCamera
import com.joon.chalkak.domain.SpeedCamera
import com.joon.chalkak.domain.SpeedJudgementResult
import org.junit.Assert.assertEquals
import org.junit.Test

class CameraPassDetectorTest {
    @Test
    fun detectPasses_recordsCameraOnceWithinPassRadius() {
        val detector = CameraPassDetector(passRadiusMeters = 80.0)
        val sample = LocationSpeedSample(
            location = GeoLocation(latitude = 37.0, longitude = 127.0),
            speedKmh = 70.0,
            accuracyMeters = 5f,
            measuredAtMillis = 1_000L,
            bearingDegrees = 0.0
        )
        val nearbyCamera = NearbySpeedCamera(
            camera = speedCamera(),
            distanceMeters = 50.0,
            bearingToCameraDegrees = 0.0
        )

        val first = detector.detectPasses("session", sample, listOf(nearbyCamera))
        val second = detector.detectPasses("session", sample, listOf(nearbyCamera))

        assertEquals(1, first.size)
        assertEquals(SpeedJudgementResult.ENFORCEMENT_RISK, first.first().judgement.result)
        assertEquals(0, second.size)
    }

    @Test
    fun detectPasses_ignoresCameraOutsidePassRadius() {
        val detector = CameraPassDetector(passRadiusMeters = 80.0)
        val sample = LocationSpeedSample(
            location = GeoLocation(latitude = 37.0, longitude = 127.0),
            speedKmh = 50.0,
            accuracyMeters = 5f,
            measuredAtMillis = 1_000L,
            bearingDegrees = 0.0
        )

        val records = detector.detectPasses(
            sessionId = "session",
            sample = sample,
            nearbyCameras = listOf(
                NearbySpeedCamera(
                    camera = speedCamera(),
                    distanceMeters = 100.0,
                    bearingToCameraDegrees = 0.0
                )
            )
        )

        assertEquals(0, records.size)
    }

    @Test
    fun detectPasses_ignoresCameraBehindDrivingDirection() {
        val detector = CameraPassDetector(passRadiusMeters = 80.0)
        val sample = LocationSpeedSample(
            location = GeoLocation(latitude = 37.0, longitude = 127.0),
            speedKmh = 50.0,
            accuracyMeters = 5f,
            measuredAtMillis = 1_000L,
            bearingDegrees = 0.0
        )

        val records = detector.detectPasses(
            sessionId = "session",
            sample = sample,
            nearbyCameras = listOf(
                NearbySpeedCamera(
                    camera = speedCamera(),
                    distanceMeters = 50.0,
                    bearingToCameraDegrees = 180.0
                )
            )
        )

        assertEquals(0, records.size)
    }

    private fun speedCamera(): SpeedCamera =
        SpeedCamera(
            id = "camera-1",
            latitude = 37.0,
            longitude = 127.0,
            location = "테스트 위치",
            roadName = "테스트로",
            roadDirection = null,
            enforcementType = EnforcementType.SPEED,
            speedLimitKmh = 60,
            sectionPosition = null,
            sectionLengthMeters = null,
            protectedAreaType = null,
            referenceDate = null
        )
}
