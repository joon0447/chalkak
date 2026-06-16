package com.joon.chalkak.data.camera.repository

import com.joon.chalkak.data.camera.local.SpeedCameraLocalDataSource
import com.joon.chalkak.data.camera.remote.PublicDataCameraApiClient
import com.joon.chalkak.domain.GeoLocation
import com.joon.chalkak.domain.NearbySpeedCamera
import com.joon.chalkak.domain.SpeedCamera
import com.joon.chalkak.domain.bearingToDegrees
import com.joon.chalkak.domain.boundsForRadius
import com.joon.chalkak.domain.distanceToMeters

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

    fun findNearbyCachedCameras(
        currentLocation: GeoLocation,
        radiusMeters: Double = DEFAULT_SEARCH_RADIUS_METERS,
        maxResults: Int = DEFAULT_MAX_NEARBY_RESULTS
    ): List<NearbySpeedCamera> {
        require(radiusMeters >= 0.0) { "radiusMeters must be 0 or greater." }
        require(maxResults > 0) { "maxResults must be greater than 0." }

        val bounds = currentLocation.boundsForRadius(radiusMeters)
        return localDataSource.findInBounds(
            minLatitude = bounds.minLatitude,
            maxLatitude = bounds.maxLatitude,
            minLongitude = bounds.minLongitude,
            maxLongitude = bounds.maxLongitude
        ).mapNotNull { camera ->
            val distanceMeters = currentLocation.distanceToMeters(camera.locationCoordinate)
            if (distanceMeters <= radiusMeters) {
                NearbySpeedCamera(
                    camera = camera,
                    distanceMeters = distanceMeters,
                    bearingToCameraDegrees = currentLocation.bearingToDegrees(camera.locationCoordinate)
                )
            } else {
                null
            }
        }.sortedBy { it.distanceMeters }
            .take(maxResults)
    }

    fun getLastSyncedAtMillis(): Long? =
        localDataSource.getLastSyncedAtMillis()

    fun clearCache() {
        localDataSource.clear()
    }

    private companion object {
        const val DEFAULT_MAX_PAGES = 50
        const val DEFAULT_SEARCH_RADIUS_METERS = 1_000.0
        const val DEFAULT_MAX_NEARBY_RESULTS = 10
    }
}

data class CameraRefreshResult(
    val savedCount: Int,
    val lastSyncedAtMillis: Long?
)
