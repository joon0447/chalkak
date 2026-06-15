package com.joon.chalkak.presentation.main

import com.joon.chalkak.domain.DrivingStatus
import com.joon.chalkak.model.DriveRecord
import com.joon.chalkak.model.DriveRecordGroup
import com.joon.chalkak.model.HistorySummary
import com.joon.chalkak.model.NearbyCamera
import com.joon.chalkak.model.RecentRecord

data class MainUiState(
    val selectedTab: MainTab = MainTab.HOME,
    val currentSpeedKmh: Int = 58,
    val drivingStatus: DrivingStatus = DrivingStatus.SAFE,
    val nearbyCamera: NearbyCamera = NearbyCamera(
        distanceText = "전방 320m",
        title = "단속 카메라",
        subtitle = "제한속도 60km/h · 고정식"
    ),
    val recentRecords: List<RecentRecord> = listOf(
        RecentRecord("올림픽대로 · 58 km/h", "오늘 14:32 · 제한 60km/h"),
        RecentRecord("강변북로 · 79 km/h", "오늘 13:15 · 제한 80km/h"),
        RecentRecord("경부고속도로 · 107 km/h", "어제 18:47 · 제한 110km/h")
    ),
    val historySummary: HistorySummary = HistorySummary(
        totalDriveCount = "23회",
        safePassRate = "89%",
        warningCount = "3건"
    ),
    val driveRecordGroups: List<DriveRecordGroup> = listOf(
        DriveRecordGroup(
            date = "6월 15일 일요일",
            records = listOf(
                DriveRecord(
                    time = "14:32 ~ 15:18",
                    route = "강남대로  >  테헤란로",
                    cameraCount = "카메라 4개",
                    safeCount = "안전 4",
                    status = DrivingStatus.SAFE
                ),
                DriveRecord(
                    time = "08:05 ~ 08:47",
                    route = "올림픽대로  >  강변북로",
                    cameraCount = "카메라 6개",
                    safeCount = "안전 5",
                    warningText = "주의 1",
                    status = DrivingStatus.WARNING
                )
            )
        ),
        DriveRecordGroup(
            date = "6월 14일 토요일",
            records = listOf(
                DriveRecord(
                    time = "10:20 ~ 11:05",
                    route = "서초대로  >  반포대로",
                    cameraCount = "카메라 3개",
                    safeCount = "안전 3",
                    status = DrivingStatus.SAFE
                )
            )
        ),
        DriveRecordGroup(
            date = "6월 13일 금요일",
            records = listOf(
                DriveRecord(
                    time = "18:30 ~ 19:12",
                    route = "양재대로  >  헌릉로",
                    cameraCount = "카메라 5개",
                    safeCount = "안전 4",
                    warningText = "불확실 1",
                    status = DrivingStatus.UNKNOWN
                )
            )
        )
    )
)
