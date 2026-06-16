package com.joon.chalkak.domain

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
