package com.joon.chalkak.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.joon.chalkak.BuildConfig
import com.joon.chalkak.presentation.common.AccentBlue
import com.joon.chalkak.presentation.common.ArchiveIcon
import com.joon.chalkak.presentation.common.CameraIcon
import com.joon.chalkak.presentation.common.DocumentIcon
import com.joon.chalkak.presentation.common.GpsIcon
import com.joon.chalkak.presentation.common.InfoIcon
import com.joon.chalkak.presentation.common.PinIcon
import com.joon.chalkak.presentation.common.SectionLabel
import com.joon.chalkak.presentation.common.SettingsDivider
import com.joon.chalkak.presentation.common.SettingsGroup
import com.joon.chalkak.presentation.common.SettingsRow
import com.joon.chalkak.presentation.common.ShieldIcon
import com.joon.chalkak.presentation.common.TextMuted
import com.joon.chalkak.presentation.common.TextPrimary
import com.joon.chalkak.presentation.common.TextSecondary
import com.joon.chalkak.presentation.common.TrashIcon
import com.joon.chalkak.presentation.common.WarningAmber
import com.joon.chalkak.presentation.main.MainUiState

@Composable
fun SettingsScreen(
    uiState: MainUiState,
    onLocationPermissionClick: () -> Unit,
    onCameraDataUpdateClick: () -> Unit,
    onGpsAccuracyClick: () -> Unit,
    onClearRecordsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Text(
            text = "설정",
            color = TextPrimary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(22.dp))

        SectionLabel("위치 및 데이터")
        SettingsGroup {
            SettingsRow(
                PinIcon,
                "위치 권한",
                uiState.locationPermissionSubtitle,
                Color(0xFF123E8C),
                AccentBlue,
                onClick = onLocationPermissionClick
            )
            SettingsDivider()
            SettingsRow(
                CameraIcon,
                "카메라 데이터",
                uiState.cameraDataSubtitle,
                Color(0xFF0D4422),
                com.joon.chalkak.presentation.common.SafeGreen,
                onClick = onCameraDataUpdateClick
            )
            SettingsDivider()
            SettingsRow(
                GpsIcon,
                "GPS 정확도 설정",
                uiState.gpsAccuracySubtitle,
                Color(0xFF4B3605),
                WarningAmber,
                onClick = onGpsAccuracyClick
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        SectionLabel("기록 보관")
        SettingsGroup {
            SettingsRow(
                ArchiveIcon,
                "기록 보관 기간",
                uiState.recordRetentionSubtitle,
                Color(0xFF252C35),
                TextSecondary
            )
            SettingsDivider()
            SettingsRow(
                icon = TrashIcon,
                title = "모든 기록 삭제",
                subtitle = "로컬에 저장된 주행 기록을 모두 삭제합니다",
                iconBackground = Color(0xFF4A1417),
                iconColor = Color(0xFFFF4A55),
                titleColor = Color(0xFFFF4A55),
                onClick = onClearRecordsClick
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        SectionLabel("정보")
        SettingsGroup {
            SettingsRow(ShieldIcon, "개인정보 처리방침", "", Color(0xFF123E8C), AccentBlue)
            SettingsDivider()
            SettingsRow(DocumentIcon, "이용약관", "", Color(0xFF252C35), TextSecondary)
            SettingsDivider()
            SettingsRow(InfoIcon, "앱 버전", BuildConfig.VERSION_NAME, Color(0xFF252C35), TextSecondary)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "찍혔나?는 참고용 주행 속도 로그이며,\n단속 여부를 보장하지 않습니다.",
            color = TextMuted,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "© SnapCheck",
            color = TextMuted,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
