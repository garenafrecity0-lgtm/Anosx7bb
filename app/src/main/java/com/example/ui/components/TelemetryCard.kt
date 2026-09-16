package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SystemTelemetry
import com.example.ui.theme.CyberCrimson
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberGold
import com.example.ui.theme.CyberNeonGreen
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun TelemetryGrid(
    telemetry: SystemTelemetry,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TelemetryTile(
                modifier = Modifier.weight(1f),
                title = "FPS CIBLE",
                value = "${telemetry.estimatedFps}",
                unit = "Hz",
                subtitle = "Taux de rafraîchissement",
                statusLabel = if (telemetry.estimatedFps >= 90) "ULTRA FLUIDE" else "STABLE",
                statusColor = if (telemetry.estimatedFps >= 90) CyberCyan else CyberNeonGreen,
                icon = Icons.Default.Speed
            )
            TelemetryTile(
                modifier = Modifier.weight(1f),
                title = "TEMPÉRATURE",
                value = String.format("%.1f", telemetry.batteryTempC),
                unit = "°C",
                subtitle = if (telemetry.batteryTempC < 37f) "Thermique Optimale" else "Charge Élevée",
                statusLabel = if (telemetry.batteryTempC < 37f) "NORMAL" else "CHAUD",
                statusColor = if (telemetry.batteryTempC < 37f) CyberNeonGreen else CyberCrimson,
                icon = Icons.Default.DeviceThermostat
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val pingColor = when {
                telemetry.networkPingMs < 35 -> CyberNeonGreen
                telemetry.networkPingMs < 65 -> CyberCyan
                telemetry.networkPingMs < 100 -> CyberGold
                else -> CyberCrimson
            }
            TelemetryTile(
                modifier = Modifier.weight(1f),
                title = "LATENCE PING",
                value = "${telemetry.networkPingMs}",
                unit = "ms",
                subtitle = "Serveur Gaming",
                statusLabel = if (telemetry.networkPingMs < 45) "FAIBLE LATENCE" else "MOYEN",
                statusColor = pingColor,
                icon = Icons.Default.Wifi
            )
            TelemetryTile(
                modifier = Modifier.weight(1f),
                title = "BATTERIE",
                value = "${telemetry.batteryLevel}",
                unit = "%",
                subtitle = if (telemetry.isCharging) "En charge rapide" else "Décharge standard",
                statusLabel = if (telemetry.isCharging) "EN CHARGE" else if (telemetry.batteryLevel > 20) "OK" else "FAIBLE",
                statusColor = if (telemetry.batteryLevel > 20) CyberNeonGreen else CyberCrimson,
                icon = Icons.Default.Bolt
            )
        }
    }
}

@Composable
fun TelemetryTile(
    title: String,
    value: String,
    unit: String,
    subtitle: String,
    statusLabel: String,
    statusColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurface)
            .border(BorderStroke(1.dp, DarkBorder), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = title,
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = statusLabel,
                        color = statusColor,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unit,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}
