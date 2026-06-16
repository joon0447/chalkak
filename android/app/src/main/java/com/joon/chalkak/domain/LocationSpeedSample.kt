package com.joon.chalkak.domain

import kotlin.math.roundToInt

data class LocationReading(
    val location: GeoLocation,
    val elapsedRealtimeMillis: Long,
    val speedMetersPerSecond: Double?,
    val accuracyMeters: Float?,
    val bearingDegrees: Double?
)

data class LocationSpeedSample(
    val location: GeoLocation,
    val speedKmh: Double,
    val accuracyMeters: Float?,
    val measuredAtMillis: Long,
    val bearingDegrees: Double?
) {
    val roundedSpeedKmh: Int
        get() = speedKmh.roundToInt().coerceAtLeast(0)
}

fun LocationReading.toSpeedSample(previous: LocationReading?): LocationSpeedSample {
    val speedKmh = speedMetersPerSecond
        ?.takeIf { it >= 0.0 }
        ?.let { it * KMH_PER_METER_PER_SECOND }
        ?: calculateFallbackSpeedKmh(previous)

    return LocationSpeedSample(
        location = location,
        speedKmh = speedKmh.coerceAtLeast(0.0),
        accuracyMeters = accuracyMeters,
        measuredAtMillis = elapsedRealtimeMillis,
        bearingDegrees = bearingDegrees ?: previous?.location?.bearingToDegrees(location)
    )
}

private fun LocationReading.calculateFallbackSpeedKmh(previous: LocationReading?): Double {
    if (previous == null) return 0.0

    val elapsedSeconds = (elapsedRealtimeMillis - previous.elapsedRealtimeMillis) / 1000.0
    if (elapsedSeconds <= 0.0) return 0.0

    return location.distanceToMeters(previous.location) / elapsedSeconds * KMH_PER_METER_PER_SECOND
}

private const val KMH_PER_METER_PER_SECOND = 3.6
