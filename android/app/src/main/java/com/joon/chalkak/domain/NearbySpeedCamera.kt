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
    val maxLateralDistanceMeters: Double = 45.0,
    val maxLateralDistanceWithoutDirectionHintMeters: Double = 30.0,
    val maxDirectionHintDifferenceDegrees: Double = 70.0,
    val duplicateCameraDistanceMeters: Double = 25.0
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

        val lateralDistanceLimit = if (camera.camera.explicitDirectionBearingDegrees() == null) {
            policy.maxLateralDistanceWithoutDirectionHintMeters
        } else {
            policy.maxLateralDistanceMeters
        }

        forwardDistanceMeters > 0.0 &&
            bearingDifference <= policy.maxBearingDifferenceDegrees &&
            lateralDistanceMeters <= lateralDistanceLimit &&
            camera.matchesDirectionHint(bearing, policy)
    }.sortedWith(
        compareBy<NearbySpeedCamera> { it.directionHintPenalty(bearing, policy) }
            .thenBy { it.distanceMeters }
    ).collapseDirectionalDuplicates(policy)
}

private fun NearbySpeedCamera.matchesDirectionHint(
    driverBearingDegrees: Double,
    policy: ForwardCameraSearchPolicy
): Boolean {
    val cameraDirectionBearing = camera.explicitDirectionBearingDegrees() ?: return true
    return angularDistanceDegrees(driverBearingDegrees, cameraDirectionBearing) <=
        policy.maxDirectionHintDifferenceDegrees
}

private fun NearbySpeedCamera.directionHintPenalty(
    driverBearingDegrees: Double,
    policy: ForwardCameraSearchPolicy
): Int {
    val cameraDirectionBearing = camera.explicitDirectionBearingDegrees() ?: return 1
    return if (
        angularDistanceDegrees(driverBearingDegrees, cameraDirectionBearing) <=
        policy.maxDirectionHintDifferenceDegrees
    ) {
        0
    } else {
        2
    }
}

private fun List<NearbySpeedCamera>.collapseDirectionalDuplicates(
    policy: ForwardCameraSearchPolicy
): List<NearbySpeedCamera> =
    fold(emptyList<NearbySpeedCamera>()) { selected, candidate ->
        val hasNearbyDuplicate = selected.any { existing ->
            existing.camera.roadName != null &&
                existing.camera.roadName == candidate.camera.roadName &&
                existing.camera.locationCoordinate.distanceToMeters(candidate.camera.locationCoordinate) <=
                policy.duplicateCameraDistanceMeters
        }
        if (hasNearbyDuplicate) selected else selected + candidate
    }.sortedBy { it.distanceMeters }

private fun SpeedCamera.explicitDirectionBearingDegrees(): Double? {
    val directionTexts = listOfNotNull(roadDirection, location, roadName)
    return directionTexts.firstNotNullOfOrNull { it.toExplicitDirectionBearingDegrees() }
}

private fun String.toExplicitDirectionBearingDegrees(): Double? {
    val normalized = replace(" ", "")
        .replace("(", "")
        .replace(")", "")
        .trim()

    return when {
        normalized.hasAny("북동행", "북동향", "북동쪽") -> 45.0
        normalized.hasAny("남동행", "남동향", "남동쪽") -> 135.0
        normalized.hasAny("남서행", "남서향", "남서쪽") -> 225.0
        normalized.hasAny("북서행", "북서향", "북서쪽") -> 315.0
        normalized.hasAny("북행", "북향", "북쪽") -> 0.0
        normalized.hasAny("동행", "동향", "동쪽") -> 90.0
        normalized.hasAny("남행", "남향", "남쪽") -> 180.0
        normalized.hasAny("서행", "서향", "서쪽") -> 270.0
        else -> null
    }
}

private fun String.hasAny(vararg tokens: String): Boolean =
    tokens.any { contains(it) }
