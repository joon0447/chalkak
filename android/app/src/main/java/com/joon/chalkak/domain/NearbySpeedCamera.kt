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
    val maxBearingDifferenceDegrees: Double = 70.0,
    val maxLateralDistanceMeters: Double = 70.0,
    val maxLateralDistanceWithoutDirectionHintMeters: Double = 55.0,
    val maxDirectionHintDifferenceDegrees: Double = 100.0,
    val duplicateCameraDistanceMeters: Double = 40.0
)

fun List<NearbySpeedCamera>.filterForwardCorridor(
    sample: LocationSpeedSample,
    policy: ForwardCameraSearchPolicy = ForwardCameraSearchPolicy()
): List<NearbySpeedCamera> {
    val bearing = sample.bearingDegrees ?: return emptyList()
    if (sample.speedKmh < policy.minSpeedKmh) return emptyList()

    return mapNotNull { camera -> camera.toForwardCandidate(bearing, policy) }
        .sortedWith(compareBy<ForwardCameraCandidate> { it.score }.thenBy { it.camera.distanceMeters })
        .selectBestCandidatePerPhysicalPoint(policy)
        .sortedBy { it.camera.distanceMeters }
        .map { it.camera }
}

private data class ForwardCameraCandidate(
    val camera: NearbySpeedCamera,
    val score: Double
)

private fun NearbySpeedCamera.toForwardCandidate(
    driverBearingDegrees: Double,
    policy: ForwardCameraSearchPolicy
): ForwardCameraCandidate? {
    val bearingDifference = angularDistanceDegrees(driverBearingDegrees, bearingToCameraDegrees)
    if (bearingDifference > policy.maxBearingDifferenceDegrees) return null

    val bearingDifferenceRadians = Math.toRadians(bearingDifference)
    val forwardDistanceMeters = distanceMeters * cos(bearingDifferenceRadians)
    if (forwardDistanceMeters <= 0.0) return null

    val lateralDistanceMeters = distanceMeters * sin(bearingDifferenceRadians)
    val directionHint = camera.explicitDirectionBearingDegrees()
    val lateralDistanceLimit = if (directionHint == null) {
        policy.maxLateralDistanceWithoutDirectionHintMeters
    } else {
        policy.maxLateralDistanceMeters
    }
    if (lateralDistanceMeters > lateralDistanceLimit) return null

    val directionHintPenalty = directionHintPenalty(driverBearingDegrees, policy)
        ?: return null

    return ForwardCameraCandidate(
        camera = this,
        score = bearingDifference + lateralDistanceMeters * LATERAL_DISTANCE_SCORE_WEIGHT +
            directionHintPenalty
    )
}

private fun NearbySpeedCamera.directionHintPenalty(
    driverBearingDegrees: Double,
    policy: ForwardCameraSearchPolicy
): Double? {
    val cameraDirectionBearing = camera.explicitDirectionBearingDegrees() ?: return UNKNOWN_DIRECTION_HINT_PENALTY
    val directionDifference = angularDistanceDegrees(driverBearingDegrees, cameraDirectionBearing)
    if (directionDifference > policy.maxDirectionHintDifferenceDegrees) return null

    return directionDifference * DIRECTION_HINT_SCORE_WEIGHT
}

private fun List<ForwardCameraCandidate>.selectBestCandidatePerPhysicalPoint(
    policy: ForwardCameraSearchPolicy
): List<ForwardCameraCandidate> =
    fold(emptyList()) { selected, candidate ->
        if (selected.any { it.camera.isSamePhysicalPoint(candidate.camera, policy) }) {
            selected
        } else {
            selected + candidate
        }
    }

private fun NearbySpeedCamera.isSamePhysicalPoint(
    other: NearbySpeedCamera,
    policy: ForwardCameraSearchPolicy
): Boolean {
    val cameraDistanceMeters = camera.locationCoordinate.distanceToMeters(other.camera.locationCoordinate)
    val sameRoad = camera.roadName != null && camera.roadName == other.camera.roadName
    return cameraDistanceMeters <= policy.duplicateCameraDistanceMeters &&
        (sameRoad || cameraDistanceMeters <= policy.duplicateCameraDistanceMeters / 2.0)
}

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

private const val LATERAL_DISTANCE_SCORE_WEIGHT = 0.35
private const val DIRECTION_HINT_SCORE_WEIGHT = 0.45
private const val UNKNOWN_DIRECTION_HINT_PENALTY = 8.0
