package com.joon.chalkak.data.camera.repository

import com.joon.chalkak.data.camera.local.SpeedCameraLocalDataSource
import com.joon.chalkak.data.camera.remote.PublicDataCameraApiClient
import com.joon.chalkak.domain.SpeedCamera

class SpeedCameraRepository(
    private val remoteDataSource: PublicDataCameraApiClient,
    private val localDataSource: SpeedCameraLocalDataSource
) {
    suspend fun refreshCameras(
        provinceName: String? = null,
        cityName: String? = null,
        pageSize: Int = 1000,
        maxPages: Int = DEFAULT_MAX_PAGES
    ): CameraRefreshResult {
        val cameras = mutableListOf<SpeedCamera>()

        for (page in 1..maxPages) {
            val pageItems = remoteDataSource.fetchCameras(
                pageNo = page,
                numOfRows = pageSize,
                provinceName = provinceName,
                cityName = cityName
            )
            cameras += pageItems

            if (pageItems.size < pageSize) {
                break
            }
        }

        localDataSource.replaceAll(cameras)
        return CameraRefreshResult(
            savedCount = cameras.size,
            lastSyncedAtMillis = localDataSource.getLastSyncedAtMillis()
        )
    }

    fun getCachedCameras(): List<SpeedCamera> =
        localDataSource.getAll()

    fun findCachedCamerasInBounds(
        minLatitude: Double,
        maxLatitude: Double,
        minLongitude: Double,
        maxLongitude: Double
    ): List<SpeedCamera> =
        localDataSource.findInBounds(
            minLatitude = minLatitude,
            maxLatitude = maxLatitude,
            minLongitude = minLongitude,
            maxLongitude = maxLongitude
        )

    fun getLastSyncedAtMillis(): Long? =
        localDataSource.getLastSyncedAtMillis()

    fun clearCache() {
        localDataSource.clear()
    }

    private companion object {
        const val DEFAULT_MAX_PAGES = 50
    }
}

data class CameraRefreshResult(
    val savedCount: Int,
    val lastSyncedAtMillis: Long?
)
