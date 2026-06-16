package com.joon.chalkak.domain

data class SpeedCamera(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val location: String,
    val roadName: String?,
    val roadDirection: String?,
    val enforcementType: EnforcementType,
    val speedLimitKmh: Int?,
    val sectionPosition: EnforcementSectionPosition?,
    val sectionLengthMeters: Int?,
    val protectedAreaType: ProtectedAreaType?,
    val referenceDate: String?
) {
    val locationCoordinate: GeoLocation
        get() = GeoLocation(latitude = latitude, longitude = longitude)
}

enum class EnforcementType {
    SPEED,
    SIGNAL,
    TRAFFIC_VIOLATION,
    BUS_LANE,
    PARKING,
    ETC,
    UNKNOWN
}

enum class EnforcementSectionPosition {
    START,
    END,
    SINGLE_POINT,
    ETC,
    UNKNOWN
}

enum class ProtectedAreaType {
    CHILDREN,
    SENIOR,
    DISABLED,
    ETC,
    NONE,
    UNKNOWN
}
