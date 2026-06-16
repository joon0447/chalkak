package com.joon.chalkak.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ForwardCameraSearchTest {
    @Test
    fun filterForwardCorridor_keepsCameraInFrontCorridor() {
        val result = listOf(
            nearbyCamera(distanceMeters = 120.0, bearingToCameraDegrees = 5.0)
        ).filterForwardCorridor(movingNorthSample())

        assertEquals(1, result.size)
    }

    @Test
    fun filterForwardCorridor_excludesCameraBehindDriver() {
        val result = listOf(
            nearbyCamera(distanceMeters = 50.0, bearingToCameraDegrees = 180.0)
        ).filterForwardCorridor(movingNorthSample())

        assertEquals(0, result.size)
    }

    @Test
    fun filterForwardCorridor_excludesCameraOutsideLateralCorridor() {
        val result = listOf(
            nearbyCamera(distanceMeters = 400.0, bearingToCameraDegrees = 30.0)
        ).filterForwardCorridor(movingNorthSample())

        assertEquals(0, result.size)
    }

    @Test
    fun filterForwardCorridor_excludesCameraWhenDriverIsNearlyStopped() {
        val result = listOf(
            nearbyCamera(distanceMeters = 50.0, bearingToCameraDegrees = 0.0)
        ).filterForwardCorridor(movingNorthSample(speedKmh = 5.0))

        assertEquals(0, result.size)
    }

    @Test
    fun filterForwardCorridor_excludesCameraWhenBearingIsMissing() {
        val result = listOf(
            nearbyCamera(distanceMeters = 50.0, bearingToCameraDegrees = 0.0)
        ).filterForwardCorridor(movingNorthSample(bearingDegrees = null))

        assertEquals(0, result.size)
    }

    private fun movingNorthSample(
        speedKmh: Double = 40.0,
        bearingDegrees: Double? = 0.0
    ): LocationSpeedSample =
        LocationSpeedSample(
            location = GeoLocation(latitude = 37.0, longitude = 127.0),
            speedKmh = speedKmh,
            accuracyMeters = 5f,
            measuredAtMillis = 1_000L,
            bearingDegrees = bearingDegrees
        )

    private fun nearbyCamera(
        distanceMeters: Double,
        bearingToCameraDegrees: Double
    ): NearbySpeedCamera =
        NearbySpeedCamera(
            camera = SpeedCamera(
                id = "camera",
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
            ),
            distanceMeters = distanceMeters,
            bearingToCameraDegrees = bearingToCameraDegrees
        )
}
