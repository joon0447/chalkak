package com.joon.chalkak.data.camera.local

import com.joon.chalkak.domain.EnforcementSectionPosition
import com.joon.chalkak.domain.EnforcementType
import com.joon.chalkak.domain.ProtectedAreaType
import com.joon.chalkak.domain.SpeedCamera

data class SpeedCameraEntity(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val location: String,
    val roadName: String?,
    val roadDirection: String?,
    val enforcementType: String,
    val speedLimitKmh: Int?,
    val sectionPosition: String?,
    val sectionLengthMeters: Int?,
    val protectedAreaType: String?,
    val referenceDate: String?
)

fun SpeedCamera.toEntity(): SpeedCameraEntity =
    SpeedCameraEntity(
        id = id,
        latitude = latitude,
        longitude = longitude,
        location = location,
        roadName = roadName,
        roadDirection = roadDirection,
        enforcementType = enforcementType.name,
        speedLimitKmh = speedLimitKmh,
        sectionPosition = sectionPosition?.name,
        sectionLengthMeters = sectionLengthMeters,
        protectedAreaType = protectedAreaType?.name,
        referenceDate = referenceDate
    )

fun SpeedCameraEntity.toDomain(): SpeedCamera =
    SpeedCamera(
        id = id,
        latitude = latitude,
        longitude = longitude,
        location = location,
        roadName = roadName,
        roadDirection = roadDirection,
        enforcementType = enumValueOrDefault(enforcementType, EnforcementType.UNKNOWN),
        speedLimitKmh = speedLimitKmh,
        sectionPosition = sectionPosition?.let {
            enumValueOrDefault(it, EnforcementSectionPosition.UNKNOWN)
        },
        sectionLengthMeters = sectionLengthMeters,
        protectedAreaType = protectedAreaType?.let {
            enumValueOrDefault(it, ProtectedAreaType.UNKNOWN)
        },
        referenceDate = referenceDate
    )

private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, default: T): T =
    enumValues<T>().firstOrNull { it.name == value } ?: default
