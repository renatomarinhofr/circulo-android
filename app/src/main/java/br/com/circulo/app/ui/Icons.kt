package br.com.circulo.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Small, coherent 24-unit icon set; every outline shares the same weight. */
object CirculoIcons {
    private fun icon(name: String, draw: PathBuilder.() -> Unit) = ImageVector.Builder(
        name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f,
    ).apply {
        path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.7f,
            strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero, pathBuilder = draw)
    }.build()

    val Shield = icon("Shield") {
        moveTo(12f, 3f); lineTo(20f, 6f); verticalLineTo(11f)
        curveTo(20f, 16f, 16f, 20f, 12f, 22f)
        curveTo(8f, 20f, 4f, 16f, 4f, 11f); verticalLineTo(6f); close()
        moveTo(8.5f, 12f); lineTo(11f, 14.5f); lineTo(15.5f, 10f)
    }
    val History = icon("History") {
        moveTo(4f, 7f); curveTo(6f, 3f, 12f, 2f, 16f, 4f)
        curveTo(21f, 6f, 23f, 12f, 20f, 17f)
        curveTo(17f, 23f, 8f, 23f, 4f, 17f)
        moveTo(4f, 3f); verticalLineTo(8f); horizontalLineTo(9f)
        moveTo(12f, 8f); verticalLineTo(13f); lineTo(15f, 15f)
    }
    val Settings = icon("Settings") {
        moveTo(4f, 6f); horizontalLineTo(8f); moveTo(14f, 6f); horizontalLineTo(20f)
        moveTo(4f, 12f); horizontalLineTo(14f); moveTo(20f, 12f); horizontalLineTo(20f)
        moveTo(4f, 18f); horizontalLineTo(5f); moveTo(11f, 18f); horizontalLineTo(20f)
        moveTo(14f, 6f); arcToRelative(3f, 3f, 0f, true, true, -6f, 0f); arcToRelative(3f, 3f, 0f, true, true, 6f, 0f)
        moveTo(20f, 12f); arcToRelative(3f, 3f, 0f, true, true, -6f, 0f); arcToRelative(3f, 3f, 0f, true, true, 6f, 0f)
        moveTo(11f, 18f); arcToRelative(3f, 3f, 0f, true, true, -6f, 0f); arcToRelative(3f, 3f, 0f, true, true, 6f, 0f)
    }
    val Power = icon("Power") {
        moveTo(12f, 3f); verticalLineTo(11f)
        moveTo(6.4f, 5.8f); curveTo(1f, 10f, 4f, 21f, 12f, 21f)
        curveTo(20f, 21f, 23f, 10f, 17.6f, 5.8f)
    }
    val Check = icon("Check") { moveTo(5f, 12f); lineTo(10f, 17f); lineTo(19f, 7f) }
    val Pause = icon("Pause") { moveTo(8f, 5f); verticalLineTo(19f); moveTo(16f, 5f); verticalLineTo(19f) }
    val Arrow = icon("Arrow") {
        moveTo(4f, 12f); horizontalLineTo(20f); moveTo(14f, 6f); lineTo(20f, 12f); lineTo(14f, 18f)
    }
    val Chevron = icon("Chevron") { moveTo(9f, 5f); lineTo(16f, 12f); lineTo(9f, 19f) }
    val PhoneOff = icon("PhoneOff") {
        moveTo(8f, 3f); lineTo(4f, 3f); curveTo(0f, 8f, 8f, 23f, 17f, 21f)
        lineTo(21f, 17f); lineTo(16f, 13f); lineTo(13f, 15f)
        curveTo(10f, 13f, 8f, 11f, 8f, 8f); lineTo(10f, 6f); close()
        moveTo(16f, 3f); lineTo(22f, 9f); moveTo(22f, 3f); lineTo(16f, 9f)
    }
    val Contacts = icon("Contacts") {
        moveTo(16f, 7f); arcToRelative(4f, 4f, 0f, true, true, -8f, 0f); arcToRelative(4f, 4f, 0f, true, true, 8f, 0f)
        moveTo(4f, 21f); verticalLineTo(19f); curveTo(4f, 12f, 20f, 12f, 20f, 19f); verticalLineTo(21f)
    }
    val Lock = icon("Lock") {
        moveTo(7f, 10f); verticalLineTo(7f); curveTo(7f, 0.5f, 17f, 0.5f, 17f, 7f); verticalLineTo(10f)
        moveTo(5f, 10f); horizontalLineTo(19f); verticalLineTo(21f); horizontalLineTo(5f); close()
        moveTo(12f, 14f); verticalLineTo(17f)
    }
    val Info = icon("Info") {
        moveTo(22f, 12f); arcToRelative(10f, 10f, 0f, true, true, -20f, 0f); arcToRelative(10f, 10f, 0f, true, true, 20f, 0f)
        moveTo(12f, 11f); verticalLineTo(17f); moveTo(12f, 7f); verticalLineTo(7.1f)
    }
    val Trash = icon("Trash") {
        moveTo(3f, 6f); horizontalLineTo(21f); moveTo(9f, 6f); verticalLineTo(3f); horizontalLineTo(15f); verticalLineTo(6f)
        moveTo(5f, 6f); lineTo(6f, 21f); horizontalLineTo(18f); lineTo(19f, 6f)
        moveTo(10f, 10f); verticalLineTo(17f); moveTo(14f, 10f); verticalLineTo(17f)
    }
    val Moon = icon("Moon") {
        moveTo(20f, 15.5f); curveTo(10f, 18f, 6f, 9f, 10f, 3f)
        curveTo(1f, 4f, 0f, 18f, 9f, 21f); curveTo(15f, 23f, 20f, 19f, 20f, 15.5f); close()
    }
    val Touch = icon("Touch") {
        moveTo(9f, 13f); verticalLineTo(5f); curveTo(9f, 2f, 13f, 2f, 13f, 5f); verticalLineTo(11f)
        curveTo(18f, 8f, 23f, 13f, 19f, 21f); horizontalLineTo(10f); lineTo(4f, 14f)
        curveTo(3f, 12f, 5f, 10f, 7f, 12f); lineTo(9f, 14f)
    }
}
