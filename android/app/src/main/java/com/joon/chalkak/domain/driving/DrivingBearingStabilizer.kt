package com.joon.chalkak.domain.driving

import com.joon.chalkak.domain.LocationSpeedSample
import com.joon.chalkak.domain.bearingToDegrees
import com.joon.chalkak.domain.distanceToMeters
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class DrivingBearingStabilizer(
    private val maxBearingCount: Int = DEFAULT_MAX_BEARING_COUNT
) {
    private val recentBearings = ArrayDeque<Double>()
    private var previousSample: LocationSpeedSample? = null

    fun stabilize(sample: LocationSpeedSample): LocationSpeedSample {
        val previous = previousSample
        if (previous != null) {
            val movementDistanceMeters = previous.location.distanceToMeters(sample.location)
            if (movementDistanceMeters >= MIN_MOVEMENT_DISTANCE_METERS) {
                addBearing(previous.location.bearingToDegrees(sample.location))
            }
        }
        previousSample = sample

        val bearingCandidates = buildList {
            addAll(recentBearings)
            sample.bearingDegrees?.let(::add)
        }
        val stabilizedBearing = bearingCandidates.circularMeanDegrees() ?: sample.bearingDegrees
        return sample.copy(bearingDegrees = stabilizedBearing)
    }

    fun reset() {
        recentBearings.clear()
        previousSample = null
    }

    private fun addBearing(bearing: Double) {
        recentBearings += bearing
        while (recentBearings.size > maxBearingCount) {
            recentBearings.removeFirst()
        }
    }

    private fun List<Double>.circularMeanDegrees(): Double? {
        if (isEmpty()) return null

        val x = sumOf { cos(Math.toRadians(it)) }
        val y = sumOf { sin(Math.toRadians(it)) }
        return Math.toDegrees(atan2(y, x)).normalizeDegrees()
    }

    private fun Double.normalizeDegrees(): Double =
        ((this % FULL_CIRCLE_DEGREES) + FULL_CIRCLE_DEGREES) % FULL_CIRCLE_DEGREES

    private companion object {
        const val DEFAULT_MAX_BEARING_COUNT = 5
        const val MIN_MOVEMENT_DISTANCE_METERS = 8.0
        const val FULL_CIRCLE_DEGREES = 360.0
    }
}
