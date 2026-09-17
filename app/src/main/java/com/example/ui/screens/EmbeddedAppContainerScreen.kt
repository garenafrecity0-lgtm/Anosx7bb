package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
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
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun EmbeddedAppContainerScreen(
    viewModel: BoosterViewModel,
    onCloseContainer: () -> Unit
) {
    val protectedApp by viewModel.protectedApp.collectAsState()
    val authStatus by viewModel.authStatus.collectAsState()

    var showHudOverlay by remember { mutableStateOf(true) }
    var containerMode by remember { mutableStateOf(if (protectedApp.embeddedAppUrl.isNotBlank()) "WEB" else "TRAINING_ARENA") }

    // Intercept hardware back button to confirm exit
    BackHandler {
        onCloseContainer()
    }

    // Auto-check auth every 10 seconds: if key expires, immediately close container
    LaunchedEffect(Unit) {
        while (true) {
            delay(10000)
            if (!viewModel.authStatus.value.isAuthenticated) {
                onCloseContainer()
                break
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B12))
    ) {
        // Main Container Content
        if (containerMode == "WEB" && protectedApp.embeddedAppUrl.isNotBlank()) {
            InAppWebEngine(
                url = protectedApp.embeddedAppUrl,
                onClose = onCloseContainer
            )
        } else {
            InAppVipTrainingArena(
                appName = protectedApp.appName,
                onClose = onCloseContainer
            )
        }

        // Floating VIP HUD Top Bar
        if (showHudOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                    color = DarkSurface.copy(alpha = 0.94f)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(CyberGold.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = CyberGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = protectedApp.appName,
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "⚡ Exécuté dans Anos Booster • Clé VIP Active",
                                        color = CyberNeonGreen,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (protectedApp.embeddedAppUrl.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            containerMode = if (containerMode == "WEB") "TRAINING_ARENA" else "WEB"
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (containerMode == "WEB") Icons.Default.Tune else Icons.Default.VideogameAsset,
                                            contentDescription = "Basculer",
                                            tint = CyberCyan,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = onCloseContainer,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(CyberCrimson.copy(alpha = 0.2f))
                                        .testTag("close_in_app_container_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Quitter le mode Sandbox",
                                        tint = CyberCrimson,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Live Telemetry Metrics HUD
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HudMetricBadge(label = "FPS", value = "120 FPS", color = CyberNeonGreen)
                            HudMetricBadge(label = "PING", value = "12 ms", color = CyberCyan)
                            HudMetricBadge(label = "THERMIQUE", value = "31°C", color = CyberCyan)
                            HudMetricBadge(label = "PROTECTION", value = "VIP ANOS", color = CyberGold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HudMetricBadge(label: String, value: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(DarkBackground.copy(alpha = 0.8f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = "$label: ",
            color = TextMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Normal
        )
        Text(
            text = value,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun InAppWebEngine(
    url: String,
    onClose: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var progress by remember { mutableFloatStateOf(0f) }
    var loadError by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        setSupportZoom(true)
                        builtInZoomControls = false
                        cacheMode = WebSettings.LOAD_DEFAULT
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    }
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    setBackgroundColor(android.graphics.Color.parseColor("#070B12"))

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            isLoading = true
                            loadError = false
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            isLoading = false
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            isLoading = false
                            loadError = true
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            progress = newProgress / 100f
                        }
                    }

                    loadUrl(url)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF070B12)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        color = CyberCyan,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "CHARGEMENT DU JEU DANS LE BOOSTER...",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .width(180.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = CyberGold,
                        trackColor = DarkSurfaceElevated
                    )
                }
            }
        }

        if (loadError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF070B12))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = CyberGold,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "CONNEXION AU SERVEUR SÉCURISÉ",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "L'application est chargée en local avec le module d'optimisation VIP 120 FPS actif.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Button(
                        onClick = onClose,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberCyan,
                            contentColor = DarkBackground
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("RETOURNER AU COFFRE-FORT", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun InAppVipTrainingArena(
    appName: String,
    onClose: () -> Unit
) {
    var generalSens by remember { mutableFloatStateOf(95f) }
    var redDotSens by remember { mutableFloatStateOf(98f) }
    var scope2xSens by remember { mutableFloatStateOf(92f) }
    var scope4xSens by remember { mutableFloatStateOf(89f) }
    var awmSens by remember { mutableFloatStateOf(85f) }

    var ultraTouchResponse by remember { mutableStateOf(true) }
    var autoAimStabilizer by remember { mutableStateOf(true) }
    var recoilSuppressor by remember { mutableStateOf(true) }
    var antiBanShield by remember { mutableStateOf(true) }
    var fps120Lock by remember { mutableStateOf(true) }

    var targetHits by remember { mutableIntStateOf(0) }
    var targetScore by remember { mutableIntStateOf(0) }
    var headshots by remember { mutableIntStateOf(0) }
    var targetX by remember { mutableFloatStateOf(0.5f) }
    var targetY by remember { mutableFloatStateOf(0.5f) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAnim"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = 70.dp)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header inside In-App Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(DarkSurfaceElevated, DarkSurface)
                    )
                )
                .border(BorderStroke(1.dp, CyberGold.copy(alpha = 0.4f)), RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "🎮 $appName",
                            color = CyberGold,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Module d'optimisation & Simulateur de Sensibilité VIP",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberNeonGreen.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "120 FPS LOCK",
                            color = CyberNeonGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        // INTERACTIVE TARGET SHOOTING / SENSITIVITY TEST PAD
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(DarkSurface)
                .border(BorderStroke(1.dp, CyberCyan.copy(alpha = 0.35f)), RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "STAND DE TEST RÉFLEXES TACTILE 120Hz",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Touchez la cible rouge pour calibrer la sensibilité de tir",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CyberCrimson.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "🎯 $headshots Headshots",
                                color = CyberCrimson,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CyberGold.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "⭐ $targetScore PTS",
                                color = CyberGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Interactive Canvas Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF04060A))
                        .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val width = size.width
                                val height = size.height
                                val targetPixelX = targetX * width
                                val targetPixelY = targetY * height
                                val distance = Math.hypot(
                                    (offset.x - targetPixelX).toDouble(),
                                    (offset.y - targetPixelY).toDouble()
                                )

                                if (distance < 60) {
                                    // Hit!
                                    targetHits++
                                    targetScore += if (distance < 25) {
                                        headshots++
                                        100 // Headshot
                                    } else {
                                        50
                                    }
                                    // Spawn new target location
                                    targetX = Random.nextFloat().coerceIn(0.15f, 0.85f)
                                    targetY = Random.nextFloat().coerceIn(0.15f, 0.85f)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val gridSpacing = 30.dp.toPx()
                        for (x in 0..(size.width / gridSpacing).toInt()) {
                            drawLine(
                                color = DarkBorder.copy(alpha = 0.3f),
                                start = Offset(x * gridSpacing, 0f),
                                end = Offset(x * gridSpacing, size.height),
                                strokeWidth = 1f
                            )
                        }
                        for (y in 0..(size.height / gridSpacing).toInt()) {
                            drawLine(
                                color = DarkBorder.copy(alpha = 0.3f),
                                start = Offset(0f, y * gridSpacing),
                                end = Offset(size.width, y * gridSpacing),
                                strokeWidth = 1f
                            )
                        }

                        // Draw Interactive Target
                        val center = Offset(targetX * size.width, targetY * size.height)
                        drawCircle(
                            color = CyberCrimson.copy(alpha = 0.25f),
                            radius = 35.dp.toPx() * pulseScale,
                            center = center
                        )
                        drawCircle(
                            color = CyberCrimson.copy(alpha = 0.6f),
                            radius = 20.dp.toPx(),
                            center = center
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 6.dp.toPx(),
                            center = center
                        )
                        // Crosshair lines
                        drawLine(
                            color = CyberCyan,
                            start = Offset(center.x - 40, center.y),
                            end = Offset(center.x + 40, center.y),
                            strokeWidth = 2f
                        )
                        drawLine(
                            color = CyberCyan,
                            start = Offset(center.x, center.y - 40),
                            end = Offset(center.x, center.y + 40),
                            strokeWidth = 2f
                        )
                    }

                    Text(
                        text = "TOUCHER LA CIBLE POUR TESTER LA RÉPONSE TACTILE",
                        color = TextMuted.copy(alpha = 0.5f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 6.dp)
                    )
                }
            }
        }

        // SENSITIVITY CALIBRATION SLIDERS
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(DarkSurface)
                .border(BorderStroke(1.dp, CyberCyan.copy(alpha = 0.3f)), RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CALIBRATION SENSIBILITÉ ULTRA RAPIDE",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                VipSensitivitySlider(
                    label = "Général (Rotation Rapide 360°)",
                    value = generalSens,
                    onValueChange = { generalSens = it }
                )
                VipSensitivitySlider(
                    label = "Point Rouge (Headshot Précision)",
                    value = redDotSens,
                    onValueChange = { redDotSens = it }
                )
                VipSensitivitySlider(
                    label = "Viseur 2X (Moyenne Portée)",
                    value = scope2xSens,
                    onValueChange = { scope2xSens = it }
                )
                VipSensitivitySlider(
                    label = "Viseur 4X (Longue Portée)",
                    value = scope4xSens,
                    onValueChange = { scope4xSens = it }
                )
                VipSensitivitySlider(
                    label = "Viseur Sniper AWM",
                    value = awmSens,
                    onValueChange = { awmSens = it }
                )
            }
        }

        // VIP MOD ENGINE TOGGLES
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(DarkSurface)
                .border(BorderStroke(1.dp, CyberNeonGreen.copy(alpha = 0.3f)), RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "MODULES VIP ACTIFS EN TEMPS RÉEL",
                    color = CyberNeonGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black
                )

                VipFeatureSwitch(
                    title = "Réponse Tactile 120Hz & Zéro Latence",
                    desc = "Élimine le délai de réponse au toucher sur tout l'écran",
                    checked = ultraTouchResponse,
                    onCheckedChange = { ultraTouchResponse = it }
                )
                VipFeatureSwitch(
                    title = "Stabilisateur de Recul & Viroscope",
                    desc = "Maintient le réticule stable pendant les tirs continus",
                    checked = autoAimStabilizer,
                    onCheckedChange = { autoAimStabilizer = it }
                )
                VipFeatureSwitch(
                    title = "Bouclier Anti-Détection & Anti-Ban",
                    desc = "Masque les modifications de sensibilité auprès du serveur",
                    checked = antiBanShield,
                    onCheckedChange = { antiBanShield = it }
                )
                VipFeatureSwitch(
                    title = "Verrouillage 120 FPS Ultra Fluide",
                    desc = "Force le taux de rafraîchissement maximal du GPU",
                    checked = fps120Lock,
                    onCheckedChange = { fps120Lock = it }
                )
            }
        }

        // CLOSE / EXIT BUTTON
        Button(
            onClick = onClose,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = DarkSurfaceElevated,
                contentColor = TextPrimary
            ),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, DarkBorder)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "FERMER LE JEU & RETOURNER AU COFFRE-FORT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun VipSensitivitySlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = TextSecondary, fontSize = 11.sp)
            Text(
                text = "${value.toInt()}%",
                color = CyberCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(
                thumbColor = CyberCyan,
                activeTrackColor = CyberCyan,
                inactiveTrackColor = DarkBorder
            )
        )
    }
}

@Composable
private fun VipFeatureSwitch(
    title: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceElevated)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(text = desc, color = TextMuted, fontSize = 10.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = CyberNeonGreen,
                checkedTrackColor = CyberNeonGreen.copy(alpha = 0.3f),
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = DarkBorder
            )
        )
    }
}
