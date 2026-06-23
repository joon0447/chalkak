package com.joon.chalkak.data.driving

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.joon.chalkak.R
import com.joon.chalkak.data.camera.local.SpeedCameraDatabaseHelper
import com.joon.chalkak.data.camera.local.SpeedCameraLocalDataSource
import com.joon.chalkak.data.camera.remote.PublicDataCameraApiClient
import com.joon.chalkak.data.camera.repository.SpeedCameraRepository
import com.joon.chalkak.data.drive.local.DriveRecordDatabaseHelper
import com.joon.chalkak.data.drive.local.DriveRecordLocalDataSource
import com.joon.chalkak.data.location.AndroidLocationSpeedTracker
import com.joon.chalkak.domain.CameraPassRecord
import com.joon.chalkak.domain.DriveSession
import com.joon.chalkak.domain.DriveSessionSource
import com.joon.chalkak.domain.LocationSpeedSample
import com.joon.chalkak.domain.NearbySpeedCamera
import com.joon.chalkak.domain.SpeedJudgementResult
import com.joon.chalkak.domain.driving.CameraPassDetector
import com.joon.chalkak.domain.driving.DrivingBearingStabilizer
import com.joon.chalkak.domain.driving.DrivingStartDetector
import com.joon.chalkak.domain.driving.DrivingStopDetector
import com.joon.chalkak.domain.driving.DrivingDetectionState
import com.joon.chalkak.domain.driving.DrivingDetectionStatus
import com.joon.chalkak.domain.filterForwardCorridor
import com.joon.chalkak.presentation.main.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class DrivingDetectionService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var state = DrivingDetectionState()
    private var locationJob: Job? = null
    private var currentSession: DriveSession? = null

    private val locationSpeedTracker by lazy { AndroidLocationSpeedTracker(this) }
    private val cameraRepository by lazy {
        SpeedCameraRepository(
            remoteDataSource = PublicDataCameraApiClient(),
            localDataSource = SpeedCameraLocalDataSource(SpeedCameraDatabaseHelper(this))
        )
    }
    private val driveRecordDataSource by lazy {
        DriveRecordLocalDataSource(DriveRecordDatabaseHelper(this))
    }
    private val cameraPassDetector = CameraPassDetector()
    private val drivingBearingStabilizer = DrivingBearingStabilizer()
    private val startDetector = DrivingStartDetector(state.config)
    private val stopDetector = DrivingStopDetector(state.config)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopDetection()
            else -> startLowPowerMonitoring()
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        locationJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startLowPowerMonitoring() {
        if (state.status == DrivingDetectionStatus.MONITORING_LOW_POWER) return

        locationJob?.cancel()
        startDetector.reset()
        stopDetector.reset()
        state = state.copy(
            status = DrivingDetectionStatus.MONITORING_LOW_POWER,
            lastStatusChangedAtMillis = System.currentTimeMillis(),
            errorMessage = null
        )
        startForegroundCompat(buildNotification("자동 주행 감지 대기 중", state.status))
        locationJob = serviceScope.launch {
            locationSpeedTracker.speedSamples(highAccuracy = false)
                .catch { throwable ->
                    Log.e(TAG, "Low power monitoring failed: ${throwable.message}", throwable)
                    setErrorState(throwable.message ?: "위치 감시 실패")
                }
                .collect { sample ->
                    if (startDetector.addSample(sample)) {
                        startHighAccuracyRecording(sample)
                    }
                }
        }
        Log.d(TAG, "Driving detection service started: ${state.status}")
    }

    private fun stopDetection() {
        locationJob?.cancel()
        locationJob = null
        currentSession?.let { session ->
            serviceScope.launch(Dispatchers.IO) {
                driveRecordDataSource.finishSession(session.id, System.currentTimeMillis())
            }
        }
        currentSession = null
        cameraPassDetector.reset()
        drivingBearingStabilizer.reset()
        startDetector.reset()
        stopDetector.reset()
        state = state.copy(
            status = DrivingDetectionStatus.OFF,
            activeSessionId = null,
            lastStatusChangedAtMillis = System.currentTimeMillis(),
            errorMessage = null
        )
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Log.d(TAG, "Driving detection service stopped.")
    }

    private fun startHighAccuracyRecording(firstSample: LocationSpeedSample) {
        locationJob?.cancel()
        val session = DriveSession(
            id = UUID.randomUUID().toString(),
            startedAtMillis = System.currentTimeMillis(),
            source = DriveSessionSource.AUTO
        )
        currentSession = session
        cameraPassDetector.reset()
        drivingBearingStabilizer.reset()
        stopDetector.reset()
        state = state.copy(
            status = DrivingDetectionStatus.RECORDING_HIGH_ACCURACY,
            activeSessionId = session.id,
            lastStatusChangedAtMillis = System.currentTimeMillis(),
            errorMessage = null
        )
        startForegroundCompat(buildNotification("주행 기록 중", state.status))

        serviceScope.launch(Dispatchers.IO) {
            driveRecordDataSource.startSession(session)
        }
        locationJob = serviceScope.launch {
            handleRecordingSample(drivingBearingStabilizer.stabilize(firstSample))
            locationSpeedTracker.speedSamples(highAccuracy = true)
                .catch { throwable ->
                    Log.e(TAG, "High accuracy recording failed: ${throwable.message}", throwable)
                    setErrorState(throwable.message ?: "주행 기록 실패")
                }
                .collect { sample ->
                    val stabilizedSample = drivingBearingStabilizer.stabilize(sample)
                    handleRecordingSample(stabilizedSample)
                    if (stopDetector.addSample(sample)) {
                        finishCurrentSessionAndReturnToMonitoring()
                    } else if (sample.speedKmh < state.config.stopSpeedThresholdKmh) {
                        updateStoppingPending()
                    } else if (state.status == DrivingDetectionStatus.STOPPING_PENDING) {
                        updateRecording()
                    }
                }
        }
        Log.d(TAG, "Driving detected. High accuracy recording started: ${session.id}")
    }

    private suspend fun handleRecordingSample(sample: LocationSpeedSample) {
        val session = currentSession ?: return
        val forwardCameras = withContext(Dispatchers.IO) {
            cameraRepository.findNearbyCachedCameras(
                currentLocation = sample.location,
                radiusMeters = CAMERA_SEARCH_RADIUS_METERS,
                maxResults = MAX_NEARBY_CAMERA_CANDIDATES
            ).filterForwardCorridor(sample)
                .take(MAX_FORWARD_CAMERA_RESULTS)
        }
        val passRecords = cameraPassDetector.detectPasses(
            sessionId = session.id,
            sample = sample,
            nearbyCameras = forwardCameras
        )
        if (passRecords.isEmpty()) return

        withContext(Dispatchers.IO) {
            passRecords.forEach { record ->
                driveRecordDataSource.insertPassRecord(record)
                Log.d(TAG, record.toLogText())
            }
        }
    }

    private fun finishCurrentSessionAndReturnToMonitoring() {
        val session = currentSession ?: return startLowPowerMonitoring()
        serviceScope.launch(Dispatchers.IO) {
            driveRecordDataSource.finishSession(session.id, System.currentTimeMillis())
        }
        currentSession = null
        cameraPassDetector.reset()
        drivingBearingStabilizer.reset()
        Log.d(TAG, "Driving stopped. Session finished: ${session.id}")
        startLowPowerMonitoring()
    }

    private fun updateStoppingPending() {
        if (state.status == DrivingDetectionStatus.STOPPING_PENDING) return
        state = state.copy(
            status = DrivingDetectionStatus.STOPPING_PENDING,
            lastStatusChangedAtMillis = System.currentTimeMillis()
        )
        startForegroundCompat(buildNotification("정지 감지 중", state.status))
    }

    private fun updateRecording() {
        state = state.copy(
            status = DrivingDetectionStatus.RECORDING_HIGH_ACCURACY,
            lastStatusChangedAtMillis = System.currentTimeMillis()
        )
        startForegroundCompat(buildNotification("주행 기록 중", state.status))
    }

    private fun setErrorState(message: String) {
        state = state.copy(
            status = DrivingDetectionStatus.ERROR,
            errorMessage = message,
            lastStatusChangedAtMillis = System.currentTimeMillis()
        )
        startForegroundCompat(buildNotification("오류: $message", state.status))
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(
        contentText: String,
        status: DrivingDetectionStatus
    ): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        return builder
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("찍혔나? 자동 주행 감지")
            .setContentText(contentText)
            .setOngoing(true)
            .setShowWhen(false)
            .setContentIntent(openAppPendingIntent())
            .addAction(buildStopAction())
            .setSubText(status.toNotificationSubText())
            .build()
    }

    @Suppress("DEPRECATION")
    private fun buildStopAction(): Notification.Action =
        Notification.Action.Builder(
            R.mipmap.ic_launcher,
            "중지",
            stopPendingIntent()
        ).build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "자동 주행 감지",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "백그라운드에서 주행 시작과 종료를 감지합니다."
        }
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_START = "com.joon.chalkak.action.START_DRIVING_DETECTION"
        const val ACTION_STOP = "com.joon.chalkak.action.STOP_DRIVING_DETECTION"

        private const val TAG = "DrivingDetection"
        private const val CHANNEL_ID = "driving_detection"
        private const val NOTIFICATION_ID = 1001
        private const val CAMERA_SEARCH_RADIUS_METERS = 1_000.0
        private const val MAX_NEARBY_CAMERA_CANDIDATES = 50
        private const val MAX_FORWARD_CAMERA_RESULTS = 10
    }
}

private fun DrivingDetectionStatus.toNotificationSubText(): String =
    when (this) {
        DrivingDetectionStatus.OFF -> "꺼짐"
        DrivingDetectionStatus.MONITORING_LOW_POWER -> "대기"
        DrivingDetectionStatus.DRIVING_DETECTED -> "감지"
        DrivingDetectionStatus.RECORDING_HIGH_ACCURACY -> "기록"
        DrivingDetectionStatus.STOPPING_PENDING -> "정지 확인"
        DrivingDetectionStatus.ERROR -> "오류"
    }

private fun CameraPassRecord.toLogText(): String =
    "Camera pass recorded: camera=${camera.id}, speed=$measuredSpeedKmh, " +
        "limit=${camera.speedLimitKmh}, result=${judgement.result}, risk=${judgement.result == SpeedJudgementResult.ENFORCEMENT_RISK}"

private fun DrivingDetectionService.openAppPendingIntent(): PendingIntent {
    val intent = Intent(this, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    return PendingIntent.getActivity(
        this,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

private fun DrivingDetectionService.stopPendingIntent(): PendingIntent {
    val intent = Intent(this, DrivingDetectionService::class.java).apply {
        action = DrivingDetectionService.ACTION_STOP
    }
    return PendingIntent.getService(
        this,
        1,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
