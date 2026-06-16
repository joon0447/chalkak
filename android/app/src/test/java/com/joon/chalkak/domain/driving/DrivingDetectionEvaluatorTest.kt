package com.joon.chalkak.domain.driving

import com.joon.chalkak.domain.GeoLocation
import com.joon.chalkak.domain.LocationSpeedSample
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DrivingDetectionEvaluatorTest {
    @Test
    fun startDetector_detectsDriving_whenFastAccurateSamplesMoveFarEnough() {
        val detector = DrivingStartDetector(testConfig())

        assertFalse(detector.addSample(sample(index = 0, speedKmh = 22.0)))
        assertFalse(detector.addSample(sample(index = 1, speedKmh = 25.0)))
        assertTrue(detector.addSample(sample(index = 2, speedKmh = 28.0)))
    }

    @Test
    fun startDetector_ignoresInaccurateSamples() {
        val detector = DrivingStartDetector(testConfig())

        assertFalse(detector.addSample(sample(index = 0, speedKmh = 30.0, accuracyMeters = 100f)))
        assertFalse(detector.addSample(sample(index = 1, speedKmh = 30.0, accuracyMeters = 100f)))
        assertFalse(detector.addSample(sample(index = 2, speedKmh = 30.0, accuracyMeters = 100f)))
    }

    @Test
    fun stopDetector_detectsStop_whenLowSpeedContinuesLongEnough() {
        val detector = DrivingStopDetector(testConfig())

        assertFalse(detector.addSample(sample(index = 0, speedKmh = 0.0)))
        assertTrue(detector.addSample(sample(index = 4, speedKmh = 0.0)))
    }

    private fun testConfig(): DrivingDetectionConfig =
        DrivingDetectionConfig(
            startSpeedThresholdKmh = 20.0,
            startDetectionWindowMillis = 60_000L,
            startRequiredSampleCount = 3,
            startMinDistanceMeters = 100.0,
            stopSpeedThresholdKmh = 5.0,
            stopDetectionWindowMillis = 30_000L,
            maxAcceptableAccuracyMeters = 50f
        )

    private fun sample(
        index: Int,
        speedKmh: Double,
        accuracyMeters: Float = 10f
    ): LocationSpeedSample =
        LocationSpeedSample(
            location = GeoLocation(latitude = 37.0 + index * 0.001, longitude = 127.0),
            speedKmh = speedKmh,
            accuracyMeters = accuracyMeters,
            measuredAtMillis = 1_000L + index * 10_000L,
            bearingDegrees = 0.0
        )
}
