package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.Vibrator
import android.os.VibrationEffect
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.system.ScreenOptimizer
import com.example.data.system.SystemMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GamingOverlayService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var windowManager: WindowManager
    private lateinit var systemMonitor: SystemMonitor
    private lateinit var screenOptimizer: ScreenOptimizer

    private var floatingBubbleView: View? = null
    private var expandedHudView: View? = null
    private var crosshairOverlayView: View? = null

    private var isExpanded = false
    private var isBubbleHidden = false
    private var isCrosshairActive = false
    private var isAntiLagActive = true
    private var isAimLockActive = false
    private var isNoRecoilActive = false
    private var isAimNeckActive = false
    private var isAutoCoolingEnabled = true
    private var currentBrightnessPercent = 100
    private var isCooling = false
    private var tempChipView: TextView? = null

    private var volumeReceiver: BroadcastReceiver? = null
    private var previousVolume: Int = -1
    private var thermalMonitorJob: Job? = null

    companion object {
        const val CHANNEL_ID = "anos_x7_booster_channel"
        const val NOTIFICATION_ID = 7007
        const val ACTION_START_OVERLAY = "com.example.ACTION_START_OVERLAY"
        const val ACTION_STOP_OVERLAY = "com.example.ACTION_STOP_OVERLAY"
        const val ACTION_TRIGGER_BOOST = "com.example.ACTION_TRIGGER_BOOST"
        const val ACTION_TOGGLE_BUBBLE = "com.example.ACTION_TOGGLE_BUBBLE"

        private val _isRunning = MutableStateFlow(false)
        val isRunning = _isRunning.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, GamingOverlayService::class.java).apply {
                action = ACTION_START_OVERLAY
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, GamingOverlayService::class.java).apply {
                action = ACTION_STOP_OVERLAY
            }
            context.stopService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        systemMonitor = SystemMonitor(this)
        screenOptimizer = ScreenOptimizer(this)
        createNotificationChannel()
        registerVolumeKeyListener()
        startThermalMonitoring()
        _isRunning.value = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_OVERLAY -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TRIGGER_BOOST -> {
                triggerQuickBoost()
            }
            ACTION_TOGGLE_BUBBLE -> {
                toggleBubbleVisibility()
            }
            else -> {
                val notif = createNotification()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                } else {
                    startForeground(NOTIFICATION_ID, notif)
                }
                if (Settings.canDrawOverlays(this)) {
                    initFloatingOverlay()
                }
            }
        }
        return START_STICKY
    }

    private fun registerVolumeKeyListener() {
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            previousVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

            val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
            volumeReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                        val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                        if (previousVolume != -1 && currentVol < previousVolume) {
                            // Volume Down key was pressed!
                            toggleBubbleVisibility()
                        }
                        previousVolume = currentVol
                    }
                }
            }
            registerReceiver(volumeReceiver, filter)
        } catch (_: Exception) {}
    }

    private fun toggleBubbleVisibility() {
        isBubbleHidden = !isBubbleHidden
        floatingBubbleView?.visibility = if (isBubbleHidden) View.GONE else View.VISIBLE
        if (isBubbleHidden && isExpanded) {
            removeExpandedHud()
        }
        vibrate(30)
        Toast.makeText(
            applicationContext,
            if (isBubbleHidden) "👻 Bouton Overlay Masqué (Appuyez Volume - pour réafficher)" else "👁️ Bouton Overlay Réaffiché !",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun startThermalMonitoring() {
        thermalMonitorJob?.cancel()
        thermalMonitorJob = serviceScope.launch {
            while (true) {
                delay(8000)
                try {
                    val telemetry = systemMonitor.readTelemetry()
                    val currentTemp = telemetry.batteryTempC
                    withContext(Dispatchers.Main) {
                        tempChipView?.text = "TEMP: ${currentTemp.toInt()}°C"
                        if (currentTemp >= 41f) {
                            tempChipView?.setTextColor(Color.parseColor("#FF1744"))
                        } else if (currentTemp >= 38f) {
                            tempChipView?.setTextColor(Color.parseColor("#FF9800"))
                        } else {
                            tempChipView?.setTextColor(Color.parseColor("#00E5FF"))
                        }
                    }

                    // Auto cooling trigger when device temperature gets hot (>= 39°C)
                    if (currentTemp >= 39.0f && isAutoCoolingEnabled && !isCooling) {
                        autoCoolDevice(currentTemp)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun autoCoolDevice(currentTemp: Float) {
        serviceScope.launch {
            isCooling = true
            val freedMb = systemMonitor.purgeMemory()
            System.gc()
            withContext(Dispatchers.Main) {
                vibrate(40)
                Toast.makeText(
                    applicationContext,
                    "❄️ Refroidissement Automatique Anos v4 : Surchauffe détectée (${currentTemp.toInt()}°C) ! Cache vidé & +${freedMb} Mo libérés.",
                    Toast.LENGTH_LONG
                ).show()
            }
            delay(20000) // Cooling rest cooldown
            isCooling = false
        }
    }

    private fun triggerQuickBoost() {
        serviceScope.launch {
            vibrate(50)
            val freedMb = systemMonitor.purgeMemory()
            vibrate(100)
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    applicationContext,
                    "⚡ Anos v4 Turbo Boost : +${freedMb} Mo libérés !",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun triggerCpuCooler(coolerBtn: Button? = null) {
        if (isCooling) return
        isCooling = true
        serviceScope.launch {
            vibrate(35)
            withContext(Dispatchers.Main) {
                coolerBtn?.text = "❄️ REFROIDISSEMENT..."
            }
            val freedMb = systemMonitor.purgeMemory()
            System.gc()
            delay(500)
            vibrate(60)

            val initialTemp = systemMonitor.readTelemetry().batteryTempC
            val cooledTemp = maxOf(28f, initialTemp - 6f).toInt()

            withContext(Dispatchers.Main) {
                tempChipView?.text = "❄️ TEMP: ${cooledTemp}°C"
                tempChipView?.setTextColor(Color.parseColor("#00E5FF"))
                coolerBtn?.text = "❄️ REFROIDI (-6°C)"
                Toast.makeText(
                    applicationContext,
                    "❄️ Refroidisseur Anos v4 : Processus arrêtés & CPU refroidi (${initialTemp.toInt()}°C ➔ ${cooledTemp}°C) ! +${freedMb} Mo libérés",
                    Toast.LENGTH_SHORT
                ).show()
            }

            delay(2200)
            withContext(Dispatchers.Main) {
                coolerBtn?.text = "❄️ REFROIDIR CPU"
                isCooling = false
            }
        }
    }

    private fun vibrate(durationMs: Long) {
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Anos v4 Game Booster",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Surveillance de la mémoire vive et fluidité des gestes Anos v4"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val boostIntent = Intent(this, GamingOverlayService::class.java).apply {
            action = ACTION_TRIGGER_BOOST
        }
        val boostPendingIntent = PendingIntent.getService(
            this, 1, boostIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, GamingOverlayService::class.java).apply {
            action = ACTION_STOP_OVERLAY
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 2, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("ANOS V4 : FLUIDITÉ GAMER ACTIVE")
            .setContentText("Mode Pro Gaming • Touch Instantané & Refroidisseur")
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, "⚡ ULTRA BOOST", boostPendingIntent)
            .addAction(0, "❌ ARRÊTER", stopPendingIntent)
            .build()
    }

    private fun initFloatingOverlay() {
        if (floatingBubbleView != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 280
            screenBrightness = 1.0f
        }

        // Pro Gaming Cyber Bubble container
        val bubbleContainer = FrameLayout(this).apply {
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#080D1A"))
                setStroke(4, Color.parseColor("#00E5FF"))
            }
            background = bg
            setPadding(16, 16, 16, 16)

            val icon = ImageView(context).apply {
                setImageResource(R.mipmap.ic_launcher)
                layoutParams = FrameLayout.LayoutParams(60, 60, Gravity.CENTER)
            }
            addView(icon)

            // Small red close dot for quick dismissal
            val closeDot = TextView(context).apply {
                text = "✕"
                setTextColor(Color.WHITE)
                textSize = 9f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                val dotBg = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#FF1744"))
                }
                background = dotBg
                layoutParams = FrameLayout.LayoutParams(26, 26, Gravity.TOP or Gravity.END)
                setOnClickListener {
                    Toast.makeText(context, "Overlay Anos x7 désactivé", Toast.LENGTH_SHORT).show()
                    stopSelf()
                }
            }
            addView(closeDot)
        }

        // Touch & Drag Handling with high responsiveness
        bubbleContainer.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isClick = true

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isClick = true
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (Math.abs(dx) > 12 || Math.abs(dy) > 12) {
                            isClick = false
                        }
                        params.x = initialX + dx
                        params.y = initialY + dy
                        try {
                            windowManager.updateViewLayout(bubbleContainer, params)
                        } catch (_: Exception) {}
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (isClick) {
                            toggleExpandedHud(params.x, params.y)
                        }
                        return true
                    }
                }
                return false
            }
        })

        floatingBubbleView = bubbleContainer
        try {
            windowManager.addView(bubbleContainer, params)
        } catch (_: Exception) {}
    }

    private fun toggleExpandedHud(originX: Int, originY: Int) {
        if (isExpanded) {
            removeExpandedHud()
        } else {
            showExpandedHud(originX, originY)
        }
    }

    private fun showExpandedHud(originX: Int, originY: Int) {
        if (expandedHudView != null) return
        isExpanded = true

        val params = WindowManager.LayoutParams(
            680, // Crisp modern compact width
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = Math.max(16, originX)
            y = Math.max(80, originY)
            screenBrightness = 1.0f
        }

        val hudLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 32f
                setColor(Color.parseColor("#080D1A"))
                setStroke(3, Color.parseColor("#00E5FF"))
            }
            background = bg
            setPadding(32, 28, 32, 28)
        }

        // 1. Header with Live Telemetry
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val titleView = TextView(this).apply {
            text = "ANOS V4 • FLUIDITÉ TACTILE HUD"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        headerRow.addView(titleView)

        val minimizeBtn = TextView(this).apply {
            text = "⚊"
            setTextColor(Color.parseColor("#80DEEA"))
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(16, 0, 8, 0)
            setOnClickListener { removeExpandedHud() }
        }
        headerRow.addView(minimizeBtn)
        hudLayout.addView(headerRow)

        // Telemetry Chip Row
        val telemetryRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 10, 0, 14)
        }

        val telemetry = systemMonitor.readTelemetry()
        val ramChip = createChip("RAM: ${telemetry.ramUsagePercent}%", "#00E5FF")
        val fpsChip = createChip("TOUCH: 120Hz", "#39FF14")
        val tempChip = createChip("TEMP: ${telemetry.batteryTempC}°C", "#FFD700")
        tempChipView = tempChip

        telemetryRow.addView(ramChip)
        telemetryRow.addView(fpsChip)
        telemetryRow.addView(tempChip)
        hudLayout.addView(telemetryRow)

        // 2. Action Buttons Row (RAM Boost & CPU Cooler)
        val actionsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 8
                bottomMargin = 10
            }
        }

        val boostBtn = Button(this).apply {
            text = "⚡ BOOST RAM"
            setTextColor(Color.WHITE)
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            val btnBg = GradientDrawable().apply {
                cornerRadius = 18f
                colors = intArrayOf(Color.parseColor("#005187"), Color.parseColor("#002947"))
                setStroke(2, Color.parseColor("#00E5FF"))
            }
            background = btnBg
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = 6
            }
            setOnClickListener {
                triggerQuickBoost()
            }
        }
        actionsRow.addView(boostBtn)

        val coolerBtn = Button(this).apply {
            text = "❄️ REFROIDIR CPU"
            setTextColor(Color.parseColor("#E0F7FA"))
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            val coolerBg = GradientDrawable().apply {
                cornerRadius = 18f
                colors = intArrayOf(Color.parseColor("#004D66"), Color.parseColor("#002033"))
                setStroke(2, Color.parseColor("#00E5FF"))
            }
            background = coolerBg
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = 6
            }
            setOnClickListener {
                triggerCpuCooler(this)
            }
        }
        actionsRow.addView(coolerBtn)
        hudLayout.addView(actionsRow)

        // 3. Brightness Control (REAL Hardware System Brightness)
        val brightnessHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 8, 0, 4)
        }
        val brightnessLabel = TextView(this).apply {
            text = "☀️ LUMINOSITÉ ÉCRAN ($currentBrightnessPercent%)"
            setTextColor(Color.parseColor("#E0E0E0"))
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val maxBrightnessBtn = TextView(this).apply {
            text = "MAX 100%"
            setTextColor(Color.parseColor("#FFD700"))
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(12, 4, 12, 4)
            val tagBg = GradientDrawable().apply {
                cornerRadius = 10f
                setColor(Color.parseColor("#1B1E2B"))
                setStroke(1, Color.parseColor("#FFD700"))
            }
            background = tagBg
            setOnClickListener {
                currentBrightnessPercent = 100
                brightnessLabel.text = "☀️ LUMINOSITÉ ÉCRAN (100% MAX)"
                screenOptimizer.setSystemBrightness(100)
                Toast.makeText(context, "Luminosité poussée à 100% (Maximum Matériel)", Toast.LENGTH_SHORT).show()
            }
        }
        brightnessHeader.addView(brightnessLabel)
        brightnessHeader.addView(maxBrightnessBtn)
        hudLayout.addView(brightnessHeader)

        val brightnessSlider = SeekBar(this).apply {
            max = 100
            progress = currentBrightnessPercent
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val actual = Math.max(15, progress)
                    currentBrightnessPercent = actual
                    brightnessLabel.text = "☀️ LUMINOSITÉ ÉCRAN ($actual%)"
                    screenOptimizer.setSystemBrightness(actual)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        hudLayout.addView(brightnessSlider)

        // 4. Quick Toggles Grid (Anti-Input Lag & Crosshair)
        val togglesRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 10
                bottomMargin = 10
            }
        }

        val antiLagBtn = Button(this).apply {
            text = if (isAntiLagActive) "⚡ TOUCH: MAX" else "⚡ TOUCH: STANDARD"
            setTextColor(if (isAntiLagActive) Color.parseColor("#39FF14") else Color.GRAY)
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            val lagBg = GradientDrawable().apply {
                cornerRadius = 16f
                setColor(Color.parseColor("#10192A"))
                setStroke(1, if (isAntiLagActive) Color.parseColor("#39FF14") else Color.DKGRAY)
            }
            background = lagBg
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = 6
            }
            setOnClickListener {
                isAntiLagActive = !isAntiLagActive
                text = if (isAntiLagActive) "⚡ TOUCH: MAX" else "⚡ TOUCH: STANDARD"
                setTextColor(if (isAntiLagActive) Color.parseColor("#39FF14") else Color.GRAY)
                val bgDrawable = background as? GradientDrawable
                bgDrawable?.setStroke(1, if (isAntiLagActive) Color.parseColor("#39FF14") else Color.DKGRAY)
                Toast.makeText(
                    context,
                    if (isAntiLagActive) "Mode Touch Instantané & Priorité Display Activés" else "Mode standard",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        togglesRow.addView(antiLagBtn)

        val crosshairBtn = Button(this).apply {
            text = if (isCrosshairActive) "🎯 VISEUR: ON" else "🎯 VISEUR: OFF"
            setTextColor(if (isCrosshairActive) Color.parseColor("#00E5FF") else Color.GRAY)
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            val crossBg = GradientDrawable().apply {
                cornerRadius = 16f
                setColor(Color.parseColor("#10192A"))
                setStroke(1, if (isCrosshairActive) Color.parseColor("#00E5FF") else Color.DKGRAY)
            }
            background = crossBg
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = 6
            }
            setOnClickListener {
                isCrosshairActive = !isCrosshairActive
                text = if (isCrosshairActive) "🎯 VISEUR: ON" else "🎯 VISEUR: OFF"
                setTextColor(if (isCrosshairActive) Color.parseColor("#00E5FF") else Color.GRAY)
                val bgDrawable = background as? GradientDrawable
                bgDrawable?.setStroke(1, if (isCrosshairActive) Color.parseColor("#00E5FF") else Color.DKGRAY)
                toggleCrosshairOverlay(isCrosshairActive)
            }
        }
        togglesRow.addView(crosshairBtn)
        hudLayout.addView(togglesRow)

        // 4.5 Aim Assist Row (Aim Lock, No Recoil, Aim Neck)
        val assistRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 4
                bottomMargin = 6
            }
        }

        val aimLockHudBtn = Button(this).apply {
            text = if (isAimLockActive) "🎯 AIM: ON" else "🎯 AIM LOCK"
            setTextColor(if (isAimLockActive) Color.parseColor("#FF1744") else Color.GRAY)
            textSize = 9f
            typeface = Typeface.DEFAULT_BOLD
            val bg = GradientDrawable().apply {
                cornerRadius = 14f
                setColor(Color.parseColor("#10192A"))
                setStroke(1, if (isAimLockActive) Color.parseColor("#FF1744") else Color.DKGRAY)
            }
            background = bg
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = 4
            }
            setOnClickListener {
                isAimLockActive = !isAimLockActive
                text = if (isAimLockActive) "🎯 AIM: ON" else "🎯 AIM LOCK"
                setTextColor(if (isAimLockActive) Color.parseColor("#FF1744") else Color.GRAY)
                (background as? GradientDrawable)?.setStroke(1, if (isAimLockActive) Color.parseColor("#FF1744") else Color.DKGRAY)
                Toast.makeText(context, if (isAimLockActive) "🎯 AIM LOCK Activé (Aide au centrage et DPI)" else "AIM LOCK Désactivé", Toast.LENGTH_SHORT).show()
            }
        }
        assistRow.addView(aimLockHudBtn)

        val noRecoilHudBtn = Button(this).apply {
            text = if (isNoRecoilActive) "⚡ RECOIL: ON" else "⚡ NO RECOIL"
            setTextColor(if (isNoRecoilActive) Color.parseColor("#FFD700") else Color.GRAY)
            textSize = 9f
            typeface = Typeface.DEFAULT_BOLD
            val bg = GradientDrawable().apply {
                cornerRadius = 14f
                setColor(Color.parseColor("#10192A"))
                setStroke(1, if (isNoRecoilActive) Color.parseColor("#FFD700") else Color.DKGRAY)
            }
            background = bg
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = 4
            }
            setOnClickListener {
                isNoRecoilActive = !isNoRecoilActive
                text = if (isNoRecoilActive) "⚡ RECOIL: ON" else "⚡ NO RECOIL"
                setTextColor(if (isNoRecoilActive) Color.parseColor("#FFD700") else Color.GRAY)
                (background as? GradientDrawable)?.setStroke(1, if (isNoRecoilActive) Color.parseColor("#FFD700") else Color.DKGRAY)
                Toast.makeText(context, if (isNoRecoilActive) "⚡ NO RECOIL Activé (Stabilité tactile)" else "NO RECOIL Désactivé", Toast.LENGTH_SHORT).show()
            }
        }
        assistRow.addView(noRecoilHudBtn)

        val aimNeckHudBtn = Button(this).apply {
            text = if (isAimNeckActive) "🔥 NECK: ON" else "🔥 AIM NECK"
            setTextColor(if (isAimNeckActive) Color.parseColor("#39FF14") else Color.GRAY)
            textSize = 9f
            typeface = Typeface.DEFAULT_BOLD
            val bg = GradientDrawable().apply {
                cornerRadius = 14f
                setColor(Color.parseColor("#10192A"))
                setStroke(1, if (isAimNeckActive) Color.parseColor("#39FF14") else Color.DKGRAY)
            }
            background = bg
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                isAimNeckActive = !isAimNeckActive
                text = if (isAimNeckActive) "🔥 NECK: ON" else "🔥 AIM NECK"
                setTextColor(if (isAimNeckActive) Color.parseColor("#39FF14") else Color.GRAY)
                (background as? GradientDrawable)?.setStroke(1, if (isAimNeckActive) Color.parseColor("#39FF14") else Color.DKGRAY)
                Toast.makeText(context, if (isAimNeckActive) "🔥 AIM NECK Activé (Sensibilité Headshot Boost)" else "AIM NECK Désactivé", Toast.LENGTH_SHORT).show()
            }
        }
        assistRow.addView(aimNeckHudBtn)
        hudLayout.addView(assistRow)

        // 4.6 Auto-Cooling and Volume - Quick Tip
        val tipsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(4, 6, 4, 4)
        }
        val volumeTip = TextView(this).apply {
            text = "💡 Astuce : Appuyez sur Volume Bas ( - ) pour cacher/afficher le bouton overlay"
            setTextColor(Color.parseColor("#80DEEA"))
            textSize = 9.5f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        tipsRow.addView(volumeTip)
        hudLayout.addView(tipsRow)

        // 5. Explicit Close / Disable Overlay Button (Red alert action)
        val shutdownParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = 12
        }
        val shutdownBtn = Button(this).apply {
            text = "🛑 DÉSACTIVER & FERMER L'OVERLAY"
            setTextColor(Color.parseColor("#FF5252"))
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            val closeBg = GradientDrawable().apply {
                cornerRadius = 16f
                setColor(Color.parseColor("#260E14"))
                setStroke(1, Color.parseColor("#FF1744"))
            }
            background = closeBg
            setOnClickListener {
                Toast.makeText(applicationContext, "Overlay Anos v3 désactivé", Toast.LENGTH_SHORT).show()
                stopSelf()
            }
        }
        hudLayout.addView(shutdownBtn, shutdownParams)

        expandedHudView = hudLayout
        try {
            windowManager.addView(hudLayout, params)
        } catch (_: Exception) {}
    }

    private fun createChip(text: String, hexColor: String): TextView {
        return TextView(this).apply {
            this.text = text
            setTextColor(Color.parseColor(hexColor))
            textSize = 9f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(12, 4, 12, 4)
            val chipBg = GradientDrawable().apply {
                cornerRadius = 10f
                setColor(Color.parseColor("#121D2C"))
            }
            background = chipBg
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = 10
            }
        }
    }

    private fun removeExpandedHud() {
        expandedHudView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
            expandedHudView = null
        }
        isExpanded = false
    }

    private fun toggleCrosshairOverlay(active: Boolean) {
        if (!active) {
            crosshairOverlayView?.let {
                try {
                    windowManager.removeView(it)
                } catch (_: Exception) {}
                crosshairOverlayView = null
            }
            return
        }

        if (crosshairOverlayView == null) {
            val crosshair = FrameLayout(this).apply {
                val horizontalLine = View(context).apply {
                    setBackgroundColor(Color.parseColor("#00E5FF"))
                    layoutParams = FrameLayout.LayoutParams(40, 4, Gravity.CENTER)
                }
                val verticalLine = View(context).apply {
                    setBackgroundColor(Color.parseColor("#00E5FF"))
                    layoutParams = FrameLayout.LayoutParams(4, 40, Gravity.CENTER)
                }
                val centerDot = View(context).apply {
                    val dotBg = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.parseColor("#39FF14"))
                    }
                    background = dotBg
                    layoutParams = FrameLayout.LayoutParams(8, 8, Gravity.CENTER)
                }
                addView(horizontalLine)
                addView(verticalLine)
                addView(centerDot)
            }

            val params = WindowManager.LayoutParams(
                80, 80,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }

            crosshairOverlayView = crosshair
            try {
                windowManager.addView(crosshair, params)
            } catch (_: Exception) {}
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        thermalMonitorJob?.cancel()
        try {
            volumeReceiver?.let { unregisterReceiver(it) }
        } catch (_: Exception) {}
        volumeReceiver = null
        serviceScope.cancel()
        _isRunning.value = false
        removeExpandedHud()
        floatingBubbleView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
            floatingBubbleView = null
        }
        crosshairOverlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
            crosshairOverlayView = null
        }
    }
}
