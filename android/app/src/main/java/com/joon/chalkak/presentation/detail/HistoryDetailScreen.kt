package com.joon.chalkak.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.joon.chalkak.model.CameraPassDetail
import com.joon.chalkak.model.DriveRecord
import com.joon.chalkak.presentation.common.ChevronLeftIcon
import com.joon.chalkak.presentation.common.SafeGreen
import com.joon.chalkak.presentation.common.StatusPill
import com.joon.chalkak.presentation.common.SurfaceDark
import com.joon.chalkak.presentation.common.TextMuted
import com.joon.chalkak.presentation.common.TextPrimary
import com.joon.chalkak.presentation.common.TextSecondary
import com.joon.chalkak.presentation.common.TinyIconBox
import com.joon.chalkak.presentation.common.WarningAmber

@Composable
fun HistoryDetailScreen(
    record: DriveRecord?,
    onBackClick: () -> Unit
) {
    var showCameraPasses by remember(record?.id) { mutableStateOf(false) }

    LaunchedEffect(record?.id) {
        showCameraPasses = false
        withFrameNanos { }
        showCameraPasses = true
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onBackClick),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TinyIconBox(icon = ChevronLeftIcon, color = TextPrimary)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "통과한 카메라",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = record?.time ?: "기록을 찾을 수 없음",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        when {
            record == null -> item { MissingDriveRecordCard() }
            !showCameraPasses -> items(4) { CameraPassSkeletonCard() }
            record.cameraPasses.isEmpty() -> item { EmptyCameraPassCard() }
            else -> items(
                items = record.cameraPasses,
                key = { detail -> "${detail.passedTime}-${detail.location}-${detail.measuredSpeedText}" }
            ) { detail ->
                CameraPassDetailCard(detail = detail)
            }
        }
    }
}

@Composable
private fun CameraPassSkeletonCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceDark)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SkeletonLine(width = 64.dp, height = 18.dp)
            SkeletonLine(width = 58.dp, height = 22.dp)
        }
        Spacer(modifier = Modifier.height(14.dp))
        SkeletonLine(width = 190.dp, height = 18.dp)
        Spacer(modifier = Modifier.height(8.dp))
        SkeletonLine(width = 130.dp, height = 14.dp)
        Spacer(modifier = Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SkeletonLine(modifier = Modifier.weight(1f), height = 30.dp)
            SkeletonLine(modifier = Modifier.weight(1f), height = 30.dp)
            SkeletonLine(modifier = Modifier.weight(1f), height = 30.dp)
            SkeletonLine(modifier = Modifier.weight(1f), height = 30.dp)
        }
    }
}

@Composable
private fun SkeletonLine(
    width: Dp? = null,
    height: Dp,
    modifier: Modifier = Modifier
) {
    Spacer(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier)
            .height(height)
            .clip(RoundedCornerShape(6.dp))
            .background(TextMuted.copy(alpha = 0.24f))
    )
}

@Composable
private fun MissingDriveRecordCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceDark)
            .padding(16.dp)
    ) {
        Text(
            text = "기록 없음",
            color = TextPrimary,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "선택한 주행 기록을 찾을 수 없습니다",
            color = TextMuted,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun EmptyCameraPassCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceDark)
            .padding(16.dp)
    ) {
        Text(
            text = "통과 기록 없음",
            color = TextPrimary,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "이 주행에서는 기록된 단속 카메라가 없습니다",
            color = TextMuted,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun CameraPassDetailCard(detail: CameraPassDetail) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceDark)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = detail.passedTime,
                color = TextPrimary,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold
            )
            StatusPill(status = detail.status)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = detail.roadName ?: detail.location,
            color = TextPrimary,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
        if (detail.roadName != null && detail.roadName != detail.location) {
            Spacer(modifier = Modifier.height(3.dp))
            Text(text = detail.location, color = TextMuted, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DetailMetric(label = "측정", value = detail.measuredSpeedText, color = TextPrimary)
            DetailMetric(label = "제한", value = detail.speedLimitText, color = SafeGreen)
            DetailMetric(label = "단속", value = detail.enforcementThresholdText, color = WarningAmber)
            DetailMetric(label = "거리", value = detail.distanceText, color = TextSecondary)
        }
    }
}

@Composable
private fun RowScope.DetailMetric(
    label: String,
    value: String,
    color: Color
) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.Start
    ) {
        Text(text = label, color = TextMuted, style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = color,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
