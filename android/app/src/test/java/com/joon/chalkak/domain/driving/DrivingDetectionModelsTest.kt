package com.joon.chalkak.domain.driving

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DrivingDetectionModelsTest {
    @Test
    fun state_isInactiveByDefault() {
        val state = DrivingDetectionState()

        assertFalse(state.isServiceActive)
        assertFalse(state.isRecording)
    }

    @Test
    fun state_isRecording_whenHighAccuracyRecordingIsActive() {
        val state = DrivingDetectionState(status = DrivingDetectionStatus.RECORDING_HIGH_ACCURACY)

        assertTrue(state.isServiceActive)
        assertTrue(state.isRecording)
    }

    @Test(expected = IllegalArgumentException::class)
    fun config_requiresStartSpeedToBeGreaterThanStopSpeed() {
        DrivingDetectionConfig(
            startSpeedThresholdKmh = 5.0,
            stopSpeedThresholdKmh = 5.0
        )
    }
}
