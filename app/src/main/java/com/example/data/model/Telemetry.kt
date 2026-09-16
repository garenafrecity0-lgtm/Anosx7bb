package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class PerformanceMode(
    val title: String,
    val subtitle: String,
    val description: String,
    val targetFps: Int,
    val accentHex: Long,
    val tag: String
) {
    BEAST(
        title = "Beast Mode x7",
        subtitle = "Performance Maximale",
        description = "Libération agressive de la RAM, priorité CPU/GPU absolue et fréquence d'affichage débridée.",
        targetFps = 120,
        accentHex = 0xFFFF2E63,
        tag = "ULTRA"
    ),
    BALANCED(
        title = "Mode Équilibré",
        subtitle = "Fluidité & Autonomie",
        description = "Optimisation standard idéale pour des sessions de jeu prolongées sans surchauffe.",
        targetFps = 60,
        accentHex = 0xFF00F5FF,
        tag = "STABLE"
    ),
    ECO(
        title = "Éco Batterie",
        subtitle = "Consommation Réduite",
        description = "Gestion thermique active et restriction des services d'arrière-plan énergivores.",
        targetFps = 45,
        accentHex = 0xFF00FFA3,
        tag = "BATTERIE"
    ),
    COMPETITIVE(
        title = "Compétitif FPS",
        subtitle = "Zéro Latence",
        description = "Priorité paquet réseau, réactivité tactile maximale et limitation des baisses de framerate.",
        targetFps = 90,
        accentHex = 0xFF9D4EDD,
        tag = "LATENCE MIN"
    )
}

data class SystemTelemetry(
    val totalRamMb: Long = 0L,
    val availRamMb: Long = 0L,
    val usedRamMb: Long = 0L,
    val ramUsagePercent: Int = 0,
    val batteryTempC: Float = 32.0f,
    val batteryLevel: Int = 100,
    val isCharging: Boolean = false,
    val estimatedFps: Int = 60,
    val networkPingMs: Int = 28,
    val cpuCores: Int = 8,
    val cpuArch: String = "ARM64-v8a",
    val deviceModel: String = "Android Gamer",
    val isLowMemory: Boolean = false
)

data class GameApp(
    val packageName: String,
    val appName: String,
    val isNativeGame: Boolean = false,
    val isCustomAdded: Boolean = false,
    val lastBoosted: Long = 0L,
    val boostCount: Int = 0
)

enum class CrosshairStyle {
    CLASSIC_CROSS,
    DOT_PINPOINT,
    CIRCLE_RETICLE,
    TACTICAL_T,
    APEX_CHEVRON
}

data class CrosshairConfig(
    val style: CrosshairStyle = CrosshairStyle.CLASSIC_CROSS,
    val color: Color = Color(0xFF00F5FF),
    val sizeDp: Float = 28f,
    val thicknessDp: Float = 2.5f,
    val gapDp: Float = 8f,
    val showCenterDot: Boolean = true,
    val dotRadiusDp: Float = 2f
)
