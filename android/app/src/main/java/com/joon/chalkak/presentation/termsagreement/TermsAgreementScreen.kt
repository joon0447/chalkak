package com.joon.chalkak.presentation.termsagreement

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.joon.chalkak.presentation.common.AccentBlue
import com.joon.chalkak.presentation.common.ChevronRightIcon
import com.joon.chalkak.presentation.common.SurfaceDark
import com.joon.chalkak.presentation.common.TextMuted
import com.joon.chalkak.presentation.common.TextPrimary
import com.joon.chalkak.presentation.common.TextSecondary
import com.joon.chalkak.ui.theme.ChalkakTheme
import com.joon.chalkak.ui.theme.DarkSurface

@Composable
fun TermsAgreementScreen(onAgree: () -> Unit) {
    var isAgeAgreed by remember { mutableStateOf(false) }
    var isBackgroundLocationAgreed by remember { mutableStateOf(false) }
    var isPrivacyPolicyAgreed by remember { mutableStateOf(false) }
    var isTermsOfServiceAgreed by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val canContinue = isAgeAgreed &&
        isBackgroundLocationAgreed &&
        isPrivacyPolicyAgreed &&
        isTermsOfServiceAgreed

    Scaffold(containerColor = DarkSurface) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "서비스 이용 동의",
                color = TextPrimary,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "찰칵을 시작하려면 아래 필수 항목에 모두 동의해 주세요.",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(28.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceDark)
                    .padding(vertical = 6.dp)
            ) {
                AgreementRow(
                    title = "[필수] 만 14세 이상입니다",
                    description = "만 14세 이상만 서비스를 이용할 수 있습니다.",
                    checked = isAgeAgreed,
                    onCheckedChange = { isAgeAgreed = it }
                )
                AgreementRow(
                    title = "[필수] 백그라운드 위치 접근",
                    description = "운전 자동 감지를 위해 앱을 사용하지 않을 때도 위치 정보에 접근합니다.",
                    checked = isBackgroundLocationAgreed,
                    onCheckedChange = { isBackgroundLocationAgreed = it }
                )
                AgreementRow(
                    title = "[필수] 개인정보처리방침",
                    description = "개인정보 수집 및 처리 방침에 동의합니다.",
                    checked = isPrivacyPolicyAgreed,
                    onCheckedChange = { isPrivacyPolicyAgreed = it },
                    onDetailsClick = {
                        uriHandler.openUri(PRIVACY_POLICY_URL)
                    }
                )
                AgreementRow(
                    title = "[필수] 서비스 이용방침",
                    description = "서비스 이용방침에 동의합니다.",
                    checked = isTermsOfServiceAgreed,
                    onCheckedChange = { isTermsOfServiceAgreed = it },
                    onDetailsClick = {
                        uriHandler.openUri(TERMS_OF_SERVICE_URL)
                    }
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "위치 권한은 동의 후 Android 시스템 화면에서 별도로 허용할 수 있습니다.",
                color = TextMuted,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = onAgree,
                enabled = canContinue,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentBlue,
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFF27313D),
                    disabledContentColor = TextMuted
                )
            ) {
                Text("동의하고 시작하기", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AgreementRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onDetailsClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = AccentBlue)
        )
        Column(modifier = Modifier.weight(1f).padding(top = 3.dp, end = 8.dp)) {
            Text(title, color = TextPrimary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(description, color = TextMuted, style = MaterialTheme.typography.bodySmall)
        }
        if (onDetailsClick != null) {
            IconButton(onClick = onDetailsClick) {
                Icon(
                    imageVector = ChevronRightIcon,
                    contentDescription = "$title 자세히 보기",
                    tint = TextMuted
                )
            }
        }
    }
}

private const val PRIVACY_POLICY_URL =
    "https://docs.google.com/document/d/14Q8KoW93nTqD1R14hNpIqjXmgJ6tYB9xJtaKDzH5Q5k/edit?usp=sharing"
private const val TERMS_OF_SERVICE_URL =
    "https://docs.google.com/document/d/1A6nCb1jyo82T9G0S0x4NQNp1RCmYRca61XruSUkim3o/edit?usp=sharing"

@Preview(showBackground = true, backgroundColor = 0xFF0B1016)
@Composable
private fun TermsAgreementScreenPreview() {
    ChalkakTheme { TermsAgreementScreen(onAgree = {}) }
}
