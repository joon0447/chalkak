package com.joon.chalkak.domain

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class GeoLocation(
    val latitude: Double,
    val longitude: Double
) {
    init {
        require(latitude in -90.0..90.0) { "latitude must be between -90 and 90." }
        require(longitude in -180.0..180.0) { "longitude must be between -180 and 180." }
    }
}

data class GeoBounds(
    val minLatitude: Double,
    val maxLatitude: Double,
    val minLongitude: Double,
    val maxLongitude: Double
)

fun GeoLocation.distanceToMeters(other: GeoLocation): Double {
    val latitudeDelta = (other.latitude - latitude).toRadians()
    val longitudeDelta = (other.longitude - longitude).toRadians()
    val startLatitude = latitude.toRadians()
    val endLatitude = other.latitude.toRadians()

    val haversine = sin(latitudeDelta / 2).square() +
        cos(startLatitude) * cos(endLatitude) * sin(longitudeDelta / 2).square()

    return EARTH_RADIUS_METERS * 2 * asin(sqrt(haversine.coerceIn(0.0, 1.0)))
}

fun GeoLocation.bearingToDegrees(other: GeoLocation): Double {
    val startLatitude = latitude.toRadians()
    val endLatitude = other.latitude.toRadians()
    val longitudeDelta = (other.longitude - longitude).toRadians()
    val y = sin(longitudeDelta) * cos(endLatitude)
    val x = cos(startLatitude) * sin(endLatitude) -
        sin(startLatitude) * cos(endLatitude) * cos(longitudeDelta)

    return atan2(y, x).toDegrees().normalizeDegrees()
}

fun angularDistanceDegrees(first: Double, second: Double): Double {
    val difference = kotlin.math.abs(first.normalizeDegrees() - second.normalizeDegrees())
    return kotlin.math.min(difference, 360.0 - difference)
}

fun GeoLocation.boundsForRadius(radiusMeters: Double): GeoBounds {
    require(radiusMeters >= 0.0) { "radiusMeters must be 0 or greater." }

    val latitudeDelta = radiusMeters / METERS_PER_DEGREE_LATITUDE
    val longitudeDelta = radiusMeters / longitudeMetersPerDegree().coerceAtLeast(MIN_LONGITUDE_METERS)

    return GeoBounds(
        minLatitude = (latitude - latitudeDelta).coerceAtLeast(-90.0),
        maxLatitude = (latitude + latitudeDelta).coerceAtMost(90.0),
        minLongitude = (longitude - longitudeDelta).coerceAtLeast(-180.0),
        maxLongitude = (longitude + longitudeDelta).coerceAtMost(180.0)
    )
}

private fun GeoLocation.longitudeMetersPerDegree(): Double =
    METERS_PER_DEGREE_LATITUDE * cos(latitude.toRadians())

private fun Double.toRadians(): Double =
    this * PI / 180.0

private fun Double.toDegrees(): Double =
    this * 180.0 / PI

private fun Double.normalizeDegrees(): Double =
    ((this % 360.0) + 360.0) % 360.0

private fun Double.square(): Double =
    this * this

private const val EARTH_RADIUS_METERS = 6_371_000.0
private const val METERS_PER_DEGREE_LATITUDE = 111_320.0
private const val MIN_LONGITUDE_METERS = 1.0
