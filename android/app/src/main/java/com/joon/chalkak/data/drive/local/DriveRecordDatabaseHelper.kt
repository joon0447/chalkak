package com.joon.chalkak.data.drive.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DriveRecordDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_DRIVE_SESSIONS_TABLE)
        db.execSQL(CREATE_CAMERA_PASS_RECORDS_TABLE)
        db.execSQL(CREATE_PASS_SESSION_INDEX)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL(ADD_DRIVE_SESSION_SOURCE_COLUMN)
            return
        }

        db.execSQL("DROP TABLE IF EXISTS $TABLE_CAMERA_PASS_RECORDS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_DRIVE_SESSIONS")
        onCreate(db)
    }

    companion object {
        const val DATABASE_NAME = "chalkak_drive_records.db"
        const val DATABASE_VERSION = 2

        const val TABLE_DRIVE_SESSIONS = "drive_sessions"
        const val TABLE_CAMERA_PASS_RECORDS = "camera_pass_records"
    }
}

private const val CREATE_DRIVE_SESSIONS_TABLE = """
    CREATE TABLE drive_sessions (
        id TEXT PRIMARY KEY NOT NULL,
        started_at_millis INTEGER NOT NULL,
        ended_at_millis INTEGER,
        source TEXT NOT NULL DEFAULT 'MANUAL'
    )
"""

private const val ADD_DRIVE_SESSION_SOURCE_COLUMN = """
    ALTER TABLE drive_sessions ADD COLUMN source TEXT NOT NULL DEFAULT 'MANUAL'
"""

private const val CREATE_CAMERA_PASS_RECORDS_TABLE = """
    CREATE TABLE camera_pass_records (
        id TEXT PRIMARY KEY NOT NULL,
        session_id TEXT NOT NULL,
        camera_id TEXT NOT NULL,
        camera_location TEXT NOT NULL,
        road_name TEXT,
        speed_limit_kmh INTEGER,
        passed_at_millis INTEGER NOT NULL,
        measured_speed_kmh INTEGER NOT NULL,
        location_accuracy_meters REAL,
        distance_to_camera_meters REAL,
        judgement_result TEXT NOT NULL,
        enforcement_threshold_kmh INTEGER,
        FOREIGN KEY(session_id) REFERENCES drive_sessions(id)
    )
"""

private const val CREATE_PASS_SESSION_INDEX = """
    CREATE INDEX idx_camera_pass_records_session_id ON camera_pass_records(session_id)
"""
