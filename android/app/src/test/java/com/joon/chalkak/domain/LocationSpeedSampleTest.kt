package com.joon.chalkak.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class LocationSpeedSampleTest {
    @Test
    fun toSpeedSample_usesMeasuredLocationSpeed_whenAvailable() {
        val reading = LocationReading(
            location = GeoLocation(latitude = 37.5665, longitude = 126.9780),
            elapsedRealtimeMillis = 1_000L,
            speedMetersPerSecond = 10.0,
            accuracyMeters = 5f,
            bearingDegrees = 90.0
        )

        val sample = reading.toSpeedSample(previous = null)

        assertEquals(36.0, sample.speedKmh, 0.001)
        assertEquals(36, sample.roundedSpeedKmh)
    }

    @Test
    fun toSpeedSample_calculatesFallbackSpeed_whenMeasuredSpeedIsMissing() {
        val previous = LocationReading(
            location = GeoLocation(latitude = 37.5665, longitude = 126.9780),
            elapsedRealtimeMillis = 1_000L,
            speedMetersPerSecond = null,
            accuracyMeters = null,
            bearingDegrees = null
        )
        val current = LocationReading(
            location = GeoLocation(latitude = 37.5674, longitude = 126.9780),
            elapsedRealtimeMillis = 11_000L,
            speedMetersPerSecond = null,
            accuracyMeters = null,
            bearingDegrees = null
        )

        val sample = current.toSpeedSample(previous = previous)

        assertEquals(36.0, sample.speedKmh, 3.0)
    }
}
