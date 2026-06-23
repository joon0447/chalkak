package com.joon.chalkak.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.joon.chalkak.BuildConfig
import com.joon.chalkak.R
import com.joon.chalkak.presentation.common.AccentBlue
import com.joon.chalkak.presentation.common.AppBackground
import com.joon.chalkak.presentation.common.SectionLabel
import com.joon.chalkak.presentation.common.SettingsDivider
import com.joon.chalkak.presentation.common.SettingsGroup
import com.joon.chalkak.presentation.common.SettingsRow
import com.joon.chalkak.presentation.common.TextMuted
import com.joon.chalkak.presentation.common.TextPrimary
import com.joon.chalkak.presentation.common.TextSecondary
import com.joon.chalkak.presentation.main.MainUiState

@Composable
fun SettingsScreen(
    uiState: MainUiState,
    onLocationPermissionClick: () -> Unit,
    onDrivingRegionClick: () -> Unit,
    onClearRecordsClick: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets(0),
        topBar = { SettingsTopBar() }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 14.dp, bottom = 24.dp)
        ) {
            SectionLabel("위치 및 데이터")
            SettingsGroup {
                SettingsRow(
                    painterResource(R.drawable.gps),
                    "위치 권한",
                    uiState.locationPermissionSubtitle,
                    Color(0xFF123E8C),
                    AccentBlue,
                    showIconBackground = false,
                    onClick = onLocationPermissionClick
                )
                SettingsDivider()
                SettingsRow(
                    painterResource(R.drawable.car),
                    "주 운전 지역",
                    uiState.primaryDrivingRegionSubtitle,
                    Color(0xFF252C35),
                    TextSecondary,
                    enabled = !uiState.isCameraDataUpdating,
                    showIconBackground = false,
                    onClick = onDrivingRegionClick
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            SectionLabel("기록")
            SettingsGroup {
                SettingsRow(
                    icon = painterResource(R.drawable.mdi_trash_outline),
                    title = "모든 기록 삭제",
                    subtitle = "로컬에 저장된 주행 기록을 모두 삭제합니다",
                    iconBackground = Color(0xFF4A1417),
                    iconColor = Color(0xFFFF4A55),
                    titleColor = Color(0xFFFF4A55),
                    showIconBackground = false,
                    onClick = onClearRecordsClick
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            SectionLabel("정보")
            SettingsGroup {
                SettingsRow(
                    painterResource(R.drawable.shield),
                    "개인정보 처리방침",
                    "",
                    Color(0xFF123E8C),
                    AccentBlue,
                    showIconBackground = false
                )
                SettingsDivider()
                SettingsRow(
                    painterResource(R.drawable.note),
                    "이용약관",
                    "",
                    Color(0xFF252C35),
                    TextSecondary,
                    showIconBackground = false
                )
                SettingsDivider()
                SettingsRow(
                    painterResource(R.drawable.info),
                    "앱 버전",
                    BuildConfig.VERSION_NAME,
                    Color(0xFF252C35),
                    TextSecondary,
                    showIconBackground = false
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "찍혔나?는 참고용 주행 속도 로그이며,\n단속 여부를 보장하지 않습니다.",
                color = TextMuted,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SettingsTopBar() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .background(AppBackground)
            .padding(horizontal = 20.dp),
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "설정",
            color = TextPrimary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
