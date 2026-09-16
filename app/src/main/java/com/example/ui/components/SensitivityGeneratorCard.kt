package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FreeFireSensitivity
import com.example.ui.theme.CyberCrimson
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberGold
import com.example.ui.theme.CyberNeonGreen
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.BoosterViewModel

@Composable
fun SensitivityGeneratorCard(
    viewModel: BoosterViewModel,
    modifier: Modifier = Modifier
) {
    val sensi by viewModel.freeFireSensitivity.collectAsState()
    val displayCaps by viewModel.displayCapabilities.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    var isTestingTouch by remember { mutableStateOf(false) }
    var touchX by remember { mutableFloatStateOf(0f) }
    var touchY by remember { mutableFloatStateOf(0f) }
    var touchFlickSpeed by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DarkSurface)
            .border(BorderStroke(1.5.dp, CyberCyan.copy(alpha = 0.7f)), RoundedCornerShape(20.dp))
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
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "GÉNÉRATEUR DE SENSIBILITÉ (0 - 200)",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Free Fire MAX • Visée Headshot & Tirs One-Tap",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(CyberGold.copy(alpha = 0.15f))
                        .border(1.dp, CyberGold.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "MAX 200",
                        color = CyberGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Presets Selection Row
            Text(
                text = "PROFILS RECOMMANDÉS",
                color = TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(FreeFireSensitivity.PRESETS) { preset ->
                    val isSelected = sensi.presetName == preset.presetName
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) CyberCyan else DarkSurfaceElevated)
                            .border(
                                1.dp,
                                if (isSelected) CyberCyan else DarkBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { viewModel.applySensitivityPreset(preset) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = preset.presetName,
                            color = if (isSelected) Color.Black else TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Auto-calibration button based on device screen
            Button(
                onClick = {
                    viewModel.autoGenerateDeviceSensitivity()
                    Toast.makeText(
                        context,
                        "Sensibilité calculée spécialement pour votre écran (${displayCaps.currentRefreshRate.toInt()}Hz) !",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberNeonGreen.copy(alpha = 0.15f),
                    contentColor = CyberNeonGreen
                ),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, CyberNeonGreen.copy(alpha = 0.5f))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CALIBRER SELON MON SMARTPHONE (${displayCaps.currentRefreshRate.toInt()}Hz)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sensitivity Sliders Grid (0 to 200)
            SensitivitySliderItem(
                label = "Générale (Regard / Mouvement)",
                value = sensi.general,
                accentColor = CyberCyan,
                onValueChange = { viewModel.updateSensitivity(general = it) }
            )

            SensitivitySliderItem(
                label = "Point Rouge (Red Dot)",
                value = sensi.redDot,
                accentColor = CyberCrimson,
                onValueChange = { viewModel.updateSensitivity(redDot = it) }
            )

            SensitivitySliderItem(
                label = "Viseur 2x",
                value = sensi.scope2x,
                accentColor = CyberNeonGreen,
                onValueChange = { viewModel.updateSensitivity(scope2x = it) }
            )

            SensitivitySliderItem(
                label = "Viseur 4x",
                value = sensi.scope4x,
                accentColor = CyberGold,
                onValueChange = { viewModel.updateSensitivity(scope4x = it) }
            )

            SensitivitySliderItem(
                label = "Viseur Sniper (AWM / Kar98k)",
                value = sensi.sniper,
                accentColor = Color(0xFFB388FF),
                onValueChange = { viewModel.updateSensitivity(sniper = it) }
            )

            SensitivitySliderItem(
                label = "Regard Libre (360°)",
                value = sensi.freeLook,
                accentColor = Color(0xFFFF80AB),
                onValueChange = { viewModel.updateSensitivity(freeLook = it) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // DPI & Fire Button Info Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceElevated)
                        .border(BorderStroke(1.dp, DarkBorder), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "DPI CONSEILLÉ",
                            color = TextMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${sensi.recommendedDpi} DPI",
                            color = CyberGold,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceElevated)
                        .border(BorderStroke(1.dp, DarkBorder), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "BOUTON DE TIR",
                            color = TextMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${sensi.fireButtonSizePercent}% (Taille)",
                            color = CyberCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: Copy Config & Test Area
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val textToCopy = """
                            🔥 SENSIBILITÉ ANOS V3 (FREE FIRE 0-200) 🔥
                            • Générale : ${sensi.general}
                            • Point Rouge : ${sensi.redDot}
                            • Viseur 2x : ${sensi.scope2x}
                            • Viseur 4x : ${sensi.scope4x}
                            • Viseur Sniper : ${sensi.sniper}
                            • Regard Libre : ${sensi.freeLook}
                            • DPI Écran : ${sensi.recommendedDpi}
                            • Bouton de Tir : ${sensi.fireButtonSizePercent}%
                        """.trimIndent()
                        clipboardManager.setText(AnnotatedString(textToCopy))
                        Toast.makeText(context, "Sensibilités (0-200) copiées avec succès !", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("copy_sensi_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberCyan,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "COPIER LES SENSIS",
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
                    )
                }

                Button(
                    onClick = { isTestingTouch = !isTestingTouch },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTestingTouch) CyberGold else DarkSurfaceElevated,
                        contentColor = if (isTestingTouch) Color.Black else TextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (isTestingTouch) CyberGold else DarkBorder)
                ) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isTestingTouch) "FERMER LE TEST" else "TESTER LE TOUCH",
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
                    )
                }
            }

            // Interactive Touch & Flick Test Canvas
            if (isTestingTouch) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF030712))
                        .border(BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)), RoundedCornerShape(12.dp))
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                touchX += dragAmount.x
                                touchY += dragAmount.y
                                touchFlickSpeed = (dragAmount.getDistance() * (sensi.general / 100f))
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "ZONE D'ENTRAÎNEMENT AU GLISSEMENT (DRAG TEST)",
                            color = CyberCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Glissez votre doigt ici pour tester la réactivité et la vitesse du One-Tap",
                            color = TextMuted,
                            fontSize = 9.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CyberNeonGreen.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "VITESSE DE FLICK : ${touchFlickSpeed.toInt()} px/frame (Fluidité Max)",
                                color = CyberNeonGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SensitivitySliderItem(
    label: String,
    value: Int,
    accentColor: Color,
    onValueChange: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "$value / 200",
                color = accentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        }

        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 0f..200f,
            steps = 199,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = DarkBorder
            ),
            modifier = Modifier.height(28.dp)
        )
    }
}
