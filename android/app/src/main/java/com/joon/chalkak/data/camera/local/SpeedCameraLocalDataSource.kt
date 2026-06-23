package com.joon.chalkak.data.camera.local

import android.content.ContentValues
import android.database.Cursor
import com.joon.chalkak.data.camera.local.SpeedCameraDatabaseHelper.Companion.COLUMN_ENFORCEMENT_TYPE
import com.joon.chalkak.data.camera.local.SpeedCameraDatabaseHelper.Companion.COLUMN_ID
import com.joon.chalkak.data.camera.local.SpeedCameraDatabaseHelper.Companion.COLUMN_ITEM_COUNT
import com.joon.chalkak.data.camera.local.SpeedCameraDatabaseHelper.Companion.COLUMN_LATITUDE
import com.joon.chalkak.data.camera.local.SpeedCameraDatabaseHelper.Companion.COLUMN_LOCATION
import com.joon.chalkak.data.camera.local.SpeedCameraDatabaseHelper.Companion.COLUMN_LONGITUDE
import com.joon.chalkak.data.camera.local.SpeedCameraDatabaseHelper.Companion.COLUMN_PROTECTED_AREA_TYPE
import com.joon.chalkak.data.camera.local.SpeedCameraDatabaseHelper.Companion.COLUMN_REFERENCE_DATE
import com.joon.chalkak.data.camera.local.SpeedCameraDatabaseHelper.Companion.COLUMN_ROAD_DIRECTION
import com.joon.chalkak.data.camera.local.SpeedCameraDatabaseHelper.Companion.COLUMN_ROAD_NAME
import com.joon.chalkak.data.camera.local.SpeedCameraDatabaseHelper.Companion.COLUMN_SECTION_LENGTH_METERS
import com.joon.chalkak.data.camera.local.SpeedCameraDatabaseHelper.Companion.COLUMN_SECTION_POSITION
import com.joon.chalkak.data.camera.local.SpeedCameraDatabaseHelper.Companion.COLUMN_SPEED_LIMIT_KMH
import com.joon.chalkak.data.camera.local.SpeedCameraDatabaseHelper.Companion.COLUMN_SYNCED_AT_MILLIS
import com.joon.chalkak.data.camera.local.SpeedCameraDatabaseHelper.Companion.COLUMN_SYNC_KEY
import com.joon.chalkak.data.camera.local.SpeedCameraDatabaseHelper.Companion.COLUMN_SYNC_REFERENCE_DATE
import com.joon.chalkak.data.camera.local.SpeedCameraDatabaseHelper.Companion.TABLE_SPEED_CAMERAS
import com.joon.chalkak.data.camera.local.SpeedCameraDatabaseHelper.Companion.TABLE_SYNC_METADATA
import com.joon.chalkak.domain.SpeedCamera

class SpeedCameraLocalDataSource(
    private val databaseHelper: SpeedCameraDatabaseHelper
) {
    fun replaceAll(
        cameras: List<SpeedCamera>,
        syncedAtMillis: Long = System.currentTimeMillis(),
        syncKey: String = CAMERA_SYNC_KEY,
        totalCount: Int? = cameras.size
    ) {
        val db = databaseHelper.writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_SPEED_CAMERAS, null, null)
            insertCameras(cameras)
            upsertSyncMetadata(syncKey, syncedAtMillis, totalCount, cameras.maxReferenceDate())
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun upsertAll(
        cameras: List<SpeedCamera>,
        syncKey: String,
        syncedAtMillis: Long = System.currentTimeMillis(),
        totalCount: Int? = cameras.size
    ) {
        val db = databaseHelper.writableDatabase
        db.beginTransaction()
        try {
            insertCameras(cameras)
            upsertSyncMetadata(syncKey, syncedAtMillis, totalCount, cameras.maxReferenceDate())
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getAll(): List<SpeedCamera> {
        val db = databaseHelper.readableDatabase
        return db.query(TABLE_SPEED_CAMERAS, ALL_COLUMNS, null, null, null, null, null)
            .use { cursor -> cursor.toSpeedCameras() }
    }

    fun getCount(): Int {
        val db = databaseHelper.readableDatabase
        return db.rawQuery("SELECT COUNT(*) FROM $TABLE_SPEED_CAMERAS", null).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    fun findInBounds(
        minLatitude: Double,
        maxLatitude: Double,
        minLongitude: Double,
        maxLongitude: Double
    ): List<SpeedCamera> {
        val db = databaseHelper.readableDatabase
        return db.query(
            TABLE_SPEED_CAMERAS,
            ALL_COLUMNS,
            "$COLUMN_LATITUDE BETWEEN ? AND ? AND $COLUMN_LONGITUDE BETWEEN ? AND ?",
            arrayOf(
                minLatitude.toString(),
                maxLatitude.toString(),
                minLongitude.toString(),
                maxLongitude.toString()
            ),
            null,
            null,
            null
        ).use { cursor -> cursor.toSpeedCameras() }
    }

    fun getLastSyncedAtMillis(): Long? {
        return getSyncMetadata(CAMERA_SYNC_KEY)?.syncedAtMillis
    }

    fun getSyncMetadata(syncKey: String): CameraSyncMetadata? {
        val db = databaseHelper.readableDatabase
        return db.query(
            TABLE_SYNC_METADATA,
            arrayOf(COLUMN_SYNCED_AT_MILLIS, COLUMN_ITEM_COUNT, COLUMN_SYNC_REFERENCE_DATE),
            "$COLUMN_SYNC_KEY = ?",
            arrayOf(syncKey),
            null,
            null,
            null
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                CameraSyncMetadata(
                    syncKey = syncKey,
                    syncedAtMillis = cursor.getLong(0),
                    itemCount = if (cursor.isNull(1)) null else cursor.getInt(1),
                    referenceDate = if (cursor.isNull(2)) null else cursor.getString(2)
                )
            } else {
                null
            }
        }
    }

    fun clear() {
        val db = databaseHelper.writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_SPEED_CAMERAS, null, null)
            db.delete(TABLE_SYNC_METADATA, null, null)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun Cursor.toSpeedCameras(): List<SpeedCamera> =
        buildList {
            while (moveToNext()) {
                add(
                    SpeedCameraEntity(
                        id = getString(getColumnIndexOrThrow(COLUMN_ID)),
                        latitude = getDouble(getColumnIndexOrThrow(COLUMN_LATITUDE)),
                        longitude = getDouble(getColumnIndexOrThrow(COLUMN_LONGITUDE)),
                        location = getString(getColumnIndexOrThrow(COLUMN_LOCATION)),
                        roadName = getNullableString(COLUMN_ROAD_NAME),
                        roadDirection = getNullableString(COLUMN_ROAD_DIRECTION),
                        enforcementType = getString(getColumnIndexOrThrow(COLUMN_ENFORCEMENT_TYPE)),
                        speedLimitKmh = getNullableInt(COLUMN_SPEED_LIMIT_KMH),
                        sectionPosition = getNullableString(COLUMN_SECTION_POSITION),
                        sectionLengthMeters = getNullableInt(COLUMN_SECTION_LENGTH_METERS),
                        protectedAreaType = getNullableString(COLUMN_PROTECTED_AREA_TYPE),
                        referenceDate = getNullableString(COLUMN_REFERENCE_DATE)
                    ).toDomain()
                )
            }
        }

    private fun insertCameras(cameras: List<SpeedCamera>) {
        val db = databaseHelper.writableDatabase
        cameras.forEach { camera ->
            db.insertWithOnConflict(
                TABLE_SPEED_CAMERAS,
                null,
                camera.toEntity().toContentValues(),
                android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
            )
        }
    }

    private fun upsertSyncMetadata(
        syncKey: String,
        syncedAtMillis: Long,
        itemCount: Int?,
        referenceDate: String?
    ) {
        databaseHelper.writableDatabase.insertWithOnConflict(
            TABLE_SYNC_METADATA,
            null,
            ContentValues().apply {
                put(COLUMN_SYNC_KEY, syncKey)
                put(COLUMN_SYNCED_AT_MILLIS, syncedAtMillis)
                put(COLUMN_ITEM_COUNT, itemCount)
                put(COLUMN_SYNC_REFERENCE_DATE, referenceDate)
            },
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    private fun List<SpeedCamera>.maxReferenceDate(): String? =
        mapNotNull { it.referenceDate }.maxOrNull()

    private fun Cursor.getNullableString(columnName: String): String? {
        val index = getColumnIndexOrThrow(columnName)
        return if (isNull(index)) null else getString(index)
    }

    private fun Cursor.getNullableInt(columnName: String): Int? {
        val index = getColumnIndexOrThrow(columnName)
        return if (isNull(index)) null else getInt(index)
    }

    private fun SpeedCameraEntity.toContentValues(): ContentValues =
        ContentValues().apply {
            put(COLUMN_ID, id)
            put(COLUMN_LATITUDE, latitude)
            put(COLUMN_LONGITUDE, longitude)
            put(COLUMN_LOCATION, location)
            put(COLUMN_ROAD_NAME, roadName)
            put(COLUMN_ROAD_DIRECTION, roadDirection)
            put(COLUMN_ENFORCEMENT_TYPE, enforcementType)
            put(COLUMN_SPEED_LIMIT_KMH, speedLimitKmh)
            put(COLUMN_SECTION_POSITION, sectionPosition)
            put(COLUMN_SECTION_LENGTH_METERS, sectionLengthMeters)
            put(COLUMN_PROTECTED_AREA_TYPE, protectedAreaType)
            put(COLUMN_REFERENCE_DATE, referenceDate)
        }

    private companion object {
        const val CAMERA_SYNC_KEY = "speed_camera"
        val ALL_COLUMNS = arrayOf(
            COLUMN_ID,
            COLUMN_LATITUDE,
            COLUMN_LONGITUDE,
            COLUMN_LOCATION,
            COLUMN_ROAD_NAME,
            COLUMN_ROAD_DIRECTION,
            COLUMN_ENFORCEMENT_TYPE,
            COLUMN_SPEED_LIMIT_KMH,
            COLUMN_SECTION_POSITION,
            COLUMN_SECTION_LENGTH_METERS,
            COLUMN_PROTECTED_AREA_TYPE,
            COLUMN_REFERENCE_DATE
        )
    }
}

data class CameraSyncMetadata(
    val syncKey: String,
    val syncedAtMillis: Long,
    val itemCount: Int?,
    val referenceDate: String?
)
