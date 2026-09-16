package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberCrimson
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberGold
import com.example.ui.theme.CyberNeonGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CyberHudGauge(
    percentage: Int,
    usedMb: Long,
    totalMb: Long,
    isBoosting: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 230.dp
) {
    val animatedPercent by animateFloatAsState(
        targetValue = percentage.toFloat(),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "ramPercentAnim"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "gaugeRotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isBoosting) 1800 else 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isBoosting) 400 else 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val gaugeColor = when {
        percentage >= 85 -> CyberCrimson
        percentage >= 70 -> CyberGold
        percentage >= 50 -> CyberCyan
        else -> CyberNeonGreen
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val centerOffset = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = this.size.minDimension / 2f - 18.dp.toPx()

            // Outer decorative ring
            drawCircle(
                color = Color(0xFF162238),
                radius = baseRadius + 10.dp.toPx(),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Outer rotating tick marks
            rotate(rotationAngle, pivot = centerOffset) {
                val tickCount = 36
                for (i in 0 until tickCount) {
                    val angleDeg = (i * (360f / tickCount))
                    val angleRad = Math.toRadians(angleDeg.toDouble())
                    val isMajor = i % 3 == 0
                    val tickLen = if (isMajor) 7.dp.toPx() else 3.5.dp.toPx()
                    val rInner = baseRadius + 4.dp.toPx()
                    val rOuter = rInner + tickLen

                    val startX = (centerOffset.x + rInner * cos(angleRad)).toFloat()
                    val startY = (centerOffset.y + rInner * sin(angleRad)).toFloat()
                    val endX = (centerOffset.x + rOuter * cos(angleRad)).toFloat()
                    val endY = (centerOffset.y + rOuter * sin(angleRad)).toFloat()

                    drawLine(
                        color = if (isMajor) gaugeColor.copy(alpha = 0.7f) else Color(0xFF263A5C),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // Background gauge track (260 degree arc)
            val startArc = 140f
            val sweepTotal = 260f
            val strokeW = 14.dp.toPx()
            val arcSize = Size(baseRadius * 2, baseRadius * 2)
            val arcTopLeft = Offset(centerOffset.x - baseRadius, centerOffset.y - baseRadius)

            drawArc(
                color = Color(0xFF10192A),
                startAngle = startArc,
                sweepAngle = sweepTotal,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )

            // Filled active gauge arc
            val activeSweep = (animatedPercent / 100f) * sweepTotal
            drawArc(
                brush = Brush.sweepGradient(
                    0.0f to CyberCyan,
                    0.5f to CyberGold,
                    1.0f to CyberCrimson,
                    center = centerOffset
                ),
                startAngle = startArc,
                sweepAngle = activeSweep.coerceAtLeast(2f),
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )

            // Inner cyan glowing pulse circle when boosting
            if (isBoosting) {
                drawCircle(
                    color = CyberCyan.copy(alpha = 0.15f * pulseScale),
                    radius = baseRadius * 0.75f * pulseScale
                )
            }
        }

        // Central text HUD
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "MÉMOIRE VIVE",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.8.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${animatedPercent.toInt()}%",
                color = TextPrimary,
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = (-1).sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            val usedGb = String.format("%.1f", usedMb / 1024.0)
            val totalGb = String.format("%.1f", totalMb / 1024.0)
            Text(
                text = "$usedGb / $totalGb Go",
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
