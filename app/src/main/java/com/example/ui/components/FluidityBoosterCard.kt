package com.example.ui.components

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.MotionPhotosOn
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun FluidityBoosterCard(
    viewModel: BoosterViewModel,
    modifier: Modifier = Modifier
) {
    val displayCaps by viewModel.displayCapabilities.collectAsState()
    val antiInputLagActive by viewModel.antiInputLagActive.collectAsState()
    val antiMistouchActive by viewModel.antiMistouchActive.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    var brightnessSliderValue by remember { mutableFloatStateOf(100f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DarkSurface)
            .border(BorderStroke(1.5.dp, CyberNeonGreen.copy(alpha = 0.7f)), RoundedCornerShape(20.dp))
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
                            .background(CyberNeonGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MotionPhotosOn,
                            contentDescription = null,
                            tint = CyberNeonGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "ACCÉLÉRATEUR DE FLUIDITÉ & 120 FPS",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Mouvements ultra-lisses, zéro saccades & réactivité tactile",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(CyberNeonGreen.copy(alpha = 0.15f))
                        .border(1.dp, CyberNeonGreen.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${displayCaps.currentRefreshRate.toInt()} HZ ACTIF",
                        color = CyberNeonGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Refresh rate unlock & mode selection banner
            Text(
                text = "CHOIX DU TAUX DE RAFRAÎCHISSEMENT FORCÉ",
                color = TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 90Hz and 120Hz Selectors
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val is120Forced = displayCaps.forcedRate == 120 || displayCaps.currentRefreshRate >= 110f

                // 120Hz Button Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (is120Forced) CyberNeonGreen.copy(alpha = 0.15f) else DarkSurfaceElevated)
                        .border(
                            BorderStroke(
                                if (is120Forced) 2.dp else 1.dp,
                                if (is120Forced) CyberNeonGreen else DarkBorder
                            ),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = if (is120Forced) CyberNeonGreen else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "120 HZ",
                                color = if (is120Forced) CyberNeonGreen else TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = "Ultra Fluide • 120 FPS",
                            color = if (is120Forced) CyberNeonGreen.copy(alpha = 0.8f) else TextMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (activity != null) {
                                    viewModel.forceRefreshRate(activity.window, 120)
                                    Toast.makeText(
                                        context,
                                        "⚡ Fréquence forcée à 120Hz (60Hz Bloqué) !",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (is120Forced) CyberNeonGreen else DarkSurface,
                                contentColor = if (is120Forced) Color.Black else CyberNeonGreen
                            ),
                            shape = RoundedCornerShape(8.dp),
                            border = if (!is120Forced) BorderStroke(1.dp, CyberNeonGreen.copy(alpha = 0.5f)) else null,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = if (is120Forced) "✓ ACTIF" else "FORCER 120Hz",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                // 90Hz Button Card
                val is90Forced = displayCaps.forcedRate == 90 && displayCaps.currentRefreshRate < 110f
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (is90Forced) CyberCyan.copy(alpha = 0.15f) else DarkSurfaceElevated)
                        .border(
                            BorderStroke(
                                if (is90Forced) 2.dp else 1.dp,
                                if (is90Forced) CyberCyan else DarkBorder
                            ),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = if (is90Forced) CyberCyan else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "90 HZ",
                                color = if (is90Forced) CyberCyan else TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = "Compétition • 90 FPS",
                            color = if (is90Forced) CyberCyan.copy(alpha = 0.8f) else TextMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (activity != null) {
                                    viewModel.forceRefreshRate(activity.window, 90)
                                    Toast.makeText(
                                        context,
                                        "🎯 Fréquence forcée à 90Hz (60Hz Bloqué) !",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (is90Forced) CyberCyan else DarkSurface,
                                contentColor = if (is90Forced) Color.Black else CyberCyan
                            ),
                            shape = RoundedCornerShape(8.dp),
                            border = if (!is90Forced) BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)) else null,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = if (is90Forced) "✓ ACTIF" else "FORCER 90Hz",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 60Hz Blocked Badge Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyberCrimson.copy(alpha = 0.12f))
                    .border(BorderStroke(1.dp, CyberCrimson.copy(alpha = 0.4f)), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "🚫",
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "MODE 60 HZ DÉSACTIVÉ & BLOQUÉ",
                                color = CyberCrimson,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "GPU verrouillé sur 90Hz/120Hz pour zéro micro-stutter.",
                                color = TextMuted,
                                fontSize = 9.sp
                            )
                        }
                    }
                    Button(
                        onClick = { viewModel.openDisplayRefreshRateSettings() },
                        modifier = Modifier.height(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkSurfaceElevated,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        border = BorderStroke(1.dp, DarkBorder)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DisplaySettings,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Écran OS", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Anti Input Lag Switch (Zero latency)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceElevated)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Anti-Lag & Polling Tactile Zéro Latence",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Optimisation instantanée des glissements et swipes",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
                Switch(
                    checked = antiInputLagActive,
                    onCheckedChange = { viewModel.toggleAntiInputLag() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberNeonGreen,
                        checkedTrackColor = CyberNeonGreen.copy(alpha = 0.3f),
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkBorder
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Anti-Touché par Erreur (Mistouch Shield / Palm Rejection)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceElevated)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Anti-Touché par Erreur",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CyberCyan.copy(alpha = 0.15f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Bords & Paume",
                                color = CyberCyan,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "Bloque les appuis accidentels sur les bords d'écran en jeu",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
                Switch(
                    checked = antiMistouchActive,
                    onCheckedChange = { viewModel.toggleAntiMistouch() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberCyan,
                        checkedTrackColor = CyberCyan.copy(alpha = 0.3f),
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkBorder
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick OS Shortcuts to Unlock Max Fluidity in Games
            Text(
                text = "RÉGLAGES SYSTÈME POUR MAXIMISER LA FLUIDITÉ",
                color = TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pointer speed shortcut
                Button(
                    onClick = { viewModel.openPointerSpeedSettings() },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkSurfaceElevated,
                        contentColor = CyberCyan
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Vitesse Pointeur",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Developer Options for 0.5x animations
                Button(
                    onClick = { viewModel.openDeveloperOptions() },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkSurfaceElevated,
                        contentColor = CyberGold
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, CyberGold.copy(alpha = 0.5f))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Animation,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Animations 0.5x",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Hardware Brightness slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LUMINOSITÉ SYSTÈME : ${brightnessSliderValue.toInt()}%",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = {
                        brightnessSliderValue = 100f
                        val ok = viewModel.setScreenBrightness(100)
                        if (!ok) {
                            context.startActivity(viewModel.screenOptimizer.requestWriteSettingsPermissionIntent())
                            Toast.makeText(context, "Autorisez la modification des paramètres pour régler la luminosité", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Luminosité matérielle poussée à 100% !", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.height(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberGold.copy(alpha = 0.15f),
                        contentColor = CyberGold
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    border = BorderStroke(1.dp, CyberGold.copy(alpha = 0.5f))
                ) {
                    Text("100% MAX", fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
            }

            Slider(
                value = brightnessSliderValue,
                onValueChange = {
                    brightnessSliderValue = it
                    viewModel.setScreenBrightness(it.toInt())
                },
                valueRange = 15f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = CyberGold,
                    activeTrackColor = CyberGold,
                    inactiveTrackColor = DarkBorder
                ),
                modifier = Modifier.height(28.dp)
            )
        }
    }
}
