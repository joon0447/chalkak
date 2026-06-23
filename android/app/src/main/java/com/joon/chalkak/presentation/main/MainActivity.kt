package com.joon.chalkak.presentation.main

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.SystemBarStyle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.lifecycle.lifecycleScope
import com.joon.chalkak.data.camera.local.SpeedCameraDatabaseHelper
import com.joon.chalkak.data.camera.local.SpeedCameraLocalDataSource
import com.joon.chalkak.data.camera.remote.PublicDataCameraApiClient
import com.joon.chalkak.data.camera.repository.SpeedCameraRepository
import com.joon.chalkak.data.driving.DrivingDetectionService
import com.joon.chalkak.data.drive.local.DriveRecordDatabaseHelper
import com.joon.chalkak.data.drive.local.DriveRecordLocalDataSource
import com.joon.chalkak.data.location.AndroidLocationSpeedTracker
import com.joon.chalkak.data.settings.DrivingRegion
import com.joon.chalkak.data.settings.DrivingRegionPreferences
import com.joon.chalkak.domain.CameraPassRecord
import com.joon.chalkak.domain.DriveSession
import com.joon.chalkak.domain.DrivingStatus
import com.joon.chalkak.domain.GeoLocation
import com.joon.chalkak.domain.LocationSpeedSample
import com.joon.chalkak.domain.NearbySpeedCamera
import com.joon.chalkak.domain.SpeedJudgementResult
import com.joon.chalkak.domain.driving.CameraPassDetector
import com.joon.chalkak.domain.driving.DrivingBearingStabilizer
import com.joon.chalkak.domain.filterForwardCorridor
import com.joon.chalkak.model.NearbyCamera
import com.joon.chalkak.presentation.onboarding.DrivingRegionOnboardingScreen
import com.joon.chalkak.presentation.onboarding.DrivingRegionOnboardingState
import com.joon.chalkak.presentation.onboarding.DrivingProvinceNames
import com.joon.chalkak.ui.theme.ChalkakTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val drivingRegionPreferences by lazy { DrivingRegionPreferences(this) }
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
    private var speedTrackingJob: Job? = null
    private var cameraRefreshJob: Job? = null
    private var currentSession: DriveSession? = null
    private var startTrackingAfterPermissionRequest: Boolean = false
    private var startAutoDetectionAfterPermissionRequest: Boolean = false
    private var showOnboarding by mutableStateOf(false)
    private var onboardingState by mutableStateOf(DrivingRegionOnboardingState())

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
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        val primaryRegions = drivingRegionPreferences.getPrimaryRegions()
        showOnboarding = primaryRegions.isEmpty()
        updatePrimaryRegionUi(primaryRegions)
        loadDriveRecords()
        updateCameraCacheUi()
        updateLocationPermissionUi()
        setContent {
            ChalkakTheme {
                if (showOnboarding) {
                    DrivingRegionOnboardingScreen(
                        state = onboardingState,
                        onProvinceToggle = ::toggleOnboardingProvince,
                        onAllProvinceToggle = ::toggleAllOnboardingProvinces,
                        onSubmit = { downloadSelectedRegions(enterAppAfterDownload = true) }
                    )
                } else {
                    MainScreen(
                        uiState = viewModel.uiState,
                        onDrivingActionClick = ::toggleSpeedTracking,
                        onLocationPermissionClick = ::requestLocationPermissionFromSettings,
                        regionSelectionState = onboardingState,
                        onPrepareRegionSelection = ::prepareRegionSelection,
                        onRegionProvinceToggle = ::toggleOnboardingProvince,
                        onAllRegionProvinceToggle = ::toggleAllOnboardingProvinces,
                        onRegionSelectionSubmit = { onComplete ->
                            downloadSelectedRegions(
                                enterAppAfterDownload = false,
                                onComplete = onComplete
                            )
                        },
                        onAutoDrivingDetectionClick = ::toggleAutoDrivingDetection,
                        onClearRecordsClick = ::clearDriveRecords
                    )
                }
            }
        }
        if (primaryRegions.isNotEmpty()) {
            refreshPrimaryRegionsIfNeeded(primaryRegions)
            prefetchCurrentRegionIfPossible()
        }
    }

    override fun onResume() {
        super.onResume()
        loadDriveRecords()
        updateCameraCacheUi()
    }

    private fun toggleOnboardingProvince(province: String) {
        if (onboardingState.isDownloading) return

        val selected = onboardingState.selectedProvinces
        val nextSelected = when {
            province in selected -> selected - province
            else -> selected + province
        }
        onboardingState = onboardingState.copy(
            selectedProvinces = nextSelected,
            errorText = null
        )
    }

    private fun toggleAllOnboardingProvinces() {
        if (onboardingState.isDownloading) return

        val allSelected = onboardingState.selectedProvinces.containsAll(DrivingProvinceNames)
        onboardingState = onboardingState.copy(
            selectedProvinces = if (allSelected) emptyList() else DrivingProvinceNames,
            errorText = null
        )
    }

    private fun prepareRegionSelection() {
        onboardingState = DrivingRegionOnboardingState(
            selectedProvinces = drivingRegionPreferences.getPrimaryRegions()
                .map { it.provinceName }
                .filter { it in DrivingProvinceNames }
        )
    }

    private fun downloadSelectedRegions(
        enterAppAfterDownload: Boolean,
        onComplete: () -> Unit = {}
    ) {
        if (!onboardingState.canSubmit) return

        val regions = onboardingState.selectedProvinces.map { province ->
            DrivingRegion(provinceName = province)
        }
        onboardingState = onboardingState.copy(
            isDownloading = true,
            progressText = "선택 지역 데이터 준비 중...",
            errorText = null
        )
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching {
                var totalSavedCount = 0
                var latestReferenceDate: String? = null
                regions.forEachIndexed { index, region ->
                    val result = cameraRepository.refreshRegionCameras(
                        provinceName = region.provinceName,
                        onProgress = { loadedCount, totalCount ->
                            withContext(Dispatchers.Main) {
                                onboardingState = onboardingState.copy(
                                    progressText = totalCount?.let { total ->
                                        "${index + 1}/${regions.size} ${region.provinceName} ${loadedCount}/${total}건"
                                    } ?: "${index + 1}/${regions.size} ${region.provinceName} ${loadedCount}건"
                                )
                            }
                        }
                    )
                    totalSavedCount += result.savedCount
                    latestReferenceDate = listOfNotNull(latestReferenceDate, result.referenceDate).maxOrNull()
                }
                CameraRegionDownloadSummary(
                    savedCount = totalSavedCount,
                    referenceDate = latestReferenceDate
                )
            }.onSuccess { result ->
                drivingRegionPreferences.savePrimaryRegions(regions)
                withContext(Dispatchers.Main) {
                    updatePrimaryRegionUi(regions)
                    updateCameraCacheUi(
                        "지역 데이터 준비 완료 · ${result.savedCount}건" +
                            (result.referenceDate?.let { " · 기준일 $it" } ?: "")
                    )
                    if (enterAppAfterDownload) {
                        showOnboarding = false
                    }
                    onboardingState = onboardingState.copy(isDownloading = false)
                    onComplete()
                }
                prefetchCurrentRegionIfPossible()
            }.onFailure { throwable ->
                if (throwable is CancellationException) throw throwable
                Log.e(TAG, "Primary region refresh failed: ${throwable.message}", throwable)
                withContext(Dispatchers.Main) {
                    onboardingState = onboardingState.copy(
                        isDownloading = false,
                        progressText = "",
                        errorText = "데이터를 불러오지 못했습니다. 지역명과 네트워크를 확인해주세요."
                    )
                }
            }
        }
    }

    private fun refreshPrimaryRegionsIfNeeded(regions: List<DrivingRegion>) {
        lifecycleScope.launch(Dispatchers.IO) {
            regions.forEach { region ->
                val metadata = cameraRepository.getRegionSyncMetadata(region.provinceName)
                if (metadata != null && !metadata.syncedAtMillis.isStale()) return@forEach

                runCatching {
                    cameraRepository.refreshRegionCameras(provinceName = region.provinceName)
                }.onFailure { throwable ->
                    if (throwable is CancellationException) throw throwable
                    Log.e(TAG, "Silent primary region refresh failed: ${throwable.message}", throwable)
                }
            }
            withContext(Dispatchers.Main) {
                updateCameraCacheUi()
            }
        }
    }

    private fun prefetchCurrentRegionIfPossible() {
        if (!hasLocationPermission()) return

        lifecycleScope.launch {
            runCatching {
                val sample = locationSpeedTracker.speedSamples(highAccuracy = false).first()
                withContext(Dispatchers.IO) {
                    sample.location.toDrivingRegionOrNull()
                }
            }.onSuccess { region ->
                if (region == null) return@onSuccess
                val hasProvinceCache = cameraRepository.hasRegionCache(region.provinceName)
                val hasCityCache = region.cityName?.let { city ->
                    cameraRepository.hasRegionCache(region.provinceName, city)
                } ?: false
                if (hasProvinceCache || hasCityCache) return@onSuccess

                launch(Dispatchers.IO) {
                    runCatching {
                        cameraRepository.refreshRegionCameras(
                            provinceName = region.provinceName,
                            cityName = region.cityName
                        )
                    }.onSuccess {
                        withContext(Dispatchers.Main) {
                            updateCameraCacheUi()
                        }
                    }.onFailure { throwable ->
                        if (throwable is CancellationException) throw throwable
                        Log.e(TAG, "Current region prefetch failed: ${throwable.message}", throwable)
                    }
                }
            }.onFailure { throwable ->
                if (throwable is CancellationException) throw throwable
                Log.e(TAG, "Current region lookup failed: ${throwable.message}", throwable)
            }
        }
    }

    private fun updatePrimaryRegionUi(regions: List<DrivingRegion>) {
        viewModel.updatePrimaryDrivingRegionSubtitle(
            regions.takeIf { it.isNotEmpty() }
                ?.joinToString(", ") { it.provinceName }
                ?: "첫 실행에서 설정 필요"
        )
    }

    private fun updateCameraCacheUi(subtitleOverride: String? = null) {
        if (subtitleOverride != null) {
            viewModel.updateCameraDataSubtitle(subtitleOverride)
            return
        }

        val cachedCount = cameraRepository.getCachedCameraCount()
        val lastSyncedAtMillis = cameraRepository.getLastSyncedAtMillis()
        val subtitle = when {
            cachedCount <= 0 -> "지역 데이터 필요"
            lastSyncedAtMillis != null ->
                "로컬 ${cachedCount}건 · 전국 기준 ${lastSyncedAtMillis.toSettingsTimeText()}"
            else -> "로컬 ${cachedCount}건"
        }
        viewModel.updateCameraDataSubtitle(subtitle)
    }

    @Suppress("DEPRECATION")
    private fun GeoLocation.toDrivingRegionOrNull(): DrivingRegion? {
        val addresses = Geocoder(this@MainActivity, Locale.KOREAN).getFromLocation(
            latitude,
            longitude,
            1
        ).orEmpty()
        val address = addresses.firstOrNull() ?: return null
        val province = address.adminArea?.takeIf { it.isNotBlank() } ?: return null
        val city = listOf(
            address.subAdminArea,
            address.locality?.takeIf { it != province },
            address.subLocality
        ).firstOrNull { value ->
            value != null && value.isNotBlank() && value != province
        } ?: return null

        return DrivingRegion(
            provinceName = province.trim(),
            cityName = city.trim()
        )
    }

    private fun Long.isStale(): Boolean =
        System.currentTimeMillis() - this >= REGION_REFRESH_INTERVAL_MILLIS

    private data class CameraRegionDownloadSummary(
        val savedCount: Int,
        val referenceDate: String?
    )

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
        drivingBearingStabilizer.reset()
        lifecycleScope.launch(Dispatchers.IO) {
            driveRecordDataSource.startSession(session)
            loadDriveRecordsOnMain()
        }

        viewModel.setSpeedTracking(true)
        speedTrackingJob = lifecycleScope.launch {
            locationSpeedTracker.speedSamples(highAccuracy = true)
                .catch { throwable ->
                    Log.e(SPEED_TAG, "Speed tracking failed: ${throwable.message}", throwable)
                    viewModel.stopSpeedTracking()
                }
                .collect { sample ->
                    val stabilizedSample = drivingBearingStabilizer.stabilize(sample)
                    Log.d(
                        SPEED_TAG,
                        "Speed sample: ${stabilizedSample.roundedSpeedKmh}km/h, " +
                            "accuracy=${stabilizedSample.accuracyMeters}, " +
                            "bearing=${stabilizedSample.bearingDegrees}, " +
                            "lat=${stabilizedSample.location.latitude}, lng=${stabilizedSample.location.longitude}"
                    )
                    viewModel.updateSpeed(stabilizedSample)
                    handleSpeedSample(stabilizedSample)
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
        drivingBearingStabilizer.reset()
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
        if (cameraRefreshJob?.isActive == true) return

        cameraRefreshJob = lifecycleScope.launch(Dispatchers.IO) {
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
                val result = cameraRepository.refreshCameras(maxPages = NATIONWIDE_CAMERA_CACHE_MAX_PAGES)
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
        if (cameraRefreshJob?.isActive == true || viewModel.uiState.isCameraDataUpdating) return

        viewModel.startCameraDataUpdate()
        cameraRefreshJob = lifecycleScope.launch(Dispatchers.IO) {
            runCatching {
                val result = cameraRepository.refreshCameras(
                    maxPages = NATIONWIDE_CAMERA_CACHE_MAX_PAGES,
                    onProgress = { loadedCount ->
                        withContext(Dispatchers.Main) {
                            viewModel.updateCameraDataProgress(loadedCount)
                        }
                    }
                )
                withContext(Dispatchers.Main) {
                    viewModel.finishCameraDataUpdate(
                        "전국 데이터 완료 · ${result.savedCount}건" +
                            (result.referenceDate?.let { " · 기준일 $it" } ?: "")
                    )
                }
            }.onFailure { throwable ->
                Log.e(TAG, "Manual camera cache refresh failed: ${throwable.message}", throwable)
                withContext(Dispatchers.Main) {
                    viewModel.finishCameraDataUpdate("업데이트 실패")
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

    private companion object {
        const val TAG = "CameraApi"
        const val SPEED_TAG = "SpeedTracker"
        const val CAMERA_SEARCH_RADIUS_METERS = 1_000.0
        const val MAX_NEARBY_CAMERA_CANDIDATES = 50
        const val MAX_FORWARD_CAMERA_RESULTS = 10
        const val NATIONWIDE_CAMERA_CACHE_MAX_PAGES = 60
        const val RECORD_RETENTION_DAYS = 90L
        const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
        const val REGION_REFRESH_INTERVAL_MILLIS = 7L * MILLIS_PER_DAY
        val SEOUL_ZONE: ZoneId = ZoneId.of("Asia/Seoul")
        val SETTINGS_TIME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("M월 d일 HH:mm", Locale.KOREAN)
    }
}
