package com.example.data.system

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.view.Display
import android.view.Window
import android.view.WindowManager

data class DisplayCapabilities(
    val currentRefreshRate: Float,
    val maxRefreshRate: Float,
    val supportedRates: List<Float>,
    val isHighRefreshRateActive: Boolean,
    val canWriteSystemSettings: Boolean,
    val forcedRate: Int = 120
)

class ScreenOptimizer(private val context: Context) {

    private var currentForcedRate: Int = 120

    fun getDisplayCapabilities(activity: Activity? = null): DisplayCapabilities {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity?.display ?: wm.defaultDisplay
        } else {
            @Suppress("DEPRECATION")
            wm.defaultDisplay
        }

        val supportedModes = display.supportedModes ?: emptyArray()
        val rates = supportedModes.map { it.refreshRate }.distinct().sortedDescending()
        val maxRate = rates.maxOrNull() ?: display.refreshRate
        val currentRate = display.refreshRate

        val canWrite = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.System.canWrite(context)
        } else {
            true
        }

        return DisplayCapabilities(
            currentRefreshRate = currentRate,
            maxRefreshRate = maxRate,
            supportedRates = rates,
            isHighRefreshRateActive = currentRate >= 85f,
            canWriteSystemSettings = canWrite,
            forcedRate = currentForcedRate
        )
    }

    /**
     * Force specific refresh rate (90Hz or 120Hz) on Window and lock system peak rates.
     * Prevents Android from falling back to 60Hz.
     */
    fun forceTargetRefreshRate(window: Window?, targetHz: Int): Boolean {
        currentForcedRate = targetHz
        try {
            // Priority boost for UI rendering pipeline
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_DISPLAY)
        } catch (_: Exception) {}

        // Try locking system-level refresh rate properties if permitted
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.System.canWrite(context)) {
                val targetFloat = targetHz.toFloat()
                Settings.System.putFloat(context.contentResolver, "peak_refresh_rate", targetFloat)
                Settings.System.putFloat(context.contentResolver, "min_refresh_rate", targetFloat)
                Settings.System.putInt(context.contentResolver, "user_refresh_rate", targetHz)
            }
        } catch (_: Exception) {}

        if (window == null) return false

        try {
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.context.display
            } else {
                @Suppress("DEPRECATION")
                window.windowManager.defaultDisplay
            }

            val modes = display?.supportedModes ?: emptyArray()
            
            // Find mode matching targetHz (e.g. ~90Hz or ~120Hz), strictly avoiding 60Hz
            val targetMode = if (targetHz == 90) {
                modes.filter { it.refreshRate >= 85f && it.refreshRate <= 95f }
                    .maxByOrNull { it.refreshRate }
                    ?: modes.filter { it.refreshRate >= 85f }.minByOrNull { it.refreshRate }
                    ?: modes.maxByOrNull { it.refreshRate }
            } else { // 120Hz or higher
                modes.filter { it.refreshRate >= 115f }
                    .maxByOrNull { it.refreshRate }
                    ?: modes.filter { it.refreshRate >= 85f }.maxByOrNull { it.refreshRate }
                    ?: modes.maxByOrNull { it.refreshRate }
            }

            val params = window.attributes
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && targetMode != null) {
                params.preferredDisplayModeId = targetMode.modeId
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                params.preferredRefreshRate = targetHz.toFloat()
            }
            // Enable hardware acceleration
            window.setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            )
            window.attributes = params
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * Unlock peak refresh rate (defaults to 120Hz or 90Hz)
     */
    fun applyPeakPerformance(window: Window?): Boolean {
        return forceTargetRefreshRate(window, 120)
    }

    /**
     * Sets screen hardware brightness to the maximum (0-255)
     */
    fun setSystemBrightness(brightnessPercent: Int): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(context)) {
            return false
        }
        return try {
            val brightnessValue = (brightnessPercent * 255 / 100).coerceIn(10, 255)
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                brightnessValue
            )
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun requestWriteSettingsPermissionIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    fun openDisplayRefreshRateSettings() {
        val intents = listOf(
            Intent(Settings.ACTION_DISPLAY_SETTINGS),
            Intent("android.settings.DISPLAY_SETTINGS")
        )
        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            } catch (_: Exception) {}
        }
    }

    fun openDeveloperOptions() {
        val intents = listOf(
            Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS),
            Intent("android.settings.APPLICATION_DEVELOPMENT_SETTINGS"),
            Intent(Settings.ACTION_SETTINGS)
        )
        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            } catch (_: Exception) {}
        }
    }

    fun openPointerSpeedSettings() {
        val intents = listOf(
            Intent(Settings.ACTION_INPUT_METHOD_SETTINGS),
            Intent(Settings.ACTION_LOCALE_SETTINGS),
            Intent(Settings.ACTION_SETTINGS)
        )
        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            } catch (_: Exception) {}
        }
    }
}
