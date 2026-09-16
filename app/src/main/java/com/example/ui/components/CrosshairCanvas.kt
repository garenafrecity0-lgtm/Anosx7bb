package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.CrosshairConfig
import com.example.data.model.CrosshairStyle
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface

@Composable
fun CrosshairPreviewCanvas(
    config: CrosshairConfig,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)

            // Target background rings
            drawCircle(
                color = Color(0xFF162032),
                radius = this.size.minDimension * 0.42f,
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = Color(0xFF1E2D44),
                radius = this.size.minDimension * 0.28f,
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = Color(0xFF263A5C),
                radius = this.size.minDimension * 0.14f,
                style = Stroke(width = 1.dp.toPx())
            )

            val sizePx = config.sizeDp.dp.toPx()
            val thickPx = config.thicknessDp.dp.toPx()
            val gapPx = config.gapDp.dp.toPx()

            // Draw center dot if enabled
            if (config.showCenterDot) {
                drawCircle(
                    color = config.color,
                    radius = config.dotRadiusDp.dp.toPx(),
                    center = center
                )
            }

            when (config.style) {
                CrosshairStyle.CLASSIC_CROSS -> {
                    // Top
                    drawLine(
                        color = config.color,
                        start = Offset(center.x, center.y - gapPx),
                        end = Offset(center.x, center.y - gapPx - sizePx),
                        strokeWidth = thickPx,
                        cap = StrokeCap.Square
                    )
                    // Bottom
                    drawLine(
                        color = config.color,
                        start = Offset(center.x, center.y + gapPx),
                        end = Offset(center.x, center.y + gapPx + sizePx),
                        strokeWidth = thickPx,
                        cap = StrokeCap.Square
                    )
                    // Left
                    drawLine(
                        color = config.color,
                        start = Offset(center.x - gapPx, center.y),
                        end = Offset(center.x - gapPx - sizePx, center.y),
                        strokeWidth = thickPx,
                        cap = StrokeCap.Square
                    )
                    // Right
                    drawLine(
                        color = config.color,
                        start = Offset(center.x + gapPx, center.y),
                        end = Offset(center.x + gapPx + sizePx, center.y),
                        strokeWidth = thickPx,
                        cap = StrokeCap.Square
                    )
                }

                CrosshairStyle.DOT_PINPOINT -> {
                    drawCircle(
                        color = config.color,
                        radius = (config.dotRadiusDp + 3f).dp.toPx(),
                        center = center
                    )
                    drawCircle(
                        color = config.color.copy(alpha = 0.5f),
                        radius = (config.dotRadiusDp + 8f).dp.toPx(),
                        center = center,
                        style = Stroke(width = thickPx)
                    )
                }

                CrosshairStyle.CIRCLE_RETICLE -> {
                    drawCircle(
                        color = config.color,
                        radius = sizePx * 0.7f,
                        center = center,
                        style = Stroke(width = thickPx)
                    )
                    // 4 small cross marks
                    drawLine(
                        color = config.color,
                        start = Offset(center.x, center.y - sizePx * 0.7f - 4.dp.toPx()),
                        end = Offset(center.x, center.y - sizePx * 0.7f + 4.dp.toPx()),
                        strokeWidth = thickPx
                    )
                    drawLine(
                        color = config.color,
                        start = Offset(center.x, center.y + sizePx * 0.7f - 4.dp.toPx()),
                        end = Offset(center.x, center.y + sizePx * 0.7f + 4.dp.toPx()),
                        strokeWidth = thickPx
                    )
                    drawLine(
                        color = config.color,
                        start = Offset(center.x - sizePx * 0.7f - 4.dp.toPx(), center.y),
                        end = Offset(center.x - sizePx * 0.7f + 4.dp.toPx(), center.y),
                        strokeWidth = thickPx
                    )
                    drawLine(
                        color = config.color,
                        start = Offset(center.x + sizePx * 0.7f - 4.dp.toPx(), center.y),
                        end = Offset(center.x + sizePx * 0.7f + 4.dp.toPx(), center.y),
                        strokeWidth = thickPx
                    )
                }

                CrosshairStyle.TACTICAL_T -> {
                    // Left
                    drawLine(
                        color = config.color,
                        start = Offset(center.x - gapPx, center.y),
                        end = Offset(center.x - gapPx - sizePx, center.y),
                        strokeWidth = thickPx
                    )
                    // Right
                    drawLine(
                        color = config.color,
                        start = Offset(center.x + gapPx, center.y),
                        end = Offset(center.x + gapPx + sizePx, center.y),
                        strokeWidth = thickPx
                    )
                    // Bottom
                    drawLine(
                        color = config.color,
                        start = Offset(center.x, center.y + gapPx),
                        end = Offset(center.x, center.y + gapPx + sizePx),
                        strokeWidth = thickPx
                    )
                }

                CrosshairStyle.APEX_CHEVRON -> {
                    val path = Path().apply {
                        moveTo(center.x - sizePx * 0.7f, center.y + sizePx * 0.5f)
                        lineTo(center.x, center.y - gapPx)
                        lineTo(center.x + sizePx * 0.7f, center.y + sizePx * 0.5f)
                    }
                    drawPath(
                        path = path,
                        color = config.color,
                        style = Stroke(width = thickPx, cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}
