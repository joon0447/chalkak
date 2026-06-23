package com.joon.chalkak.domain.driving

import com.joon.chalkak.domain.GeoLocation
import com.joon.chalkak.domain.LocationSpeedSample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DrivingBearingStabilizerTest {
    @Test
    fun stabilize_usesRecentMovementVectorWhenBearingIsMissing() {
        val stabilizer = DrivingBearingStabilizer()

        stabilizer.stabilize(sample(latitude = 37.0, longitude = 127.0, bearingDegrees = null))
        val stabilized = stabilizer.stabilize(
            sample(latitude = 37.001, longitude = 127.0, measuredAtMillis = 2_000L, bearingDegrees = null)
        )

        assertNotNull(stabilized.bearingDegrees)
        assertEquals(0.0, stabilized.bearingDegrees ?: -1.0, 1.0)
    }

    private fun sample(
        latitude: Double,
        longitude: Double,
        measuredAtMillis: Long = 1_000L,
        bearingDegrees: Double?
    ): LocationSpeedSample =
        LocationSpeedSample(
            location = GeoLocation(latitude = latitude, longitude = longitude),
            speedKmh = 40.0,
            accuracyMeters = 5f,
            measuredAtMillis = measuredAtMillis,
            bearingDegrees = bearingDegrees
        )
}
