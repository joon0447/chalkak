package com.joon.chalkak.presentation.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.joon.chalkak.presentation.common.AccentBlue
import com.joon.chalkak.presentation.common.AppBackground
import com.joon.chalkak.presentation.common.BackIcon
import com.joon.chalkak.presentation.common.SafeGreen
import com.joon.chalkak.presentation.common.SurfaceDark
import com.joon.chalkak.presentation.common.TextMuted
import com.joon.chalkak.presentation.common.TextPrimary
import com.joon.chalkak.presentation.common.TextSecondary
import com.joon.chalkak.presentation.common.WarningAmber

data class DrivingRegionOnboardingState(
    val selectedProvinces: List<String> = emptyList(),
    val isDownloading: Boolean = false,
    val progressText: String = "",
    val errorText: String? = null
) {
    val canSubmit: Boolean
        get() = selectedProvinces.isNotEmpty() && !isDownloading
}

@Composable
fun DrivingRegionOnboardingScreen(
    state: DrivingRegionOnboardingState,
    title: String = "주 운전 지역",
    subtitle: String = "자주 운전하는 시/도를 선택하면 해당 지역 카메라 데이터를 먼저 준비합니다.",
    actionText: String = "시작하기",
    topBarTitle: String? = null,
    applyStatusBarsPadding: Boolean = true,
    onBackClick: (() -> Unit)? = null,
    onProvinceToggle: (String) -> Unit,
    onAllProvinceToggle: () -> Unit,
    onSubmit: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AppBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (applyStatusBarsPadding) Modifier.statusBarsPadding() else Modifier)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.Center
        ) {
            if (topBarTitle != null) {
                RegionTopBar(
                    title = topBarTitle,
                    onBackClick = onBackClick
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
            Text(
                text = title,
                color = TextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "여러 지역을 선택할 수 있고, 전체 버튼으로 모든 지역을 선택할 수 있습니다.",
                color = WarningAmber,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(24.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceDark)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ProvinceCard(
                    province = "전체",
                    selected = state.selectedProvinces.containsAll(DrivingProvinceNames),
                    enabled = !state.isDownloading,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onAllProvinceToggle
                )
                ProvinceOptions(
                    selectedProvinces = state.selectedProvinces,
                    enabled = !state.isDownloading,
                    onProvinceToggle = onProvinceToggle
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${state.selectedProvinces.size}/${DrivingProvinceNames.size} 선택됨",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
                Button(
                    onClick = onSubmit,
                    enabled = state.canSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentBlue,
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF27313D),
                        disabledContentColor = TextMuted
                    )
                ) {
                    if (state.isDownloading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier
                                .padding(end = 10.dp)
                                .size(18.dp)
                        )
                    }
                    Text(
                        text = if (state.isDownloading) "데이터 준비 중" else actionText,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            if (state.progressText.isNotBlank()) {
                Text(
                    text = state.progressText,
                    color = SafeGreen,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            state.errorText?.let { errorText ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorText,
                    color = WarningAmber,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun RegionTopBar(
    title: String,
    onBackClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = BackIcon,
            contentDescription = "뒤로가기",
            tint = TextPrimary,
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(enabled = onBackClick != null) { onBackClick?.invoke() }
                .padding(5.dp)
        )
        Text(
            text = title,
            color = TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun ProvinceOptions(
    selectedProvinces: List<String>,
    enabled: Boolean,
    onProvinceToggle: (String) -> Unit
) {
    DrivingProvinceNames.chunked(3).forEach { rowItems ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            rowItems.forEach { province ->
                ProvinceCard(
                    province = province,
                    selected = province in selectedProvinces,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    onClick = { onProvinceToggle(province) }
                )
            }
            repeat(3 - rowItems.size) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ProvinceCard(
    province: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(15.dp)
    val backgroundColor = if (selected) Color(0xFF143C63) else Color(0xFF1C232D)
    val borderColor = if (selected) AccentBlue else Color(0xFF303946)
    val textColor = if (selected) TextPrimary else TextSecondary

    Text(
        text = province,
        color = textColor,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = modifier
            .height(42.dp)
            .clip(shape)
            .background(backgroundColor)
            .border(BorderStroke(1.dp, borderColor), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 13.dp)
    )
}

val DrivingProvinceNames = listOf(
    "서울특별시",
    "부산광역시",
    "대구광역시",
    "인천광역시",
    "광주광역시",
    "대전광역시",
    "울산광역시",
    "세종특별자치시",
    "경기도",
    "강원특별자치도",
    "충청북도",
    "충청남도",
    "전북특별자치도",
    "전라남도",
    "경상북도",
    "경상남도",
    "제주특별자치도"
)
