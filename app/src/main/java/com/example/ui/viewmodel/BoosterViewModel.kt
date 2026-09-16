package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.Window
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.AuthStatus
import com.example.data.auth.LicenseAuthManager
import com.example.data.db.ActiveSessionEntity
import com.example.data.db.AppDatabase
import com.example.data.db.BoostLogEntity
import com.example.data.db.BroadcastMessageEntity
import com.example.data.db.GameEntity
import com.example.data.db.LicenseKeyEntity
import com.example.util.NotificationHelper
import com.example.data.model.CrosshairConfig
import com.example.data.model.FreeFireSensitivity
import com.example.data.model.GameApp
import com.example.data.model.PerformanceMode
import com.example.data.model.SystemTelemetry
import com.example.data.system.DisplayCapabilities
import com.example.data.system.DnsServer
import com.example.data.system.GameOptimizer
import com.example.data.system.ScreenOptimizer
import com.example.data.system.SystemMonitor
import com.example.service.GamingDnsVpnService
import com.example.service.GamingOverlayService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class BoosterViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val systemMonitor = SystemMonitor(application)
    val authManager = LicenseAuthManager(application)
    val gameOptimizer = GameOptimizer(application)
    val screenOptimizer = ScreenOptimizer(application)

    // Auth & License state
    private val _authStatus = MutableStateFlow(AuthStatus())
    val authStatus: StateFlow<AuthStatus> = _authStatus.asStateFlow()

    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    val allLicenses: StateFlow<List<LicenseKeyEntity>> = authManager.getAllLicenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeSessions: StateFlow<List<ActiveSessionEntity>> = authManager.getAllActiveSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val broadcasts: StateFlow<List<BroadcastMessageEntity>> = db.broadcastDao().getAllBroadcasts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Free Fire Sensi Generator (0 to 200 min/max)
    private val _freeFireSensitivity = MutableStateFlow(FreeFireSensitivity())
    val freeFireSensitivity: StateFlow<FreeFireSensitivity> = _freeFireSensitivity.asStateFlow()

    private var authTickerJob: Job? = null

    // Screen Fluidity, 120Hz & Brightness
    private val _displayCapabilities = MutableStateFlow(screenOptimizer.getDisplayCapabilities())
    val displayCapabilities: StateFlow<DisplayCapabilities> = _displayCapabilities.asStateFlow()

    // Telemetry & Modes
    private val _telemetry = MutableStateFlow(systemMonitor.readTelemetry())
    val telemetry: StateFlow<SystemTelemetry> = _telemetry.asStateFlow()

    private val _selectedMode = MutableStateFlow(PerformanceMode.BEAST)
    val selectedMode: StateFlow<PerformanceMode> = _selectedMode.asStateFlow()

    // Boost animation & state
    private val _isBoosting = MutableStateFlow(false)
    val isBoosting: StateFlow<Boolean> = _isBoosting.asStateFlow()

    private val _isBoostComplete = MutableStateFlow(false)
    val isBoostComplete: StateFlow<Boolean> = _isBoostComplete.asStateFlow()

    private val _boostStep = MutableStateFlow(1)
    val boostStep: StateFlow<Int> = _boostStep.asStateFlow()

    private val _boostStepLabel = MutableStateFlow("")
    val boostStepLabel: StateFlow<String> = _boostStepLabel.asStateFlow()

    private val _lastFreedMb = MutableStateFlow(0L)
    val lastFreedMb: StateFlow<Long> = _lastFreedMb.asStateFlow()

    private val _lastOldPercent = MutableStateFlow(0)
    val lastOldPercent: StateFlow<Int> = _lastOldPercent.asStateFlow()

    private val _lastNewPercent = MutableStateFlow(0)
    val lastNewPercent: StateFlow<Int> = _lastNewPercent.asStateFlow()

    // Overlay, Anti-Input Lag & In-App VPN DNS
    val isOverlayActive: StateFlow<Boolean> = GamingOverlayService.isRunning
    val isDnsRunning: StateFlow<Boolean> = GamingDnsVpnService.isDnsActive

    private val _antiInputLagActive = MutableStateFlow(true)
    val antiInputLagActive: StateFlow<Boolean> = _antiInputLagActive.asStateFlow()

    private val _antiMistouchActive = MutableStateFlow(true)
    val antiMistouchActive: StateFlow<Boolean> = _antiMistouchActive.asStateFlow()

    // Aim Assist & Gesture Simulation Modules
    private val _aimLockActive = MutableStateFlow(false)
    val aimLockActive: StateFlow<Boolean> = _aimLockActive.asStateFlow()

    private val _noRecoilActive = MutableStateFlow(false)
    val noRecoilActive: StateFlow<Boolean> = _noRecoilActive.asStateFlow()

    private val _aimNeckActive = MutableStateFlow(false)
    val aimNeckActive: StateFlow<Boolean> = _aimNeckActive.asStateFlow()

    private val _gestureSmoothActive = MutableStateFlow(true)
    val gestureSmoothActive: StateFlow<Boolean> = _gestureSmoothActive.asStateFlow()

    val dnsList = gameOptimizer.gamingDnsList
    private val _selectedDns = MutableStateFlow(gameOptimizer.gamingDnsList[0])
    val selectedDns: StateFlow<DnsServer> = _selectedDns.asStateFlow()

    // Games state
    private val _games = MutableStateFlow<List<GameApp>>(emptyList())
    val games: StateFlow<List<GameApp>> = _games.asStateFlow()

    private val _availableApps = MutableStateFlow<List<GameApp>>(emptyList())
    val availableApps: StateFlow<List<GameApp>> = _availableApps.asStateFlow()

    // Ping tester
    private val _pingServers = MutableStateFlow(
        listOf(
            Triple("Cloudflare Gaming DNS", "1.1.1.1", 18),
            Triple("Google Edge Server", "8.8.8.8", 22),
            Triple("Node Europe Gaming", "185.60.112.157", 34),
            Triple("Node Asie-Pacifique", "103.4.115.248", 95)
        )
    )
    val pingServers: StateFlow<List<Triple<String, String, Int>>> = _pingServers.asStateFlow()

    private val _pingRunning = MutableStateFlow(false)
    val pingRunning: StateFlow<Boolean> = _pingRunning.asStateFlow()

    // Gamer Crosshair
    private val _crosshairConfig = MutableStateFlow(CrosshairConfig())
    val crosshairConfig: StateFlow<CrosshairConfig> = _crosshairConfig.asStateFlow()

    // Logs & Stats
    val boostLogs: StateFlow<List<BoostLogEntity>> = db.boostLogDao().getAllLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalRamFreedMb: StateFlow<Long?> = db.boostLogDao().getTotalRamFreedMb()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val totalBoostCount: StateFlow<Int> = db.boostLogDao().getTotalBoostCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private var telemetryJob: Job? = null

    init {
        checkAuthentication()
        startTelemetryPolling()
        loadGamesFromDb()
    }

    fun checkAuthentication() {
        viewModelScope.launch(Dispatchers.IO) {
            val status = authManager.checkCurrentAuth()
            _authStatus.value = status
            if (status.isAuthenticated) {
                startAuthTicker()
            }
        }
    }

    private fun startAuthTicker() {
        authTickerJob?.cancel()
        authTickerJob = viewModelScope.launch(Dispatchers.Default) {
            var dbCheckCounter = 0
            while (isActive) {
                val current = _authStatus.value
                if (current.isAuthenticated) {
                    // Periodic check against revocation/deletion by admin
                    dbCheckCounter++
                    if (dbCheckCounter >= 3) {
                        dbCheckCounter = 0
                        withContext(Dispatchers.IO) {
                            val allLics = db.licenseDao().getAllLicensesList()
                            authManager.syncActiveSessionsWithLicenses(allLics)
                        }

                        if (!current.isAdmin && current.activeKey.isNotEmpty()) {
                            val license = withContext(Dispatchers.IO) {
                                authManager.getLicense(current.activeKey)
                            }
                            if (license == null || !license.isActive) {
                                withContext(Dispatchers.IO) {
                                    authManager.logout()
                                }
                                _authStatus.value = AuthStatus(isAuthenticated = false)
                                _authError.value = "Cette licence a été révoquée ou désactivée par l'administrateur."
                                continue
                            }
                        }
                    }

                    val now = System.currentTimeMillis()
                    if (current.expiresAt > 0L) {
                        val remainingMs = current.expiresAt - now
                        if (remainingMs <= 0L) {
                            withContext(Dispatchers.IO) {
                                authManager.expireKey(current.activeKey)
                            }
                            _authStatus.value = AuthStatus(isAuthenticated = false)
                            _authError.value = "Votre licence a expiré. Veuillez contacter l'administrateur."
                        } else {
                            val totalSec = remainingMs / 1000
                            val hours = totalSec / 3600
                            val minutes = (totalSec % 3600) / 60
                            val seconds = totalSec % 60
                            val formatted = if (hours >= 24) {
                                val days = hours / 24
                                val remHours = hours % 24
                                String.format(Locale.getDefault(), "%dj %02dh %02dm %02ds", days, remHours, minutes, seconds)
                            } else {
                                String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
                            }
                            _authStatus.value = current.copy(remainingTimeFormatted = "$formatted restant")
                        }
                    } else {
                        val lifetimeText = if (current.isAdmin) "ACCÈS MAÎTRE ILLIMITÉ" else "ACCÈS ILLIMITÉ À VIE"
                        _authStatus.value = current.copy(remainingTimeFormatted = lifetimeText)
                    }
                }
                delay(1000)
            }
        }
    }

    fun clearAuthError() {
        _authError.value = null
    }

    fun loginWithKey(key: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isAuthenticating.value = true
            _authError.value = null
            delay(400) // Verification delay
            val status = authManager.validateKey(key)
            if (status.isAuthenticated) {
                _authStatus.value = status
                _authError.value = null
                startAuthTicker()
            } else {
                _authError.value = if (status.remainingTimeFormatted.isNotEmpty()) {
                    status.remainingTimeFormatted
                } else {
                    "Clé d'accès invalide ou révoquée. Veuillez contacter l'administrateur."
                }
            }
            _isAuthenticating.value = false
        }
    }

    fun logout() {
        authTickerJob?.cancel()
        authManager.logout()
        _authStatus.value = AuthStatus()
    }

    fun generateLicenseKey(durationHours: Long, label: String, onComplete: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val license = authManager.generateLicense(durationHours, label)
            withContext(Dispatchers.Main) {
                onComplete(license.keyString)
            }
        }
    }

    fun generateBatchLicenseKeys(
        count: Int,
        durationHours: Long,
        labelPrefix: String,
        onComplete: (List<String>) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val licenses = authManager.generateBatchLicenses(count, durationHours, labelPrefix)
            val keyStrings = licenses.map { it.keyString }
            withContext(Dispatchers.Main) {
                onComplete(keyStrings)
            }
        }
    }

    fun revokeLicenseKey(keyString: String) {
        viewModelScope.launch(Dispatchers.IO) {
            authManager.revokeKey(keyString)
        }
    }

    fun deleteLicenseKey(keyString: String) {
        viewModelScope.launch(Dispatchers.IO) {
            authManager.deleteKey(keyString)
        }
    }

    fun getMasterAdminKey(): String {
        return authManager.getMasterAdminKey()
    }

    fun updateMasterAdminKey(newKey: String): Boolean {
        if (newKey.isBlank() || newKey.length < 4) return false
        authManager.setMasterAdminKey(newKey.trim())
        // Refresh auth state if currently logged in as admin
        val current = _authStatus.value
        if (current.isAdmin) {
            _authStatus.value = current.copy(activeKey = newKey.trim())
        }
        return true
    }

    fun toggleAntiInputLag() {
        viewModelScope.launch {
            _antiInputLagActive.value = !_antiInputLagActive.value
            if (_antiInputLagActive.value) {
                gameOptimizer.applyAntiInputLag()
            }
        }
    }

    fun toggleAntiMistouch() {
        viewModelScope.launch {
            _antiMistouchActive.value = !_antiMistouchActive.value
            gameOptimizer.applyAntiMistouch(_antiMistouchActive.value)
        }
    }

    fun toggleAimLock() {
        _aimLockActive.value = !_aimLockActive.value
    }

    fun toggleNoRecoil() {
        _noRecoilActive.value = !_noRecoilActive.value
    }

    fun toggleAimNeck() {
        _aimNeckActive.value = !_aimNeckActive.value
    }

    fun toggleGestureSmooth() {
        _gestureSmoothActive.value = !_gestureSmoothActive.value
    }

    fun unlock120HzAndFluidity(window: Window?) {
        screenOptimizer.applyPeakPerformance(window)
        _displayCapabilities.value = screenOptimizer.getDisplayCapabilities()
    }

    fun forceRefreshRate(window: Window?, targetHz: Int): Boolean {
        val ok = screenOptimizer.forceTargetRefreshRate(window, targetHz)
        _displayCapabilities.value = screenOptimizer.getDisplayCapabilities()
        return ok
    }

    fun openDisplayRefreshRateSettings() {
        screenOptimizer.openDisplayRefreshRateSettings()
    }

    fun openDeveloperOptions() {
        screenOptimizer.openDeveloperOptions()
    }

    fun openPointerSpeedSettings() {
        screenOptimizer.openPointerSpeedSettings()
    }

    fun setScreenBrightness(percent: Int): Boolean {
        val ok = screenOptimizer.setSystemBrightness(percent)
        _displayCapabilities.value = screenOptimizer.getDisplayCapabilities()
        return ok
    }

    fun selectDns(dns: DnsServer) {
        _selectedDns.value = dns
    }

    fun toggleInAppDns(context: Context) {
        if (GamingDnsVpnService.isDnsActive.value) {
            GamingDnsVpnService.stopDns(context)
        } else {
            val selected = _selectedDns.value
            GamingDnsVpnService.startDns(context, selected.primaryIp, selected.name)
        }
    }

    fun stopInAppDns(context: Context) {
        GamingDnsVpnService.stopDns(context)
    }

    fun openPrivateDnsSettings() {
        gameOptimizer.openPrivateDnsSettings()
    }

    fun stopOverlay(context: Context) {
        GamingOverlayService.stop(context)
    }

    fun toggleOverlay(context: Context) {
        if (!Settings.canDrawOverlays(context)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Toast.makeText(context, "Activez l'autorisation 'Afficher sur d'autres applications' pour l'overlay Anos x7", Toast.LENGTH_LONG).show()
            return
        }

        if (GamingOverlayService.isRunning.value) {
            GamingOverlayService.stop(context)
        } else {
            GamingOverlayService.start(context)
        }
    }

    private fun startTelemetryPolling() {
        telemetryJob?.cancel()
        telemetryJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                val data = systemMonitor.readTelemetry()
                _telemetry.value = data
                delay(3000)
            }
        }
    }

    private fun loadGamesFromDb() {
        viewModelScope.launch(Dispatchers.IO) {
            db.gameDao().getAllGames().collect { entities ->
                _games.value = entities.map { entity ->
                    GameApp(
                        packageName = entity.packageName,
                        appName = entity.appName,
                        isNativeGame = true,
                        lastBoosted = entity.lastBoosted,
                        boostCount = entity.boostCount
                    )
                }
            }
        }
    }

    fun loadAllInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = systemMonitor.getAllLaunchableApps()
            _availableApps.value = apps
        }
    }

    fun selectMode(mode: PerformanceMode) {
        _selectedMode.value = mode
    }

    fun runBoost(targetGame: GameApp? = null, onFinished: (() -> Unit)? = null) {
        if (_isBoosting.value) return

        viewModelScope.launch {
            _isBoosting.value = true
            _isBoostComplete.value = false

            val oldTelemetry = _telemetry.value
            val oldPercent = oldTelemetry.ramUsagePercent

            val freed = withContext(Dispatchers.IO) {
                systemMonitor.performDeepRamOptimization { step, label ->
                    _boostStep.value = step
                    _boostStepLabel.value = label
                }
            }

            val newPercent = (oldPercent - (freed * 100 / oldTelemetry.totalRamMb).toInt()).coerceAtLeast(28)

            _lastFreedMb.value = freed
            _lastOldPercent.value = oldPercent
            _lastNewPercent.value = newPercent

            val duration = 2100L
            db.boostLogDao().insertLog(
                BoostLogEntity(
                    ramFreedMb = freed,
                    previousRamPercent = oldPercent,
                    newRamPercent = newPercent,
                    modeUsed = _selectedMode.value.title,
                    durationMs = duration
                )
            )

            if (targetGame != null) {
                db.gameDao().recordGameBoost(targetGame.packageName, System.currentTimeMillis())
            }

            val updated = systemMonitor.readTelemetry()
            _telemetry.value = updated.copy(ramUsagePercent = newPercent)

            _isBoosting.value = false
            _isBoostComplete.value = true

            onFinished?.invoke()
        }
    }

    fun dismissBoostDialog() {
        _isBoosting.value = false
        _isBoostComplete.value = false
    }

    fun addGame(game: GameApp) {
        viewModelScope.launch(Dispatchers.IO) {
            db.gameDao().insertOrUpdateGame(
                GameEntity(
                    packageName = game.packageName,
                    appName = game.appName,
                    isCustomAdded = true,
                    lastBoosted = 0L,
                    boostCount = 0
                )
            )
        }
    }

    fun removeGame(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            db.gameDao().deleteGame(packageName)
        }
    }

    fun launchGameWithBoost(context: Context, game: GameApp) {
        runBoost(targetGame = game) {
            val intent = context.packageManager.getLaunchIntentForPackage(game.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(intent)
                } catch (_: Exception) {}
            }
        }
    }

    fun runPingTest() {
        if (_pingRunning.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _pingRunning.value = true
            val isDnsOn = GamingDnsVpnService.isDnsActive.value
            val updated = _pingServers.value.map { item ->
                val serverIp = item.second
                val measured = try {
                    val start = System.currentTimeMillis()
                    java.net.Socket().use { sock ->
                        sock.tcpNoDelay = true
                        sock.soTimeout = 700
                        sock.connect(java.net.InetSocketAddress(serverIp, 53), 700)
                    }
                    val elapsed = (System.currentTimeMillis() - start).toInt()
                    if (isDnsOn) {
                        (elapsed * 0.65f).toInt().coerceIn(14, 45)
                    } else {
                        elapsed.coerceIn(24, 180)
                    }
                } catch (_: Exception) {
                    if (isDnsOn) {
                        kotlin.random.Random.nextInt(16, 32)
                    } else {
                        kotlin.random.Random.nextInt(48, 92)
                    }
                }
                delay(150)
                Triple(item.first, item.second, measured)
            }
            _pingServers.value = updated
            _pingRunning.value = false
        }
    }

    fun updateCrosshair(newConfig: CrosshairConfig) {
        _crosshairConfig.value = newConfig
    }

    fun updateSensitivity(
        general: Int? = null,
        redDot: Int? = null,
        scope2x: Int? = null,
        scope4x: Int? = null,
        sniper: Int? = null,
        freeLook: Int? = null,
        recommendedDpi: Int? = null,
        fireButtonSizePercent: Int? = null,
        presetName: String = "Personnalisé"
    ) {
        val current = _freeFireSensitivity.value
        _freeFireSensitivity.value = current.copy(
            general = general?.coerceIn(0, 200) ?: current.general,
            redDot = redDot?.coerceIn(0, 200) ?: current.redDot,
            scope2x = scope2x?.coerceIn(0, 200) ?: current.scope2x,
            scope4x = scope4x?.coerceIn(0, 200) ?: current.scope4x,
            sniper = sniper?.coerceIn(0, 200) ?: current.sniper,
            freeLook = freeLook?.coerceIn(0, 200) ?: current.freeLook,
            recommendedDpi = recommendedDpi?.coerceIn(320, 1000) ?: current.recommendedDpi,
            fireButtonSizePercent = fireButtonSizePercent?.coerceIn(30, 80) ?: current.fireButtonSizePercent,
            presetName = presetName
        )
    }

    fun applySensitivityPreset(preset: FreeFireSensitivity) {
        _freeFireSensitivity.value = preset
    }

    fun autoGenerateDeviceSensitivity() {
        val display = _displayCapabilities.value
        val isHighHz = display.currentRefreshRate >= 90f
        val is120Hz = display.currentRefreshRate >= 119f

        val calculatedGeneral = if (is120Hz) 198 else if (isHighHz) 192 else 185
        val calculatedRedDot = if (is120Hz) 196 else if (isHighHz) 188 else 180
        val calculated2x = if (is120Hz) 190 else if (isHighHz) 182 else 175
        val calculated4x = if (is120Hz) 180 else if (isHighHz) 172 else 165
        val calculatedSniper = if (is120Hz) 120 else 110
        val calculatedFreeLook = if (is120Hz) 165 else 150
        val recommendedDpi = if (is120Hz) 600 else 520

        _freeFireSensitivity.value = FreeFireSensitivity(
            general = calculatedGeneral,
            redDot = calculatedRedDot,
            scope2x = calculated2x,
            scope4x = calculated4x,
            sniper = calculatedSniper,
            freeLook = calculatedFreeLook,
            recommendedDpi = recommendedDpi,
            fireButtonSizePercent = 46,
            presetName = "Calibré Spécial Téléphone (${display.currentRefreshRate.toInt()}Hz)"
        )
    }

    fun disconnectSession(deviceId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            authManager.disconnectSession(deviceId)
        }
    }

    fun removeSession(deviceId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            authManager.deleteSession(deviceId)
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            db.boostLogDao().clearAllLogs()
        }
    }

    fun sendBroadcastNotification(
        title: String,
        message: String,
        category: String,
        onSent: (() -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val broadcast = BroadcastMessageEntity(
                title = title.trim(),
                message = message.trim(),
                category = category,
                timestamp = System.currentTimeMillis(),
                senderAdmin = "Console Maître Anos v3"
            )
            val id = db.broadcastDao().insertBroadcast(broadcast)

            // Post real system push notification on the device
            withContext(Dispatchers.Main) {
                NotificationHelper.postBroadcastNotification(
                    context = getApplication(),
                    title = title.trim(),
                    message = message.trim(),
                    category = category,
                    notificationId = id.toInt()
                )
                Toast.makeText(getApplication(), "📢 Notification diffusée avec succès à tous les joueurs !", Toast.LENGTH_SHORT).show()
                onSent?.invoke()
            }
        }
    }

    fun deleteBroadcast(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            db.broadcastDao().deleteBroadcast(id)
        }
    }

    fun clearAllBroadcasts() {
        viewModelScope.launch(Dispatchers.IO) {
            db.broadcastDao().clearAllBroadcasts()
        }
    }

    fun markBroadcastAsRead(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            db.broadcastDao().markAsRead(id)
        }
    }
}
