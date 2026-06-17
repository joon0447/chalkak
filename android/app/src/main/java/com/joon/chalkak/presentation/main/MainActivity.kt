package com.joon.chalkak.presentation.main

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.lifecycle.lifecycleScope
import com.joon.chalkak.BuildConfig
import com.joon.chalkak.data.camera.local.SpeedCameraDatabaseHelper
import com.joon.chalkak.data.camera.local.SpeedCameraLocalDataSource
import com.joon.chalkak.data.camera.remote.PublicDataCameraApiClient
import com.joon.chalkak.data.camera.repository.SpeedCameraRepository
import com.joon.chalkak.data.driving.DrivingDetectionService
import com.joon.chalkak.data.drive.local.DriveRecordDatabaseHelper
import com.joon.chalkak.data.drive.local.DriveRecordLocalDataSource
import com.joon.chalkak.data.location.AndroidLocationSpeedTracker
import com.joon.chalkak.domain.CameraPassRecord
import com.joon.chalkak.domain.DriveSession
import com.joon.chalkak.domain.DrivingStatus
import com.joon.chalkak.domain.LocationSpeedSample
import com.joon.chalkak.domain.NearbySpeedCamera
import com.joon.chalkak.domain.SpeedJudgementResult
import com.joon.chalkak.domain.driving.CameraPassDetector
import com.joon.chalkak.domain.filterForwardCorridor
import com.joon.chalkak.model.NearbyCamera
import com.joon.chalkak.ui.theme.ChalkakTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
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
    private var speedTrackingJob: Job? = null
    private var currentSession: DriveSession? = null
    private var startTrackingAfterPermissionRequest: Boolean = false
    private var startAutoDetectionAfterPermissionRequest: Boolean = false

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        updateLocationPermissionUi()
        if (granted) {
            if (startTrackingAfterPermissionRequest) {
                startSpeedTracking()
            }
            if (startAutoDetectionAfterPermissionRequest) {
                if (hasNotificationPermission()) {
                    startDrivingDetectionService()
                } else {
                    Log.w(SPEED_TAG, "Notification permission denied.")
                    viewModel.updateAutoDrivingDetectionEnabled(false)
                }
            }
        } else {
            Log.w(SPEED_TAG, "Location permission denied.")
            viewModel.stopSpeedTracking()
            viewModel.updateAutoDrivingDetectionEnabled(false)
        }
        startTrackingAfterPermissionRequest = false
        startAutoDetectionAfterPermissionRequest = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        logCameraApiSmokeCheck()
        loadDriveRecords()
        refreshCameraCacheIfNeeded()
        updateLocationPermissionUi()
        setContent {
            ChalkakTheme {
                MainScreen(
                    uiState = viewModel.uiState,
                    onDrivingActionClick = ::toggleSpeedTracking,
                    onLocationPermissionClick = ::requestLocationPermissionFromSettings,
                    onCameraDataUpdateClick = ::refreshCameraCacheManually,
                    onGpsAccuracyClick = viewModel::toggleGpsAccuracyMode,
                    onAutoDrivingDetectionClick = ::toggleAutoDrivingDetection,
                    onClearRecordsClick = ::clearDriveRecords
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadDriveRecords()
    }

    private fun toggleSpeedTracking() {
        if (viewModel.uiState.isSpeedTracking) {
            stopSpeedTracking()
            return
        }

        if (hasLocationPermission()) {
            startSpeedTracking()
        } else {
            startTrackingAfterPermissionRequest = true
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun startSpeedTracking() {
        if (speedTrackingJob?.isActive == true) return

        val session = DriveSession(
            id = UUID.randomUUID().toString(),
            startedAtMillis = System.currentTimeMillis()
        )
        currentSession = session
        cameraPassDetector.reset()
        lifecycleScope.launch(Dispatchers.IO) {
            driveRecordDataSource.startSession(session)
            loadDriveRecordsOnMain()
        }

        viewModel.setSpeedTracking(true)
        speedTrackingJob = lifecycleScope.launch {
            locationSpeedTracker.speedSamples(highAccuracy = viewModel.uiState.gpsAccuracySubtitle == "높은 정확도 모드")
                .catch { throwable ->
                    Log.e(SPEED_TAG, "Speed tracking failed: ${throwable.message}", throwable)
                    viewModel.stopSpeedTracking()
                }
                .collect { sample ->
                    Log.d(
                        SPEED_TAG,
                        "Speed sample: ${sample.roundedSpeedKmh}km/h, " +
                            "accuracy=${sample.accuracyMeters}, " +
                            "lat=${sample.location.latitude}, lng=${sample.location.longitude}"
                    )
                    viewModel.updateSpeed(sample)
                    handleSpeedSample(sample)
                }
        }
    }

    private fun stopSpeedTracking() {
        currentSession?.let { session ->
            lifecycleScope.launch(Dispatchers.IO) {
                driveRecordDataSource.finishSession(
                    sessionId = session.id,
                    endedAtMillis = System.currentTimeMillis()
                )
                loadDriveRecordsOnMain()
            }
        }
        currentSession = null
        cameraPassDetector.reset()
        speedTrackingJob?.cancel()
        speedTrackingJob = null
        viewModel.stopSpeedTracking()
        Log.d(SPEED_TAG, "Speed tracking stopped.")
    }

    private suspend fun handleSpeedSample(sample: LocationSpeedSample) {
        val forwardCameras = withContext(Dispatchers.IO) {
            cameraRepository.findNearbyCachedCameras(
                currentLocation = sample.location,
                radiusMeters = CAMERA_SEARCH_RADIUS_METERS,
                maxResults = MAX_NEARBY_CAMERA_CANDIDATES
            ).filterForwardCorridor(sample)
                .take(MAX_FORWARD_CAMERA_RESULTS)
        }

        viewModel.updateNearbyCamera(forwardCameras.firstOrNull()?.toUiModel())

        val session = currentSession ?: return
        val passRecords = cameraPassDetector.detectPasses(
            sessionId = session.id,
            sample = sample,
            nearbyCameras = forwardCameras
        )
        if (passRecords.isEmpty()) return

        withContext(Dispatchers.IO) {
            passRecords.forEach { record ->
                driveRecordDataSource.insertPassRecord(record)
                Log.d(
                    SPEED_TAG,
                    "Camera pass recorded: camera=${record.camera.id}, " +
                        "speed=${record.measuredSpeedKmh}, " +
                        "limit=${record.camera.speedLimitKmh}, " +
                        "result=${record.judgement.result}"
                )
            }
            loadDriveRecordsOnMain()
        }
        viewModel.updateDrivingStatus(passRecords.toDrivingStatus())
    }

    private fun hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineGranted || coarseGranted
    }

    private fun requestLocationPermissionFromSettings() {
        if (hasLocationPermission()) {
            updateLocationPermissionUi()
            return
        }

        startTrackingAfterPermissionRequest = false
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun toggleAutoDrivingDetection() {
        if (viewModel.uiState.isAutoDrivingDetectionEnabled) {
            stopDrivingDetectionService()
            return
        }

        if (hasLocationPermission() && hasNotificationPermission()) {
            startDrivingDetectionService()
        } else {
            startAutoDetectionAfterPermissionRequest = true
            locationPermissionLauncher.launch(requiredAutoDetectionPermissions())
        }
    }

    private fun startDrivingDetectionService() {
        val intent = Intent(this, DrivingDetectionService::class.java).apply {
            action = DrivingDetectionService.ACTION_START
        }
        ContextCompat.startForegroundService(this, intent)
        viewModel.updateAutoDrivingDetectionEnabled(true)
    }

    private fun stopDrivingDetectionService() {
        val intent = Intent(this, DrivingDetectionService::class.java).apply {
            action = DrivingDetectionService.ACTION_STOP
        }
        startService(intent)
        viewModel.updateAutoDrivingDetectionEnabled(false)
    }

    private fun hasNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    private fun requiredAutoDetectionPermissions(): Array<String> =
        buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()

    private fun updateLocationPermissionUi() {
        viewModel.updateLocationPermissionSubtitle(
            if (hasLocationPermission()) "허용됨" else "설정 필요"
        )
    }

    private fun loadDriveRecords() {
        lifecycleScope.launch(Dispatchers.IO) {
            pruneOldDriveRecords()
            loadDriveRecordsOnMain()
        }
    }

    private suspend fun loadDriveRecordsOnMain() {
        val summary = driveRecordDataSource.getHistorySummary()
        val recentRecords = driveRecordDataSource.getRecentRecords()
        val groups = driveRecordDataSource.getDriveRecordGroups()
        withContext(Dispatchers.Main) {
            viewModel.updateDriveRecords(
                historySummary = summary,
                recentRecords = recentRecords,
                driveRecordGroups = groups
            )
        }
    }

    private fun refreshCameraCacheIfNeeded() {
        lifecycleScope.launch(Dispatchers.IO) {
            cameraRepository.getLastSyncedAtMillis()?.let { lastSyncedAtMillis ->
                withContext(Dispatchers.Main) {
                    viewModel.updateCameraDataSubtitle(
                        "최근 업데이트: ${lastSyncedAtMillis.toSettingsTimeText()}"
                    )
                }
                return@launch
            }

            runCatching {
                Log.d(TAG, "Camera cache refresh started.")
                val result = cameraRepository.refreshCameras(maxPages = INITIAL_CAMERA_CACHE_MAX_PAGES)
                withContext(Dispatchers.Main) {
                    viewModel.updateCameraDataSubtitle(
                        "최근 업데이트: ${(result.lastSyncedAtMillis ?: System.currentTimeMillis()).toSettingsTimeText()} · ${result.savedCount}건"
                    )
                }
                Log.d(
                    TAG,
                    "Camera cache refresh success: saved=${result.savedCount}, " +
                        "syncedAt=${result.lastSyncedAtMillis}"
                )
            }.onFailure { throwable ->
                Log.e(TAG, "Camera cache refresh failed: ${throwable.message}", throwable)
            }
        }
    }

    private fun refreshCameraCacheManually() {
        viewModel.updateCameraDataSubtitle("업데이트 중...")
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching {
                val result = cameraRepository.refreshCameras(maxPages = MANUAL_CAMERA_CACHE_MAX_PAGES)
                withContext(Dispatchers.Main) {
                    viewModel.updateCameraDataSubtitle(
                        "최근 업데이트: ${System.currentTimeMillis().toSettingsTimeText()} · ${result.savedCount}건"
                    )
                }
            }.onFailure { throwable ->
                Log.e(TAG, "Manual camera cache refresh failed: ${throwable.message}", throwable)
                withContext(Dispatchers.Main) {
                    viewModel.updateCameraDataSubtitle("업데이트 실패")
                }
            }
        }
    }

    private fun clearDriveRecords() {
        lifecycleScope.launch(Dispatchers.IO) {
            driveRecordDataSource.clear()
            loadDriveRecordsOnMain()
        }
    }

    private fun pruneOldDriveRecords() {
        val cutoffMillis = System.currentTimeMillis() - RECORD_RETENTION_DAYS * MILLIS_PER_DAY
        driveRecordDataSource.deleteRecordsOlderThan(cutoffMillis)
    }

    private fun Long.toSettingsTimeText(): String =
        SETTINGS_TIME_FORMATTER.format(Instant.ofEpochMilli(this).atZone(SEOUL_ZONE))

    private fun NearbySpeedCamera.toUiModel(): NearbyCamera {
        val speedLimitText = camera.speedLimitKmh?.let { "제한속도 ${it}km/h" } ?: "제한속도 미확인"
        val roadText = camera.roadName ?: camera.location
        return NearbyCamera(
            distanceText = "전방 $distanceText",
            title = "단속 카메라",
            subtitle = "$speedLimitText · $roadText"
        )
    }

    private fun List<CameraPassRecord>.toDrivingStatus(): DrivingStatus =
        when {
            any { it.judgement.result == SpeedJudgementResult.ENFORCEMENT_RISK } -> DrivingStatus.WARNING
            any { it.judgement.result == SpeedJudgementResult.WARNING } -> DrivingStatus.WARNING
            any { it.judgement.result == SpeedJudgementResult.SAFE } -> DrivingStatus.SAFE
            else -> DrivingStatus.UNKNOWN
        }

    private fun logCameraApiSmokeCheck() {
        if (!BuildConfig.DEBUG) return

        lifecycleScope.launch(Dispatchers.IO) {
            runCatching {
                Log.d(TAG, "Smoke check started.")
                val cameras = PublicDataCameraApiClient().fetchCameras(
                    pageNo = 1,
                    numOfRows = 5
                )
                Log.d(
                    TAG,
                    "Smoke check success: count=${cameras.size}, first=${cameras.firstOrNull()?.id}"
                )
            }.onFailure { throwable ->
                Log.e(TAG, "Smoke check failed: ${throwable.message}", throwable)
            }
        }
    }

    private companion object {
        const val TAG = "CameraApi"
        const val SPEED_TAG = "SpeedTracker"
        const val CAMERA_SEARCH_RADIUS_METERS = 1_000.0
        const val MAX_NEARBY_CAMERA_CANDIDATES = 50
        const val MAX_FORWARD_CAMERA_RESULTS = 10
        const val INITIAL_CAMERA_CACHE_MAX_PAGES = 5
        const val MANUAL_CAMERA_CACHE_MAX_PAGES = 10
        const val RECORD_RETENTION_DAYS = 90L
        const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
        val SEOUL_ZONE: ZoneId = ZoneId.of("Asia/Seoul")
        val SETTINGS_TIME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("M월 d일 HH:mm", Locale.KOREAN)
    }
}
