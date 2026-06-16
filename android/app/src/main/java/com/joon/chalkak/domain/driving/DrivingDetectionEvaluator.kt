package com.joon.chalkak.domain.driving

import com.joon.chalkak.domain.LocationSpeedSample
import com.joon.chalkak.domain.distanceToMeters

class DrivingStartDetector(
    private val config: DrivingDetectionConfig = DrivingDetectionConfig()
) {
    private val samples = ArrayDeque<LocationSpeedSample>()

    fun addSample(sample: LocationSpeedSample): Boolean {
        samples.addLast(sample)
        trimSamples(sample.measuredAtMillis)

        val accurateSamples = samples.filter { it.isAccurateEnough() }
        val fastSamples = accurateSamples.count { it.speedKmh >= config.startSpeedThresholdKmh }
        val distanceMeters = accurateSamples.totalDistanceMeters()

        return fastSamples >= config.startRequiredSampleCount &&
            distanceMeters >= config.startMinDistanceMeters
    }

    fun reset() {
        samples.clear()
    }

    private fun trimSamples(nowMillis: Long) {
        while (samples.isNotEmpty() && nowMillis - samples.first().measuredAtMillis > config.startDetectionWindowMillis) {
            samples.removeFirst()
        }
    }

    private fun LocationSpeedSample.isAccurateEnough(): Boolean =
        accuracyMeters == null || accuracyMeters <= config.maxAcceptableAccuracyMeters
}

class DrivingStopDetector(
    private val config: DrivingDetectionConfig = DrivingDetectionConfig()
) {
    private var lowSpeedStartedAtMillis: Long? = null

    fun addSample(sample: LocationSpeedSample): Boolean {
        if (sample.speedKmh >= config.stopSpeedThresholdKmh || !sample.isAccurateEnough()) {
            lowSpeedStartedAtMillis = null
            return false
        }

        val startedAt = lowSpeedStartedAtMillis ?: sample.measuredAtMillis.also {
            lowSpeedStartedAtMillis = it
        }

        return sample.measuredAtMillis - startedAt >= config.stopDetectionWindowMillis
    }

    fun reset() {
        lowSpeedStartedAtMillis = null
    }

    private fun LocationSpeedSample.isAccurateEnough(): Boolean =
        accuracyMeters == null || accuracyMeters <= config.maxAcceptableAccuracyMeters
}

private fun List<LocationSpeedSample>.totalDistanceMeters(): Double =
    zipWithNext().sumOf { (previous, current) ->
        previous.location.distanceToMeters(current.location)
    }
