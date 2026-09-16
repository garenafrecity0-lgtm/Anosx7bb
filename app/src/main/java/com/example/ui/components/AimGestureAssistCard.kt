package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Flare
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LineWeight
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberCrimson
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberGold
import com.example.ui.theme.CyberNeonGreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.BoosterViewModel
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.min

@Composable
fun AimGestureAssistCard(
    viewModel: BoosterViewModel,
    modifier: Modifier = Modifier
) {
    val aimLockActive by viewModel.aimLockActive.collectAsState()
    val noRecoilActive by viewModel.noRecoilActive.collectAsState()
    val aimNeckActive by viewModel.aimNeckActive.collectAsState()
    val gestureSmoothActive by viewModel.gestureSmoothActive.collectAsState()

    val view = LocalView.current

    // Live gesture state (activates strictly upon user touch/drag gestures)
    var isTouching by remember { mutableStateOf(false) }
    var currentTouchPos by remember { mutableStateOf(Offset.Zero) }
    var smoothedPos by remember { mutableStateOf(Offset.Zero) }
    var touchVelocity by remember { mutableStateOf(0f) }
    val gesturePoints = remember { mutableStateListOf<Offset>() }
    val smoothedPoints = remember { mutableStateListOf<Offset>() }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DarkSurface)
            .border(BorderStroke(1.5.dp, CyberCyan.copy(alpha = 0.6f)), RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CyberCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GpsFixed,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "ASSISTANCE AIM & GESTES TACTILES",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(
                            text = "Modules de stabilisation & lissage gestuel",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (aimLockActive || noRecoilActive || aimNeckActive || gestureSmoothActive) CyberNeonGreen.copy(alpha = 0.15f) else DarkBorder)
                        .border(
                            1.dp,
                            if (aimLockActive || noRecoilActive || aimNeckActive || gestureSmoothActive) CyberNeonGreen.copy(alpha = 0.5f) else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (aimLockActive || noRecoilActive || aimNeckActive || gestureSmoothActive) "ACTIF" else "DÉSACTIVÉ",
                        color = if (aimLockActive || noRecoilActive || aimNeckActive || gestureSmoothActive) CyberNeonGreen else TextMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Module 1: AIM LOCK Switch
            AssistModuleSwitch(
                title = "AIM LOCK",
                badge = "Magnétique",
                badgeColor = CyberCrimson,
                description = "Verrouillage et stabilisation du réticule sur la cible",
                icon = Icons.Default.TrackChanges,
                isChecked = aimLockActive,
                onCheckedChange = { viewModel.toggleAimLock() },
                accentColor = CyberCrimson
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Module 2: NO RECOIL Switch
            AssistModuleSwitch(
                title = "NO RECOIL",
                badge = "Anti-Recul",
                badgeColor = CyberGold,
                description = "Compensation automatique de la dispersion et secousse",
                icon = Icons.Default.CenterFocusStrong,
                isChecked = noRecoilActive,
                onCheckedChange = { viewModel.toggleNoRecoil() },
                accentColor = CyberGold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Module 3: AIM NECK Switch
            AssistModuleSwitch(
                title = "AIM NECK",
                badge = "Headshot Boost",
                badgeColor = CyberNeonGreen,
                description = "Micro-centrage automatique vers la zone cou et tête",
                icon = Icons.Default.Flare,
                isChecked = aimNeckActive,
                onCheckedChange = { viewModel.toggleAimNeck() },
                accentColor = CyberNeonGreen
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Module 4: GESTURE SMOOTHING PACK Switch
            AssistModuleSwitch(
                title = "SMOOTH GESTURE PACK",
                badge = "Lissage 240Hz",
                badgeColor = CyberCyan,
                description = "Lissage de mouvement fluide & suppression des micro-saccades",
                icon = Icons.Default.LineWeight,
                isChecked = gestureSmoothActive,
                onCheckedChange = { viewModel.toggleGestureSmooth() },
                accentColor = CyberCyan
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Live Interactive Gesture Smoothing Canvas
            Text(
                text = "ZONE DE TEST & LISSAGE GESTUEL EN DIRECT",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Glissez votre doigt ci-dessous pour ressentir les modules en action :",
                color = TextMuted,
                fontSize = 10.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkBackground)
                    .border(
                        BorderStroke(
                            1.dp,
                            if (isTouching) CyberCyan else DarkBorder
                        ),
                        RoundedCornerShape(14.dp)
                    )
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                isTouching = true
                                currentTouchPos = offset
                                smoothedPos = offset
                                gesturePoints.clear()
                                smoothedPoints.clear()
                                gesturePoints.add(offset)
                                smoothedPoints.add(offset)
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            },
                            onDragEnd = {
                                isTouching = false
                                gesturePoints.clear()
                                smoothedPoints.clear()
                            },
                            onDragCancel = {
                                isTouching = false
                                gesturePoints.clear()
                                smoothedPoints.clear()
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val rawPos = change.position
                                currentTouchPos = rawPos
                                val dist = hypot(dragAmount.x.toDouble(), dragAmount.y.toDouble()).toFloat()
                                touchVelocity = dist

                                // Apply smoothing factor
                                val alpha = if (gestureSmoothActive) 0.30f else 0.75f
                                var targetX = smoothedPos.x + (rawPos.x - smoothedPos.x) * alpha
                                var targetY = smoothedPos.y + (rawPos.y - smoothedPos.y) * alpha

                                // If NO RECOIL is active, dampen and stabilize downward vertical recoil drift
                                if (noRecoilActive && dragAmount.y < 0) {
                                    targetY += dragAmount.y * 0.45f
                                }

                                // If AIM NECK is active, apply upward neck compensation micro-bias
                                if (aimNeckActive) {
                                    targetY -= 3.5f
                                }

                                val newSmoothed = Offset(targetX, targetY)
                                smoothedPos = newSmoothed

                                gesturePoints.add(rawPos)
                                if (gesturePoints.size > 24) gesturePoints.removeAt(0)

                                smoothedPoints.add(newSmoothed)
                                if (smoothedPoints.size > 24) smoothedPoints.removeAt(0)
                            }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val centerTarget = Offset(canvasWidth * 0.5f, canvasHeight * 0.45f)
                    val headNeckTarget = Offset(canvasWidth * 0.5f, canvasHeight * 0.32f)

                    // Draw grid lines
                    val step = 28.dp.toPx()
                    var x = 0f
                    while (x < canvasWidth) {
                        drawLine(
                            color = DarkBorder.copy(alpha = 0.35f),
                            start = Offset(x, 0f),
                            end = Offset(x, canvasHeight),
                            strokeWidth = 1f
                        )
                        x += step
                    }
                    var y = 0f
                    while (y < canvasHeight) {
                        drawLine(
                            color = DarkBorder.copy(alpha = 0.35f),
                            start = Offset(0f, y),
                            end = Offset(canvasWidth, y),
                            strokeWidth = 1f
                        )
                        y += step
                    }

                    // Draw Target Mock zones (Body, Neck, Head)
                    val targetCenter = if (aimNeckActive) headNeckTarget else centerTarget
                    drawCircle(
                        color = (if (aimNeckActive) CyberNeonGreen else CyberCyan).copy(alpha = 0.12f),
                        radius = 38.dp.toPx(),
                        center = targetCenter
                    )
                    drawCircle(
                        color = (if (aimNeckActive) CyberNeonGreen else CyberCyan).copy(alpha = 0.35f),
                        radius = 20.dp.toPx(),
                        center = targetCenter,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    drawCircle(
                        color = (if (aimNeckActive) CyberNeonGreen else CyberCyan).copy(alpha = 0.8f),
                        radius = 4.dp.toPx(),
                        center = targetCenter
                    )

                    // Draw Raw Gesture Trail vs Smoothed Gesture Trail ONLY when user is gesturing
                    if (isTouching && gesturePoints.size >= 2) {
                        // Raw touch points (red/muted dashes)
                        val rawPath = Path().apply {
                            moveTo(gesturePoints.first().x, gesturePoints.first().y)
                            for (i in 1 until gesturePoints.size) {
                                lineTo(gesturePoints[i].x, gesturePoints[i].y)
                            }
                        }
                        drawPath(
                            path = rawPath,
                            color = TextMuted.copy(alpha = 0.35f),
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Smoothed Neon Trail (CyberCyan / NeonGreen with glow)
                        val smoothPath = Path().apply {
                            moveTo(smoothedPoints.first().x, smoothedPoints.first().y)
                            for (i in 1 until smoothedPoints.size) {
                                lineTo(smoothedPoints[i].x, smoothedPoints[i].y)
                            }
                        }
                        drawPath(
                            path = smoothPath,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    if (aimLockActive) CyberCrimson else CyberCyan,
                                    if (aimNeckActive) CyberNeonGreen else CyberGold
                                )
                            ),
                            style = Stroke(
                                width = if (gestureSmoothActive) 4.5.dp.toPx() else 2.5.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )

                        // Finger Indicator Crosshair
                        val currentFocal = smoothedPos
                        drawCircle(
                            color = if (aimLockActive) CyberCrimson else CyberCyan,
                            radius = 9.dp.toPx(),
                            center = currentFocal,
                            style = Stroke(width = 2.dp.toPx())
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 3.dp.toPx(),
                            center = currentFocal
                        )

                        // If AIM LOCK is active and close to target: draw lock vector line
                        if (aimLockActive) {
                            val distToTarget = hypot(
                                (currentFocal.x - targetCenter.x).toDouble(),
                                (currentFocal.y - targetCenter.y).toDouble()
                            ).toFloat()

                            if (distToTarget < 160.dp.toPx()) {
                                drawLine(
                                    color = CyberCrimson.copy(alpha = pulseAlpha),
                                    start = currentFocal,
                                    end = targetCenter,
                                    strokeWidth = 2.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                                drawCircle(
                                    color = CyberCrimson.copy(alpha = 0.5f * pulseAlpha),
                                    radius = 28.dp.toPx(),
                                    center = targetCenter,
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }
                        }
                    }
                }

                // Overlay UI Status when not touching or during touch
                if (!isTouching) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfaceElevated.copy(alpha = 0.85f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TouchApp,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Touchez et faites glisser pour calibrer les gestes",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    // Live Real-Time Telemetry Bar during touch
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(DarkSurfaceElevated.copy(alpha = 0.9f))
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "POLLING : 240 HZ",
                            color = CyberNeonGreen,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (gestureSmoothActive) "LISSAGE : 99.4%" else "LISSAGE : STANDARD",
                            color = CyberCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (aimLockActive) "AIM LOCK : ACTIF" else "AIM : LIBRE",
                            color = if (aimLockActive) CyberCrimson else TextMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AssistModuleSwitch(
    title: String,
    badge: String,
    badgeColor: Color,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isChecked: Boolean,
    onCheckedChange: () -> Unit,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceElevated)
            .border(
                BorderStroke(
                    1.dp,
                    if (isChecked) accentColor.copy(alpha = 0.5f) else Color.Transparent
                ),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isChecked) accentColor.copy(alpha = 0.2f) else DarkSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isChecked) accentColor else TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        color = if (isChecked) TextPrimary else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(badgeColor.copy(alpha = 0.15f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badge,
                            color = badgeColor,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = description,
                    color = TextMuted,
                    fontSize = 9.sp
                )
            }
        }

        Switch(
            checked = isChecked,
            onCheckedChange = { onCheckedChange() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = accentColor,
                checkedTrackColor = accentColor.copy(alpha = 0.35f),
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = DarkBorder
            )
        )
    }
}
