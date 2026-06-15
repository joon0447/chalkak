package com.joon.chalkak.presentation.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private fun lineIcon(name: String, block: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f, fill = null) {
            block()
        }
    }.build()

val HomeIcon = lineIcon("Home") {
    moveTo(4f, 11f)
    lineTo(12f, 4f)
    lineTo(20f, 11f)
    verticalLineTo(20f)
    horizontalLineTo(15f)
    verticalLineTo(14f)
    horizontalLineTo(9f)
    verticalLineTo(20f)
    horizontalLineTo(4f)
    close()
}

val HistoryIcon = lineIcon("History") {
    moveTo(5f, 7f)
    verticalLineTo(12f)
    horizontalLineTo(10f)
    moveTo(12f, 7f)
    verticalLineTo(12f)
    lineTo(15.5f, 14f)
    moveTo(5.5f, 16f)
    lineTo(7.5f, 18f)
    lineTo(10f, 19f)
    lineTo(13.5f, 19f)
    lineTo(17f, 17f)
    lineTo(19f, 13.5f)
    lineTo(19f, 10f)
    lineTo(17f, 6.8f)
    lineTo(13.5f, 5f)
}

val SettingsIcon = lineIcon("Settings") {
    moveTo(12f, 8f)
    horizontalLineTo(16f)
    verticalLineTo(12f)
    horizontalLineTo(12f)
    close()
    moveTo(12f, 3f)
    verticalLineTo(5.5f)
    moveTo(12f, 18.5f)
    verticalLineTo(21f)
    moveTo(3f, 12f)
    horizontalLineTo(5.5f)
    moveTo(18.5f, 12f)
    horizontalLineTo(21f)
    moveTo(5.7f, 5.7f)
    lineTo(7.5f, 7.5f)
    moveTo(16.5f, 16.5f)
    lineTo(18.3f, 18.3f)
    moveTo(18.3f, 5.7f)
    lineTo(16.5f, 7.5f)
    moveTo(7.5f, 16.5f)
    lineTo(5.7f, 18.3f)
}

val ShieldIcon = lineIcon("Shield") {
    moveTo(12f, 3.5f)
    lineTo(19f, 6.5f)
    verticalLineTo(11.5f)
    lineTo(17.5f, 16f)
    lineTo(12f, 20.5f)
    lineTo(6.5f, 16f)
    lineTo(5f, 11.5f)
    verticalLineTo(6.5f)
    close()
    moveTo(8.8f, 12f)
    lineTo(11f, 14.1f)
    lineTo(15.5f, 9.6f)
}

val CameraIcon = lineIcon("Camera") {
    moveTo(4f, 8.5f)
    horizontalLineTo(8f)
    lineTo(9.5f, 6f)
    horizontalLineTo(14.5f)
    lineTo(16f, 8.5f)
    horizontalLineTo(20f)
    verticalLineTo(18.5f)
    horizontalLineTo(4f)
    close()
    moveTo(10f, 13f)
    horizontalLineTo(14f)
    verticalLineTo(16f)
    horizontalLineTo(10f)
    close()
}

val PlayIcon = lineIcon("Play") {
    moveTo(8f, 5.5f)
    lineTo(18f, 12f)
    lineTo(8f, 18.5f)
    close()
}

val CheckCircleIcon = lineIcon("CheckCircle") {
    moveTo(12f, 4f)
    lineTo(16.5f, 5.3f)
    lineTo(20f, 9f)
    lineTo(20f, 14.5f)
    lineTo(16.5f, 18.7f)
    lineTo(12f, 20f)
    lineTo(7.5f, 18.7f)
    lineTo(4f, 14.5f)
    lineTo(4f, 9f)
    lineTo(7.5f, 5.3f)
    close()
    moveTo(8.5f, 12.2f)
    lineTo(10.8f, 14.4f)
    lineTo(15.6f, 9.6f)
}

val WarningIcon = lineIcon("Warning") {
    moveTo(12f, 4f)
    lineTo(21f, 19.5f)
    horizontalLineTo(3f)
    close()
    moveTo(12f, 9.5f)
    verticalLineTo(13.5f)
    moveTo(12f, 16.5f)
    verticalLineTo(16.8f)
}

val CarIcon = lineIcon("Car") {
    moveTo(5f, 15f)
    verticalLineTo(10.5f)
    lineTo(7f, 7f)
    horizontalLineTo(17f)
    lineTo(19f, 10.5f)
    verticalLineTo(15f)
    close()
    moveTo(7f, 15f)
    verticalLineTo(17f)
    moveTo(17f, 15f)
    verticalLineTo(17f)
    moveTo(8f, 12f)
    horizontalLineTo(16f)
}

val SlidersIcon = lineIcon("Sliders") {
    moveTo(5f, 6f)
    horizontalLineTo(19f)
    moveTo(5f, 12f)
    horizontalLineTo(19f)
    moveTo(5f, 18f)
    horizontalLineTo(19f)
    moveTo(9f, 4f)
    verticalLineTo(8f)
    moveTo(15f, 10f)
    verticalLineTo(14f)
    moveTo(11f, 16f)
    verticalLineTo(20f)
}

val PinIcon = lineIcon("Pin") {
    moveTo(12f, 21f)
    lineTo(17.5f, 13.5f)
    lineTo(18f, 9f)
    lineTo(15f, 5f)
    lineTo(12f, 4f)
    lineTo(9f, 5f)
    lineTo(6f, 9f)
    lineTo(6.5f, 13.5f)
    close()
    moveTo(10f, 9f)
    horizontalLineTo(14f)
    verticalLineTo(13f)
    horizontalLineTo(10f)
    close()
}

val GpsIcon = lineIcon("Gps") {
    moveTo(5f, 5f)
    lineTo(19f, 19f)
    moveTo(12f, 3.5f)
    verticalLineTo(7f)
    moveTo(12f, 17f)
    verticalLineTo(20.5f)
    moveTo(3.5f, 12f)
    horizontalLineTo(7f)
    moveTo(17f, 12f)
    horizontalLineTo(20.5f)
    moveTo(9f, 9f)
    lineTo(15f, 15f)
}

val ArchiveIcon = lineIcon("Archive") {
    moveTo(5f, 6f)
    horizontalLineTo(19f)
    verticalLineTo(10f)
    horizontalLineTo(5f)
    close()
    moveTo(7f, 10f)
    verticalLineTo(19f)
    horizontalLineTo(17f)
    verticalLineTo(10f)
    moveTo(10f, 13f)
    horizontalLineTo(14f)
}

val TrashIcon = lineIcon("Trash") {
    moveTo(5f, 7f)
    horizontalLineTo(19f)
    moveTo(9f, 7f)
    verticalLineTo(5f)
    horizontalLineTo(15f)
    verticalLineTo(7f)
    moveTo(7f, 7f)
    lineTo(8f, 20f)
    horizontalLineTo(16f)
    lineTo(17f, 7f)
    moveTo(10.5f, 11f)
    verticalLineTo(17f)
    moveTo(13.5f, 11f)
    verticalLineTo(17f)
}

val DocumentIcon = lineIcon("Document") {
    moveTo(7f, 4f)
    horizontalLineTo(14f)
    lineTo(18f, 8f)
    verticalLineTo(20f)
    horizontalLineTo(7f)
    close()
    moveTo(14f, 4f)
    verticalLineTo(8f)
    horizontalLineTo(18f)
    moveTo(9f, 12f)
    horizontalLineTo(15f)
    moveTo(9f, 15f)
    horizontalLineTo(15f)
}

val InfoIcon = lineIcon("Info") {
    moveTo(12f, 5f)
    horizontalLineTo(12.1f)
    moveTo(12f, 10.5f)
    verticalLineTo(17f)
    moveTo(5f, 5f)
    horizontalLineTo(19f)
    verticalLineTo(19f)
    horizontalLineTo(5f)
    close()
}

val ChevronRightIcon = lineIcon("ChevronRight") {
    moveTo(9f, 6f)
    lineTo(15f, 12f)
    lineTo(9f, 18f)
}
