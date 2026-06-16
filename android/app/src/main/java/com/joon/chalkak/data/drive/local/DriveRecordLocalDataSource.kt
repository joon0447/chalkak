package com.joon.chalkak.data.drive.local

import android.content.ContentValues
import android.database.Cursor
import com.joon.chalkak.domain.CameraPassRecord
import com.joon.chalkak.domain.DriveSession
import com.joon.chalkak.domain.DrivingStatus
import com.joon.chalkak.domain.EnforcementType
import com.joon.chalkak.domain.SpeedCamera
import com.joon.chalkak.domain.SpeedJudgement
import com.joon.chalkak.domain.SpeedJudgementResult
import com.joon.chalkak.model.CameraPassDetail
import com.joon.chalkak.model.DriveRecord
import com.joon.chalkak.model.DriveRecordGroup
import com.joon.chalkak.model.HistorySummary
import com.joon.chalkak.model.RecentRecord
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class DriveRecordLocalDataSource(
    private val databaseHelper: DriveRecordDatabaseHelper
) {
    fun startSession(session: DriveSession) {
        databaseHelper.writableDatabase.insertWithOnConflict(
            DriveRecordDatabaseHelper.TABLE_DRIVE_SESSIONS,
            null,
            ContentValues().apply {
                put("id", session.id)
                put("started_at_millis", session.startedAtMillis)
                put("ended_at_millis", session.endedAtMillis)
            },
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun finishSession(sessionId: String, endedAtMillis: Long) {
        val values = ContentValues().apply {
            put("ended_at_millis", endedAtMillis)
        }
        databaseHelper.writableDatabase.update(
            DriveRecordDatabaseHelper.TABLE_DRIVE_SESSIONS,
            values,
            "id = ?",
            arrayOf(sessionId)
        )
    }

    fun insertPassRecord(record: CameraPassRecord) {
        databaseHelper.writableDatabase.insertWithOnConflict(
            DriveRecordDatabaseHelper.TABLE_CAMERA_PASS_RECORDS,
            null,
            record.toContentValues(),
            android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE
        )
    }

    fun getRecentRecords(limit: Int = 3): List<RecentRecord> {
        val records = queryPassRecords(orderBy = "passed_at_millis DESC", limit = limit.toString())
        return records.map { record ->
            RecentRecord(
                title = "${record.camera.location} · ${record.measuredSpeedKmh} km/h",
                subtitle = "${record.passedAtMillis.toTimeText()} · 제한 ${record.camera.speedLimitKmh ?: "-"}km/h"
            )
        }
    }

    fun getHistorySummary(): HistorySummary {
        val sessions = querySessions()
        val passRecords = queryPassRecords(orderBy = "passed_at_millis DESC")
        val safeCount = passRecords.count { it.judgement.result == SpeedJudgementResult.SAFE }
        val riskyCount = passRecords.count {
            it.judgement.result == SpeedJudgementResult.WARNING ||
                it.judgement.result == SpeedJudgementResult.ENFORCEMENT_RISK
        }
        val safeRate = if (passRecords.isEmpty()) {
            0
        } else {
            (safeCount * 100) / passRecords.size
        }

        return HistorySummary(
            totalDriveCount = "${sessions.size}회",
            safePassRate = "$safeRate%",
            warningCount = "${riskyCount}건"
        )
    }

    fun getDriveRecordGroups(): List<DriveRecordGroup> {
        val passRecordsBySession = queryPassRecords(orderBy = "passed_at_millis ASC")
            .groupBy { it.sessionId }

        return querySessions()
            .sortedByDescending { it.startedAtMillis }
            .map { session ->
                val records = passRecordsBySession[session.id].orEmpty()
                DriveRecordGroup(
                    date = session.startedAtMillis.toDateLabel(),
                    records = listOf(session.toDriveRecord(records))
                )
            }
    }

    fun clear() {
        val db = databaseHelper.writableDatabase
        db.beginTransaction()
        try {
            db.delete(DriveRecordDatabaseHelper.TABLE_CAMERA_PASS_RECORDS, null, null)
            db.delete(DriveRecordDatabaseHelper.TABLE_DRIVE_SESSIONS, null, null)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun deleteRecordsOlderThan(cutoffMillis: Long) {
        val db = databaseHelper.writableDatabase
        db.beginTransaction()
        try {
            db.delete(
                DriveRecordDatabaseHelper.TABLE_CAMERA_PASS_RECORDS,
                "session_id IN (SELECT id FROM ${DriveRecordDatabaseHelper.TABLE_DRIVE_SESSIONS} WHERE started_at_millis < ?)",
                arrayOf(cutoffMillis.toString())
            )
            db.delete(
                DriveRecordDatabaseHelper.TABLE_DRIVE_SESSIONS,
                "started_at_millis < ?",
                arrayOf(cutoffMillis.toString())
            )
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun DriveSession.toDriveRecord(records: List<CameraPassRecord>): DriveRecord {
        val warningCount = records.count {
            it.judgement.result == SpeedJudgementResult.WARNING ||
                it.judgement.result == SpeedJudgementResult.ENFORCEMENT_RISK
        }
        val safeCount = records.count { it.judgement.result == SpeedJudgementResult.SAFE }
        val status = when {
            warningCount > 0 -> DrivingStatus.WARNING
            records.isEmpty() -> DrivingStatus.UNKNOWN
            else -> DrivingStatus.SAFE
        }

        return DriveRecord(
            id = id,
            time = "${startedAtMillis.toTimeText()} ~ ${(endedAtMillis ?: System.currentTimeMillis()).toTimeText()}",
            route = records.routeText(),
            cameraCount = "카메라 ${records.size}개",
            safeCount = "안전 $safeCount",
            warningText = if (warningCount > 0) "주의 $warningCount" else "",
            status = status,
            cameraPasses = records.map { it.toCameraPassDetail() }
        )
    }

    private fun CameraPassRecord.toCameraPassDetail(): CameraPassDetail =
        CameraPassDetail(
            passedTime = passedAtMillis.toTimeText(),
            location = camera.location,
            roadName = camera.roadName,
            measuredSpeedText = "${measuredSpeedKmh}km/h",
            speedLimitText = camera.speedLimitKmh?.let { "${it}km/h" } ?: "-",
            enforcementThresholdText = judgement.enforcementThresholdKmh?.let { "${it}km/h" } ?: "-",
            distanceText = distanceToCameraMeters?.let { "${it.toInt()}m" } ?: "-",
            status = judgement.result.toDrivingStatus()
        )

    private fun SpeedJudgementResult.toDrivingStatus(): DrivingStatus =
        when (this) {
            SpeedJudgementResult.SAFE -> DrivingStatus.SAFE
            SpeedJudgementResult.WARNING,
            SpeedJudgementResult.ENFORCEMENT_RISK -> DrivingStatus.WARNING
            SpeedJudgementResult.UNKNOWN -> DrivingStatus.UNKNOWN
        }

    private fun List<CameraPassRecord>.routeText(): String =
        map { it.camera.roadName ?: it.camera.location }
            .distinct()
            .take(2)
            .ifEmpty { listOf("경로 미확인") }
            .joinToString("  >  ")

    private fun querySessions(): List<DriveSession> {
        val db = databaseHelper.readableDatabase
        return db.query(
            DriveRecordDatabaseHelper.TABLE_DRIVE_SESSIONS,
            arrayOf("id", "started_at_millis", "ended_at_millis"),
            null,
            null,
            null,
            null,
            null
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        DriveSession(
                            id = cursor.getString(0),
                            startedAtMillis = cursor.getLong(1),
                            endedAtMillis = if (cursor.isNull(2)) null else cursor.getLong(2)
                        )
                    )
                }
            }
        }
    }

    private fun queryPassRecords(orderBy: String, limit: String? = null): List<CameraPassRecord> {
        val db = databaseHelper.readableDatabase
        return db.query(
            DriveRecordDatabaseHelper.TABLE_CAMERA_PASS_RECORDS,
            PASS_COLUMNS,
            null,
            null,
            null,
            null,
            orderBy,
            limit
        ).use { cursor -> cursor.toPassRecords() }
    }

    private fun Cursor.toPassRecords(): List<CameraPassRecord> =
        buildList {
            while (moveToNext()) {
                val judgementResult = SpeedJudgementResult.valueOf(getString(getColumnIndexOrThrow("judgement_result")))
                val speedLimitKmh = getNullableInt("speed_limit_kmh")
                add(
                    CameraPassRecord(
                        id = getString(getColumnIndexOrThrow("id")),
                        sessionId = getString(getColumnIndexOrThrow("session_id")),
                        camera = SpeedCamera(
                            id = getString(getColumnIndexOrThrow("camera_id")),
                            latitude = 0.0,
                            longitude = 0.0,
                            location = getString(getColumnIndexOrThrow("camera_location")),
                            roadName = getNullableString("road_name"),
                            roadDirection = null,
                            enforcementType = EnforcementType.SPEED,
                            speedLimitKmh = speedLimitKmh,
                            sectionPosition = null,
                            sectionLengthMeters = null,
                            protectedAreaType = null,
                            referenceDate = null
                        ),
                        passedAtMillis = getLong(getColumnIndexOrThrow("passed_at_millis")),
                        measuredSpeedKmh = getInt(getColumnIndexOrThrow("measured_speed_kmh")),
                        locationAccuracyMeters = getNullableFloat("location_accuracy_meters"),
                        distanceToCameraMeters = getNullableDouble("distance_to_camera_meters"),
                        judgement = SpeedJudgement(
                            result = judgementResult,
                            measuredSpeedKmh = getInt(getColumnIndexOrThrow("measured_speed_kmh")),
                            speedLimitKmh = speedLimitKmh,
                            enforcementThresholdKmh = getNullableInt("enforcement_threshold_kmh")
                        )
                    )
                )
            }
        }

    private fun CameraPassRecord.toContentValues(): ContentValues =
        ContentValues().apply {
            put("id", id)
            put("session_id", sessionId)
            put("camera_id", camera.id)
            put("camera_location", camera.location)
            put("road_name", camera.roadName)
            put("speed_limit_kmh", camera.speedLimitKmh)
            put("passed_at_millis", passedAtMillis)
            put("measured_speed_kmh", measuredSpeedKmh)
            put("location_accuracy_meters", locationAccuracyMeters)
            put("distance_to_camera_meters", distanceToCameraMeters)
            put("judgement_result", judgement.result.name)
            put("enforcement_threshold_kmh", judgement.enforcementThresholdKmh)
        }

    private fun Cursor.getNullableString(columnName: String): String? {
        val index = getColumnIndexOrThrow(columnName)
        return if (isNull(index)) null else getString(index)
    }

    private fun Cursor.getNullableInt(columnName: String): Int? {
        val index = getColumnIndexOrThrow(columnName)
        return if (isNull(index)) null else getInt(index)
    }

    private fun Cursor.getNullableFloat(columnName: String): Float? {
        val index = getColumnIndexOrThrow(columnName)
        return if (isNull(index)) null else getFloat(index)
    }

    private fun Cursor.getNullableDouble(columnName: String): Double? {
        val index = getColumnIndexOrThrow(columnName)
        return if (isNull(index)) null else getDouble(index)
    }

    private fun Long.toTimeText(): String =
        TIME_FORMATTER.format(Instant.ofEpochMilli(this).atZone(SEOUL_ZONE))

    private fun Long.toDateLabel(): String =
        DATE_FORMATTER.format(Instant.ofEpochMilli(this).atZone(SEOUL_ZONE))

    private companion object {
        val SEOUL_ZONE: ZoneId = ZoneId.of("Asia/Seoul")
        val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.KOREAN)
        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN)
        val PASS_COLUMNS = arrayOf(
            "id",
            "session_id",
            "camera_id",
            "camera_location",
            "road_name",
            "speed_limit_kmh",
            "passed_at_millis",
            "measured_speed_kmh",
            "location_accuracy_meters",
            "distance_to_camera_meters",
            "judgement_result",
            "enforcement_threshold_kmh"
        )
    }
}
