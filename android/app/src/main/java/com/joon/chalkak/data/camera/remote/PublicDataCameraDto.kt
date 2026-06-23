package com.joon.chalkak.data.camera.remote

data class PublicDataCameraDto(
    val manageNo: String,
    val provinceName: String?,
    val cityName: String?,
    val roadKind: String?,
    val roadRouteNo: String?,
    val roadRouteName: String?,
    val roadRouteDirection: String?,
    val roadAddress: String?,
    val lotAddress: String?,
    val latitude: Double?,
    val longitude: Double?,
    val location: String?,
    val enforcementTypeCode: String?,
    val speedLimitKmh: Int?,
    val sectionPositionCode: String?,
    val sectionLengthMeters: Int?,
    val protectedAreaTypeCode: String?,
    val installationYear: String?,
    val institutionName: String?,
    val phoneNumber: String?,
    val referenceDate: String?,
    val institutionCode: String?
)

data class PublicDataCameraPageDto(
    val items: List<PublicDataCameraDto>,
    val totalCount: Int?
)
