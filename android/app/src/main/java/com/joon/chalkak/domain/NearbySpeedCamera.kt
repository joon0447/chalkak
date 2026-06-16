package com.joon.chalkak.domain

import kotlin.math.cos
import kotlin.math.sin

data class NearbySpeedCamera(
    val camera: SpeedCamera,
    val distanceMeters: Double,
    val bearingToCameraDegrees: Double
) {
    val distanceText: String
        get() = if (distanceMeters >= 1000.0) {
            "%.1fkm".format(distanceMeters / 1000.0)
        } else {
            "${distanceMeters.toInt()}m"
        }
}

data class ForwardCameraSearchPolicy(
    val minSpeedKmh: Double = 10.0,
    val maxBearingDifferenceDegrees: Double = 35.0,
    val maxLateralDistanceMeters: Double = 45.0
)

fun List<NearbySpeedCamera>.filterForwardCorridor(
    sample: LocationSpeedSample,
    policy: ForwardCameraSearchPolicy = ForwardCameraSearchPolicy()
): List<NearbySpeedCamera> {
    val bearing = sample.bearingDegrees ?: return emptyList()
    if (sample.speedKmh < policy.minSpeedKmh) return emptyList()

    return filter { camera ->
        val bearingDifference = angularDistanceDegrees(bearing, camera.bearingToCameraDegrees)
        val bearingDifferenceRadians = Math.toRadians(bearingDifference)
        val forwardDistanceMeters = camera.distanceMeters * cos(bearingDifferenceRadians)
        val lateralDistanceMeters = camera.distanceMeters * sin(bearingDifferenceRadians)

        forwardDistanceMeters > 0.0 &&
            bearingDifference <= policy.maxBearingDifferenceDegrees &&
            lateralDistanceMeters <= policy.maxLateralDistanceMeters
    }.sortedBy { it.distanceMeters }
}
