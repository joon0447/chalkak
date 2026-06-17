package com.joon.chalkak.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.joon.chalkak.model.DriveRecord
import com.joon.chalkak.presentation.common.AccentBlue
import com.joon.chalkak.presentation.common.CarIcon
import com.joon.chalkak.presentation.common.SafeGreen
import com.joon.chalkak.presentation.common.ShieldIcon
import com.joon.chalkak.presentation.common.SlidersIcon
import com.joon.chalkak.presentation.common.StatusPill
import com.joon.chalkak.presentation.common.SurfaceDark
import com.joon.chalkak.presentation.common.TextMuted
import com.joon.chalkak.presentation.common.TextPrimary
import com.joon.chalkak.presentation.common.TextSecondary
import com.joon.chalkak.presentation.common.TinyIconBox
import com.joon.chalkak.presentation.common.WarningAmber
import com.joon.chalkak.presentation.common.WarningIcon
import com.joon.chalkak.presentation.main.MainUiState

@Composable
fun HistoryScreen(
    uiState: MainUiState,
    onRecordClick: (DriveRecord) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "주행 기록",
                color = TextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard(
                icon = CarIcon,
                value = uiState.historySummary.totalDriveCount,
                label = "총 주행",
                color = AccentBlue
            )
            SummaryCard(
                icon = ShieldIcon,
                value = uiState.historySummary.safePassRate,
                label = "안전 통과",
                color = SafeGreen
            )
            SummaryCard(
                icon = WarningIcon,
                value = uiState.historySummary.warningCount,
                label = "주의 기록",
                color = WarningAmber
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (uiState.driveRecordGroups.isEmpty()) {
            EmptyHistoryCard()
        } else {
            uiState.driveRecordGroups.forEach { group ->
                DateLabel(group.date)
                group.records.forEach { record ->
                    DriveRecordCard(
                        record = record,
                        onClick = { onRecordClick(record) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .padding(18.dp)
    ) {
        Text(
            text = "주행 기록 없음",
            color = TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "주행을 완료하면 기록이 날짜별로 표시됩니다",
            color = TextMuted,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun RowScope.SummaryCard(icon: ImageVector, value: String, label: String, color: Color) {
    Column(
        modifier = Modifier
            .weight(1f)
            .height(78.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceDark)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Text(
            text = value,
            color = TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold
        )
        Text(text = label, color = TextMuted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun DateLabel(text: String) {
    Text(
        text = text,
        color = TextMuted,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
    )
}

@Composable
private fun DriveRecordCard(
    record: DriveRecord,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = record.time,
                color = TextPrimary,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold
            )
            StatusPill(status = record.status)
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(text = record.route, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(9.dp))
        Row {
            Text(text = record.cameraCount, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = record.safeCount, color = SafeGreen, style = MaterialTheme.typography.bodySmall)
            if (record.warningText.isNotBlank()) {
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = record.warningText,
                    color = if (record.warningText.contains("주의")) WarningAmber else TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
