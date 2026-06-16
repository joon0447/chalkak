package com.joon.chalkak.data.camera.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class SpeedCameraDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_SPEED_CAMERAS_TABLE)
        db.execSQL(CREATE_SYNC_METADATA_TABLE)
        db.execSQL(CREATE_LATITUDE_INDEX)
        db.execSQL(CREATE_LONGITUDE_INDEX)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SPEED_CAMERAS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SYNC_METADATA")
        onCreate(db)
    }

    companion object {
        const val DATABASE_NAME = "chalkak_camera_cache.db"
        const val DATABASE_VERSION = 1

        const val TABLE_SPEED_CAMERAS = "speed_cameras"
        const val TABLE_SYNC_METADATA = "sync_metadata"

        const val COLUMN_ID = "id"
        const val COLUMN_LATITUDE = "latitude"
        const val COLUMN_LONGITUDE = "longitude"
        const val COLUMN_LOCATION = "location"
        const val COLUMN_ROAD_NAME = "road_name"
        const val COLUMN_ROAD_DIRECTION = "road_direction"
        const val COLUMN_ENFORCEMENT_TYPE = "enforcement_type"
        const val COLUMN_SPEED_LIMIT_KMH = "speed_limit_kmh"
        const val COLUMN_SECTION_POSITION = "section_position"
        const val COLUMN_SECTION_LENGTH_METERS = "section_length_meters"
        const val COLUMN_PROTECTED_AREA_TYPE = "protected_area_type"
        const val COLUMN_REFERENCE_DATE = "reference_date"

        const val COLUMN_SYNC_KEY = "sync_key"
        const val COLUMN_SYNCED_AT_MILLIS = "synced_at_millis"
    }
}

private const val CREATE_SPEED_CAMERAS_TABLE = """
    CREATE TABLE speed_cameras (
        id TEXT PRIMARY KEY NOT NULL,
        latitude REAL NOT NULL,
        longitude REAL NOT NULL,
        location TEXT NOT NULL,
        road_name TEXT,
        road_direction TEXT,
        enforcement_type TEXT NOT NULL,
        speed_limit_kmh INTEGER,
        section_position TEXT,
        section_length_meters INTEGER,
        protected_area_type TEXT,
        reference_date TEXT
    )
"""

private const val CREATE_SYNC_METADATA_TABLE = """
    CREATE TABLE sync_metadata (
        sync_key TEXT PRIMARY KEY NOT NULL,
        synced_at_millis INTEGER NOT NULL
    )
"""

private const val CREATE_LATITUDE_INDEX = """
    CREATE INDEX idx_speed_cameras_latitude ON speed_cameras(latitude)
"""

private const val CREATE_LONGITUDE_INDEX = """
    CREATE INDEX idx_speed_cameras_longitude ON speed_cameras(longitude)
"""
