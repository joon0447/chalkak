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
        maxPages: Int = DEFAULT_MAX_PAGES,
        onProgress: suspend (loadedCount: Int) -> Unit = {}
    ): CameraRefreshResult {
        val cameras = mutableListOf<SpeedCamera>()
        var totalCount: Int? = null

        for (page in 1..maxPages) {
            val cameraPage = remoteDataSource.fetchCameraPage(
                pageNo = page,
                numOfRows = pageSize,
                provinceName = provinceName,
                cityName = cityName
            )
            val pageItems = cameraPage.items
            totalCount = cameraPage.totalCount ?: totalCount
            cameras += pageItems
            onProgress(cameras.size)

            if (pageItems.size < pageSize || cameras.size >= (totalCount ?: Int.MAX_VALUE)) {
                break
            }
        }

        localDataSource.replaceAll(cameras, totalCount = totalCount ?: cameras.size)
        return CameraRefreshResult(
            savedCount = cameras.size,
            lastSyncedAtMillis = localDataSource.getLastSyncedAtMillis(),
            totalCount = totalCount,
            referenceDate = cameras.maxReferenceDate()
        )
    }

    suspend fun refreshRegionCameras(
        provinceName: String,
        cityName: String? = null,
        pageSize: Int = 1000,
        maxPages: Int = DEFAULT_MAX_PAGES,
        onProgress: suspend (loadedCount: Int, totalCount: Int?) -> Unit = { _, _ -> }
    ): CameraRefreshResult {
        val cameras = mutableListOf<SpeedCamera>()
        var totalCount: Int? = null
        val syncKey = regionSyncKey(provinceName, cityName)

        for (page in 1..maxPages) {
            val cameraPage = remoteDataSource.fetchCameraPage(
                pageNo = page,
                numOfRows = pageSize,
                provinceName = provinceName,
                cityName = cityName
            )
            val pageItems = cameraPage.items
            totalCount = cameraPage.totalCount ?: totalCount
            cameras += pageItems
            onProgress(cameras.size, totalCount)

            if (pageItems.size < pageSize || cameras.size >= (totalCount ?: Int.MAX_VALUE)) {
                break
            }
        }

        localDataSource.upsertAll(
            cameras = cameras,
            syncKey = syncKey,
            totalCount = totalCount ?: cameras.size
        )
        return CameraRefreshResult(
            savedCount = cameras.size,
            lastSyncedAtMillis = localDataSource.getSyncMetadata(syncKey)?.syncedAtMillis,
            totalCount = totalCount,
            referenceDate = cameras.maxReferenceDate()
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

    fun getCachedCameraCount(): Int =
        localDataSource.getCount()

    fun hasRegionCache(provinceName: String, cityName: String? = null): Boolean {
        val metadata = localDataSource.getSyncMetadata(regionSyncKey(provinceName, cityName))
        return metadata?.itemCount != null && metadata.itemCount > 0
    }

    fun getRegionSyncMetadata(provinceName: String, cityName: String? = null) =
        localDataSource.getSyncMetadata(regionSyncKey(provinceName, cityName))

    fun clearCache() {
        localDataSource.clear()
    }

    private companion object {
        const val DEFAULT_MAX_PAGES = 50
        const val DEFAULT_SEARCH_RADIUS_METERS = 1_000.0
        const val DEFAULT_MAX_NEARBY_RESULTS = 10
    }
}

fun regionSyncKey(provinceName: String, cityName: String? = null): String =
    listOfNotNull("region", provinceName.trim(), cityName?.trim()?.takeIf { it.isNotBlank() })
        .joinToString(":")

data class CameraRefreshResult(
    val savedCount: Int,
    val lastSyncedAtMillis: Long?,
    val totalCount: Int? = null,
    val referenceDate: String? = null
)

private fun List<SpeedCamera>.maxReferenceDate(): String? =
    mapNotNull { it.referenceDate }.maxOrNull()
