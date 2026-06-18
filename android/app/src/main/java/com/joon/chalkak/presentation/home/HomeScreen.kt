package com.joon.chalkak.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joon.chalkak.R
import com.joon.chalkak.model.NearbyCamera
import com.joon.chalkak.model.RecentRecord
import com.joon.chalkak.presentation.common.CameraIcon
import com.joon.chalkak.presentation.common.CarIcon
import com.joon.chalkak.presentation.common.CheckCircleIcon
import com.joon.chalkak.presentation.common.PlayIcon
import com.joon.chalkak.presentation.common.SafeGreen
import com.joon.chalkak.presentation.common.StatusPill
import com.joon.chalkak.presentation.common.SurfaceDark
import com.joon.chalkak.presentation.common.TextMuted
import com.joon.chalkak.presentation.common.TextPrimary
import com.joon.chalkak.presentation.common.TextSecondary
import com.joon.chalkak.presentation.common.WarningAmber
import com.joon.chalkak.presentation.main.MainUiState

@Composable
fun HomeScreen(
    uiState: MainUiState,
    onDrivingActionClick: () -> Unit,
    onAutoDrivingDetectionClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 32.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "현재 속도",
                color = TextMuted,
                style = MaterialTheme.typography.bodyMedium
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = uiState.currentSpeedKmh.toString(),
                    color = TextPrimary,
                    fontSize = 72.sp,
                    lineHeight = 76.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "km/h",
                    color = TextSecondary,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(58.dp))
        uiState.nearbyCamera?.let { camera ->
            CameraCard(camera = camera)
        } ?: EmptyInfoCard(
            title = "주변 단속 카메라 없음",
            subtitle = "카메라 데이터 업데이트 후 주행을 시작하세요"
        )
        Spacer(modifier = Modifier.height(26.dp))
        PrimaryActionButton(
            isTracking = uiState.isSpeedTracking,
            onClick = onDrivingActionClick
        )
        Spacer(modifier = Modifier.height(12.dp))
        AutoDrivingDetectionToggle(
            enabled = uiState.isAutoDrivingDetectionEnabled,
            subtitle = uiState.autoDrivingDetectionSubtitle,
            onClick = onAutoDrivingDetectionClick
        )
        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "최근 기록",
            color = TextSecondary,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))
        if (uiState.recentRecords.isEmpty()) {
            EmptyInfoCard(
                title = "최근 기록 없음",
                subtitle = "주행 기록이 생성되면 여기에 표시됩니다"
            )
        } else {
            uiState.recentRecords.forEach { record ->
                RecentRecordCard(record = record)
            }
        }
    }
}

@Composable
private fun AutoDrivingDetectionToggle(
    enabled: Boolean,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.car),
            contentDescription = null,
            tint = if (enabled) SafeGreen else TextMuted,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "자동 주행 감지",
                color = TextPrimary,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(text = subtitle, color = TextMuted, style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked = enabled,
            onCheckedChange = { onClick() }
        )
    }
}

@Composable
private fun CameraCard(camera: NearbyCamera) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = CameraIcon,
            contentDescription = null,
            tint = WarningAmber,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = "${camera.distanceText} ${camera.title}",
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = camera.subtitle,
                color = TextMuted,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun EmptyInfoCard(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .padding(horizontal = 18.dp, vertical = 17.dp)
    ) {
        Text(
            text = title,
            color = TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            color = TextMuted,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun PrimaryActionButton(
    isTracking: Boolean,
    onClick: () -> Unit
) {
    val buttonColor = if (isTracking) StopDrivingButtonColor else StartDrivingButtonColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(buttonColor)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isTracking) {
            Icon(
                painter = painterResource(R.drawable.pause),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        } else {
            Icon(
                imageVector = PlayIcon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isTracking) "주행 기록 종료" else "주행 기록 시작",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium.copy(

            ),
            fontWeight = FontWeight.Bold
        )
    }
}

private val StartDrivingButtonColor = Color(0xFF085B26)
private val StopDrivingButtonColor = Color(0xFFAD0303)

@Composable
private fun RecentRecordCard(record: RecentRecord) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .padding(horizontal = 18.dp, vertical = 17.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = CheckCircleIcon,
            contentDescription = null,
            tint = com.joon.chalkak.presentation.common.SafeGreen,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = record.title,
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = record.subtitle,
                color = TextMuted,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
