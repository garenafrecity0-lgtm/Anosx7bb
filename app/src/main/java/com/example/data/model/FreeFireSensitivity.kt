package com.example.data.model

data class FreeFireSensitivity(
    val general: Int = 195,
    val redDot: Int = 190,
    val scope2x: Int = 185,
    val scope4x: Int = 175,
    val sniper: Int = 120,
    val freeLook: Int = 150,
    val recommendedDpi: Int = 520,
    val fireButtonSizePercent: Int = 47,
    val presetName: String = "One-Tap Headshot Pro"
) {
    companion object {
        const val MIN_SENSI = 0
        const val MAX_SENSI = 200

        val PRESETS = listOf(
            FreeFireSensitivity(
                general = 198,
                redDot = 195,
                scope2x = 188,
                scope4x = 178,
                sniper = 125,
                freeLook = 160,
                recommendedDpi = 580,
                fireButtonSizePercent = 45,
                presetName = "One-Tap Headshot Pro"
            ),
            FreeFireSensitivity(
                general = 200,
                redDot = 198,
                scope2x = 192,
                scope4x = 182,
                sniper = 110,
                freeLook = 175,
                recommendedDpi = 640,
                fireButtonSizePercent = 42,
                presetName = "Rusher M1887 & MP40"
            ),
            FreeFireSensitivity(
                general = 175,
                redDot = 165,
                scope2x = 160,
                scope4x = 155,
                sniper = 85,
                freeLook = 140,
                recommendedDpi = 480,
                fireButtonSizePercent = 52,
                presetName = "Sniper Précision AWM"
            ),
            FreeFireSensitivity(
                general = 185,
                redDot = 180,
                scope2x = 175,
                scope4x = 165,
                sniper = 130,
                freeLook = 150,
                recommendedDpi = 500,
                fireButtonSizePercent = 48,
                presetName = "Équilibré Tous Smartphones"
            )
        )
    }
}
