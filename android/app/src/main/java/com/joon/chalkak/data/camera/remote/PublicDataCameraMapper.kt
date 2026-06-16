package com.joon.chalkak.data.camera.remote

import com.joon.chalkak.domain.EnforcementSectionPosition
import com.joon.chalkak.domain.EnforcementType
import com.joon.chalkak.domain.ProtectedAreaType
import com.joon.chalkak.domain.SpeedCamera

fun PublicDataCameraDto.toDomain(): SpeedCamera =
    SpeedCamera(
        id = manageNo,
        latitude = latitude ?: 0.0,
        longitude = longitude ?: 0.0,
        location = location ?: roadAddress ?: lotAddress.orEmpty(),
        roadName = roadRouteName,
        roadDirection = roadRouteDirection,
        enforcementType = enforcementTypeCode.toEnforcementType(),
        speedLimitKmh = speedLimitKmh?.takeIf { it > 0 },
        sectionPosition = sectionPositionCode.toSectionPosition(),
        sectionLengthMeters = sectionLengthMeters?.takeIf { it > 0 },
        protectedAreaType = protectedAreaTypeCode.toProtectedAreaType(),
        referenceDate = referenceDate
    )

private fun String?.toEnforcementType(): EnforcementType =
    when (normalizedCode()) {
        "01", "1" -> EnforcementType.SPEED
        "02", "2" -> EnforcementType.SIGNAL
        "03", "3" -> EnforcementType.TRAFFIC_VIOLATION
        "04", "4" -> EnforcementType.PARKING
        "05", "5" -> EnforcementType.BUS_LANE
        "99" -> EnforcementType.ETC
        else -> EnforcementType.UNKNOWN
    }

private fun String?.toSectionPosition(): EnforcementSectionPosition? =
    when (normalizedCode()) {
        null -> null
        "01", "1" -> EnforcementSectionPosition.START
        "02", "2" -> EnforcementSectionPosition.END
        "03", "3" -> EnforcementSectionPosition.SINGLE_POINT
        "99" -> EnforcementSectionPosition.ETC
        else -> EnforcementSectionPosition.UNKNOWN
    }

private fun String?.toProtectedAreaType(): ProtectedAreaType? =
    when (normalizedCode()) {
        null -> null
        "01", "1" -> ProtectedAreaType.CHILDREN
        "02", "2" -> ProtectedAreaType.SENIOR
        "03", "3" -> ProtectedAreaType.DISABLED
        "99" -> ProtectedAreaType.ETC
        else -> ProtectedAreaType.UNKNOWN
    }

private fun String?.normalizedCode(): String? =
    this?.trim()?.takeIf { it.isNotBlank() }?.padStart(2, '0')
