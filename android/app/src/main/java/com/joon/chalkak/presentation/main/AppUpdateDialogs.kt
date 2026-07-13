package com.joon.chalkak.presentation.main

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.joon.chalkak.ui.theme.ChalkakTheme

@Composable
fun AppUpdateAvailableDialog(
    onUpdateClick: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("새로운 버전이 있어요") },
        text = { Text("더 나은 찰칵을 사용할 수 있도록 최신 버전으로 업데이트해 주세요.") },
        confirmButton = {
            TextButton(onClick = onUpdateClick) {
                Text("업데이트")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("나중에")
            }
        }
    )
}

@Composable
fun AppUpdateDownloadedDialog(
    onInstallClick: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("업데이트 준비 완료") },
        text = { Text("앱을 다시 시작하여 업데이트를 적용할 수 있습니다.") },
        confirmButton = {
            TextButton(onClick = onInstallClick) {
                Text("다시 시작")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("나중에")
            }
        }
    )
}

@Preview
@Composable
private fun AppUpdateAvailableDialogPreview() {
    ChalkakTheme {
        AppUpdateAvailableDialog(onUpdateClick = {}, onDismiss = {})
    }
}
