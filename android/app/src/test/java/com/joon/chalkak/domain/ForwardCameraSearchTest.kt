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

    @Test
    fun filterForwardCorridor_excludesCameraWithOppositeExplicitDirectionHint() {
        val result = listOf(
            nearbyCamera(
                distanceMeters = 80.0,
                bearingToCameraDegrees = 0.0,
                location = "테스트 지점 남행"
            )
        ).filterForwardCorridor(movingNorthSample())

        assertEquals(0, result.size)
    }

    @Test
    fun filterForwardCorridor_keepsCameraWithMatchingExplicitDirectionHint() {
        val result = listOf(
            nearbyCamera(
                distanceMeters = 80.0,
                bearingToCameraDegrees = 0.0,
                location = "테스트 지점 북행"
            )
        ).filterForwardCorridor(movingNorthSample())

        assertEquals(1, result.size)
    }

    @Test
    fun filterForwardCorridor_doesNotTreatPlaceNameAsCardinalDirection() {
        val result = listOf(
            nearbyCamera(
                distanceMeters = 80.0,
                bearingToCameraDegrees = 0.0,
                location = "중앙시장앞(서면방향)"
            )
        ).filterForwardCorridor(movingNorthSample())

        assertEquals(1, result.size)
    }

    @Test
    fun filterForwardCorridor_collapsesNearbyDirectionalDuplicatesOnSameRoad() {
        val result = listOf(
            nearbyCamera(
                id = "north",
                distanceMeters = 80.0,
                bearingToCameraDegrees = 0.0,
                location = "테스트 지점 북행"
            ),
            nearbyCamera(
                id = "unknown",
                distanceMeters = 82.0,
                bearingToCameraDegrees = 0.0,
                location = "테스트 지점"
            )
        ).filterForwardCorridor(movingNorthSample())

        assertEquals(listOf("north"), result.map { it.camera.id })
    }

    @Test
    fun filterForwardCorridor_keepsOnlyMatchingDirectionWhenBothLanesHaveCameras() {
        val result = listOf(
            nearbyCamera(
                id = "opposite-lane",
                distanceMeters = 55.0,
                bearingToCameraDegrees = 2.0,
                location = "테스트 지점 남행"
            ),
            nearbyCamera(
                id = "driving-lane",
                distanceMeters = 60.0,
                bearingToCameraDegrees = 2.0,
                location = "테스트 지점 북행"
            )
        ).filterForwardCorridor(movingNorthSample())

        assertEquals(listOf("driving-lane"), result.map { it.camera.id })
    }

    @Test
    fun filterForwardCorridor_prefersMatchingDirectionHintOverUnknownSamePoint() {
        val result = listOf(
            nearbyCamera(
                id = "unknown",
                distanceMeters = 55.0,
                bearingToCameraDegrees = 0.0,
                location = "테스트 지점"
            ),
            nearbyCamera(
                id = "north",
                distanceMeters = 65.0,
                bearingToCameraDegrees = 0.0,
                location = "테스트 지점 북행"
            )
        ).filterForwardCorridor(movingNorthSample())

        assertEquals(listOf("north"), result.map { it.camera.id })
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
        id: String = "camera",
        distanceMeters: Double,
        bearingToCameraDegrees: Double,
        location: String = "테스트 위치"
    ): NearbySpeedCamera =
        NearbySpeedCamera(
            camera = SpeedCamera(
                id = id,
                latitude = 37.0,
                longitude = 127.0,
                location = location,
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
