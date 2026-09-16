package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.ActiveSessionEntity
import com.example.data.db.BroadcastMessageEntity
import com.example.data.db.LicenseKeyEntity
import com.example.util.NotificationHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.ui.theme.CyberCrimson
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberGold
import com.example.ui.theme.CyberNeonGreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkBorderGlowing
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.BoosterViewModel

private enum class AdminTab(val title: String, val icon: ImageVector) {
    GENERATOR("Générateur VIP", Icons.Default.FlashOn),
    SESSIONS("Sessions & Géoloc", Icons.Default.Devices),
    BROADCAST("Diffuser Notif", Icons.Default.NotificationsActive),
    LICENSES("Licences", Icons.Default.Key),
    SECURITY("Sécurité", Icons.Default.Security)
}

@Composable
fun AdminPanelScreen(
    viewModel: BoosterViewModel,
    modifier: Modifier = Modifier
) {
    val rawLicenses by viewModel.allLicenses.collectAsState()
    val activeSessions by viewModel.activeSessions.collectAsState()
    val broadcasts by viewModel.broadcasts.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // Never display the master key or admin internal keys in the user-facing licenses list
    val clientLicenses = remember(rawLicenses) {
        rawLicenses.filter { it.keyType != "ADMIN" }
    }

    var activeTab by remember { mutableStateOf(AdminTab.GENERATOR) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedDurationHours by remember { mutableStateOf<Long>(0L) } // 0 = Illimité
    var clientNote by remember { mutableStateOf("") }
    var newlyCreatedKey by remember { mutableStateOf<String?>(null) }
    var selectedBatchCount by remember { mutableStateOf(1) }
    var newlyCreatedBatchKeys by remember { mutableStateOf<List<String>>(emptyList()) }

    // Broadcast Push Notification States
    var broadcastTitleInput by remember { mutableStateOf("🔥 MISE À JOUR VIP ANOX / ANOS v4") }
    var broadcastMessageInput by remember { mutableStateOf("Nouvelle optimisation 120 FPS et fluidité tactile activée. Bon jeu à tous !") }
    var broadcastCategoryInput by remember { mutableStateOf("BOOST") }

    // Password Update States (strictly hidden with PasswordVisualTransformation)
    var newPasswordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }

    val durations = listOf(
        Pair("1 Heure", 1L),
        Pair("24 Heures", 24L),
        Pair("7 Jours", 168L),
        Pair("30 Jours", 720L),
        Pair("À Vie (Illimité)", 0L)
    )

    val onlineSessionsCount = activeSessions.count { it.isOnline }
    val activeKeysCount = clientLicenses.count { it.isActive && (it.expiresAt == 0L || it.expiresAt > System.currentTimeMillis()) }
    val expiredKeysCount = clientLicenses.count { !it.isActive || (it.expiresAt > 0L && System.currentTimeMillis() > it.expiresAt) }

    val filteredLicenses = remember(clientLicenses, searchQuery) {
        if (searchQuery.isBlank()) clientLicenses
        else clientLicenses.filter {
            it.keyString.contains(searchQuery, ignoreCase = true) ||
                    it.description.contains(searchQuery, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Futuristic Header Hero
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                DarkSurfaceElevated,
                                DarkSurface
                            )
                        )
                    )
                    .border(BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)), RoundedCornerShape(22.dp))
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            CyberCyan.copy(alpha = 0.35f),
                                            DarkSurface
                                        )
                                    )
                                )
                                .border(1.dp, CyberCyan, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "ANOS V4",
                                    color = CyberCyan,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ADMIN",
                                    color = TextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                            }
                            Text(
                                text = "Centre de Contrôle & Gestion des Licences",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(CyberNeonGreen.copy(alpha = 0.15f))
                            .border(1.dp, CyberNeonGreen.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(CyberNeonGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "EN LIGNE",
                                color = CyberNeonGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }

        // 2. Metrics Bar (4 Key Stats)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricStatCard(
                    title = "En Ligne",
                    value = "$onlineSessionsCount",
                    accentColor = CyberNeonGreen,
                    icon = Icons.Default.Devices,
                    modifier = Modifier.weight(1f)
                )
                MetricStatCard(
                    title = "Clés Actives",
                    value = "$activeKeysCount",
                    accentColor = CyberCyan,
                    icon = Icons.Default.Key,
                    modifier = Modifier.weight(1f)
                )
                MetricStatCard(
                    title = "Expirées",
                    value = "$expiredKeysCount",
                    accentColor = CyberCrimson,
                    icon = Icons.Default.Block,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3. Navigation Tabs
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(AdminTab.values()) { tab ->
                    val isSelected = activeTab == tab
                    val tabBadge = when (tab) {
                        AdminTab.SESSIONS -> " ($onlineSessionsCount)"
                        AdminTab.LICENSES -> " ($activeKeysCount)"
                        else -> ""
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) CyberCyan else DarkSurface)
                            .border(
                                1.dp,
                                if (isSelected) CyberCyan else DarkBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { activeTab = tab }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                tint = if (isSelected) Color.Black else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${tab.title}$tabBadge",
                                color = if (isSelected) Color.Black else TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // TAB 1: VIP KEY GENERATOR
        if (activeTab == AdminTab.GENERATOR) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(DarkSurface)
                        .border(BorderStroke(1.dp, CyberCyan.copy(alpha = 0.3f)), RoundedCornerShape(20.dp))
                        .padding(18.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CyberCyan.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VpnKey,
                                    contentDescription = null,
                                    tint = CyberCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "GÉNÉRER UNE CLÉ CLIENT",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "Clé universelle 100% fonctionnelle sur le téléphone d'un ami",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "DURÉE DE VALIDITÉ DE LA CLÉ",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(durations) { (label, hours) ->
                                val isSelected = selectedDurationHours == hours
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) CyberCyan else DarkSurfaceElevated)
                                        .border(
                                            1.dp,
                                            if (isSelected) CyberCyan else DarkBorder,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedDurationHours = hours }
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color.Black else TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "QUANTITÉ DE CLÉS À GÉNÉRER",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val batchCounts = listOf(1, 5, 10, 25, 50)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(batchCounts) { count ->
                                val isSelected = selectedBatchCount == count
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) CyberCyan else DarkSurfaceElevated)
                                        .border(
                                            1.dp,
                                            if (isSelected) CyberCyan else DarkBorder,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedBatchCount = count }
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = if (count == 1) "1 Clé" else "$count Clés",
                                        color = if (isSelected) Color.Black else TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = clientNote,
                            onValueChange = { clientNote = it },
                            placeholder = { Text("Nom ou note du joueur (ex: Ami Tournoi FF)", color = TextMuted, fontSize = 12.sp) },
                            label = { Text("Note / Pseudo du Joueur (Optionnel)", color = TextSecondary, fontSize = 11.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DarkSurfaceElevated,
                                unfocusedContainerColor = DarkSurfaceElevated,
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Fast 50 Keys Direct Action Button
                        Button(
                            onClick = {
                                selectedBatchCount = 50
                                viewModel.generateBatchLicenseKeys(
                                    count = 50,
                                    durationHours = selectedDurationHours,
                                    labelPrefix = clientNote.ifBlank { "Lot VIP 50" }
                                ) { keys ->
                                    newlyCreatedKey = null
                                    newlyCreatedBatchKeys = keys
                                    val allFormatted = keys.joinToString("\n")
                                    clipboardManager.setText(AnnotatedString(allFormatted))
                                    Toast.makeText(context, "⚡ 50 Clés générées et copiées en 1 clic !", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("generate_50_keys_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan,
                                contentColor = Color(0xFF001824)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "⚡ GÉNÉRER 50 CLÉS D'UN COUP",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Standard Action Button according to chosen quantity
                        Button(
                            onClick = {
                                if (selectedBatchCount == 1) {
                                    viewModel.generateLicenseKey(
                                        durationHours = selectedDurationHours,
                                        label = clientNote
                                    ) { newKey ->
                                        newlyCreatedBatchKeys = emptyList()
                                        newlyCreatedKey = newKey
                                        clipboardManager.setText(AnnotatedString(newKey))
                                        Toast.makeText(context, "Clé $newKey générée et copiée !", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    viewModel.generateBatchLicenseKeys(
                                        count = selectedBatchCount,
                                        durationHours = selectedDurationHours,
                                        labelPrefix = clientNote
                                    ) { keys ->
                                        newlyCreatedKey = null
                                        newlyCreatedBatchKeys = keys
                                        val allFormatted = keys.joinToString("\n")
                                        clipboardManager.setText(AnnotatedString(allFormatted))
                                        Toast.makeText(context, "${keys.size} Clés générées et copiées !", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("generate_license_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkSurfaceElevated,
                                contentColor = CyberCyan
                            ),
                            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = CyberCyan
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (selectedBatchCount == 1) "GÉNÉRER 1 CLÉ UNIQUE" else "GÉNÉRER $selectedBatchCount CLÉS SÉLECTIONNÉES",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                letterSpacing = 0.5.sp
                            )
                        }

                        // Batch created keys display card (50 or more keys)
                        AnimatedVisibility(
                            visible = newlyCreatedBatchKeys.isNotEmpty(),
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(16.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    DarkSurfaceElevated,
                                                    Color(0xFF07212F)
                                                )
                                            )
                                        )
                                        .border(BorderStroke(1.5.dp, CyberNeonGreen), RoundedCornerShape(16.dp))
                                        .padding(16.dp)
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = CyberNeonGreen,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "🎉 ${newlyCreatedBatchKeys.size} CLÉS VIP GÉNÉRÉES",
                                                    color = CyberNeonGreen,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    shareBatchLicenseKeys(
                                                        context = context,
                                                        keys = newlyCreatedBatchKeys,
                                                        durationHours = selectedDurationHours
                                                    )
                                                },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Share,
                                                    contentDescription = "Tout Partager",
                                                    tint = CyberNeonGreen,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // BIG ONE-CLICK COPY ALL KEYS BUTTON
                                        Button(
                                            onClick = {
                                                val allKeysJoined = newlyCreatedBatchKeys.joinToString("\n")
                                                clipboardManager.setText(AnnotatedString(allKeysJoined))
                                                Toast.makeText(
                                                    context,
                                                    "📋 Les ${newlyCreatedBatchKeys.size} clés ont été copiées dans le presse-papier !",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(50.dp)
                                                .testTag("copy_all_batch_keys_button"),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = CyberNeonGreen,
                                                contentColor = Color(0xFF00240E)
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "TOUT COPIER (${newlyCreatedBatchKeys.size} CLÉS EN 1 CLIC)",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 13.sp,
                                                letterSpacing = 0.5.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Text(
                                            text = "Aperçu de la liste (${newlyCreatedBatchKeys.size} clés) :",
                                            color = TextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 200.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFF040C14))
                                                .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                                                .padding(10.dp)
                                                .verticalScroll(rememberScrollState())
                                        ) {
                                            Column {
                                                newlyCreatedBatchKeys.forEachIndexed { idx, k ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 3.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "${idx + 1}. $k",
                                                            color = TextPrimary,
                                                            fontSize = 11.sp,
                                                            fontFamily = FontFamily.Monospace,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Icon(
                                                            imageVector = Icons.Default.ContentCopy,
                                                            contentDescription = "Copier",
                                                            tint = CyberCyan,
                                                            modifier = Modifier
                                                                .size(16.dp)
                                                                .clickable {
                                                                    clipboardManager.setText(AnnotatedString(k))
                                                                    Toast.makeText(context, "Clé #${idx + 1} copiée !", Toast.LENGTH_SHORT).show()
                                                                }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Newly created key display card
                        AnimatedVisibility(
                            visible = newlyCreatedKey != null,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(16.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    DarkSurfaceElevated,
                                                    Color(0xFF0D2533)
                                                )
                                            )
                                        )
                                        .border(BorderStroke(1.5.dp, CyberNeonGreen), RoundedCornerShape(16.dp))
                                        .padding(16.dp)
                                ) {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = CyberNeonGreen,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "CLÉ VIP PRÊTE À L'EMPLOI",
                                                    color = CyberNeonGreen,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }

                                            Row {
                                                IconButton(
                                                    onClick = {
                                                        clipboardManager.setText(AnnotatedString(newlyCreatedKey ?: ""))
                                                        Toast.makeText(context, "Clé copiée !", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.ContentCopy,
                                                        contentDescription = "Copier",
                                                        tint = CyberCyan,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }

                                                IconButton(
                                                    onClick = {
                                                        shareLicenseKey(
                                                            context = context,
                                                            key = newlyCreatedKey ?: "",
                                                            durationHours = selectedDurationHours
                                                        )
                                                    },
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Share,
                                                        contentDescription = "Partager",
                                                        tint = CyberNeonGreen,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Text(
                                            text = newlyCreatedKey ?: "",
                                            color = TextPrimary,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace,
                                            letterSpacing = 1.sp,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )

                                        Text(
                                            text = "Transmettez cette clé à votre ami : elle déverrouille l'application immédiatement.",
                                            color = TextMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // TAB 2: LIVE SESSIONS MONITORING & GEOLOCATION
        if (activeTab == AdminTab.SESSIONS) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TRAÇAGE & SESSIONS EN DIRECT",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Localisation GPS/IP, Pays, Ville et FAI des joueurs",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberNeonGreen.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$onlineSessionsCount EN LIGNE",
                            color = CyberNeonGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            // Live Admin Login Notification Info Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    CyberCyan.copy(alpha = 0.12f),
                                    DarkSurface
                                )
                            )
                        )
                        .border(BorderStroke(1.dp, CyberCyan.copy(alpha = 0.35f)), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CyberCyan.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = CyberCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "ALERTES DE CONNEXION ADMIN",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "Notification automatique reçue à chaque connexion",
                                    color = CyberCyan,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Text(
                            text = "À chaque fois qu'un joueur déverrouille l'application avec une clé, vous recevez une notification système instantanée contenant son modèle de téléphone, son pays et sa ville.",
                            color = TextMuted,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )

                        Button(
                            onClick = {
                                NotificationHelper.postAdminLoginAlert(
                                    context = context,
                                    deviceModel = "Xiaomi Poco X6 Pro 5G",
                                    city = "Paris",
                                    country = "France",
                                    flagEmoji = "🇫🇷",
                                    licenseKey = "ANOS-777-VIP",
                                    ipAddress = "197.104.22.8"
                                )
                                Toast.makeText(context, "🔔 Alerte de connexion test envoyée !", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan.copy(alpha = 0.2f),
                                contentColor = CyberCyan
                            ),
                            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "TESTER L'ALERTE DE CONNEXION (SIMULATION)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            if (activeSessions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkSurface)
                            .border(BorderStroke(1.dp, DarkBorder), RoundedCornerShape(16.dp))
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.WifiTethering,
                                contentDescription = null,
                                tint = CyberCyan.copy(alpha = 0.5f),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Aucun utilisateur externe connecté actuellement",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Les smartphones de vos amis apparaîtront ici automatiquement avec leur pays, ville et IP dès qu'ils lancent Anos v3.",
                                color = TextMuted,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            } else {
                items(activeSessions, key = { it.deviceId }) { session ->
                    ModernConnectedSessionCard(
                        session = session,
                        onDisconnect = { viewModel.disconnectSession(session.deviceId) },
                        onRemove = { viewModel.removeSession(session.deviceId) }
                    )
                }
            }
        }

        // TAB 3: BROADCAST NOTIFICATIONS TO ALL USERS
        if (activeTab == AdminTab.BROADCAST) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "DIFFUSION DE NOTIFICATIONS VIP",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Envoyez des messages push en direct à tous les joueurs",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberGold.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${broadcasts.size} DIFFUSION(S)",
                            color = CyberGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            // Notification Creation Studio
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(DarkSurface)
                        .border(BorderStroke(1.dp, CyberGold.copy(alpha = 0.4f)), RoundedCornerShape(20.dp))
                        .padding(18.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CyberGold.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Campaign,
                                    contentDescription = null,
                                    tint = CyberGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "RÉDIGER UNE NOTIFICATION GLOBALE",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "Le message apparaîtra en haut de l'écran des utilisateurs",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // Category Selector
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "TYPE DE NOTIFICATION :",
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            val categories = listOf(
                                Triple("BOOST", "🔥 Booster & Patch", CyberCrimson),
                                Triple("INFO", "📢 Information", CyberCyan),
                                Triple("VIP", "💎 Exclusif VIP", CyberGold),
                                Triple("ALERT", "⚠️ Alerte Système", Color(0xFFFF9800))
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(categories) { (catKey, label, catColor) ->
                                    val isSelected = broadcastCategoryInput == catKey
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) catColor.copy(alpha = 0.25f) else DarkSurfaceElevated
                                            )
                                            .border(
                                                BorderStroke(
                                                    1.dp,
                                                    if (isSelected) catColor else DarkBorder
                                                ),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { broadcastCategoryInput = catKey }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) catColor else TextMuted,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }

                        // Title field
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "TITRE DE L'ALERTE :",
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            OutlinedTextField(
                                value = broadcastTitleInput,
                                onValueChange = { broadcastTitleInput = it },
                                placeholder = { Text("Ex: 🔥 NOUVEAU BOOSTER 120 FPS", color = TextMuted, fontSize = 12.sp) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = DarkSurfaceElevated,
                                    unfocusedContainerColor = DarkSurfaceElevated,
                                    focusedBorderColor = CyberGold,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                singleLine = true
                            )
                        }

                        // Message body field
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "MESSAGE DE LA NOTIFICATION :",
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            OutlinedTextField(
                                value = broadcastMessageInput,
                                onValueChange = { broadcastMessageInput = it },
                                placeholder = { Text("Écrivez le message que tous les utilisateurs vont lire...", color = TextMuted, fontSize = 12.sp) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(90.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = DarkSurfaceElevated,
                                    unfocusedContainerColor = DarkSurfaceElevated,
                                    focusedBorderColor = CyberGold,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                maxLines = 4
                            )
                        }

                        // Live Notification Preview
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "APERÇU DE LA NOTIFICATION SYSTÈME :",
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(DarkBackground)
                                    .border(BorderStroke(1.dp, DarkBorderGlowing), RoundedCornerShape(14.dp))
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                when (broadcastCategoryInput) {
                                                    "BOOST" -> CyberCrimson.copy(alpha = 0.2f)
                                                    "INFO" -> CyberCyan.copy(alpha = 0.2f)
                                                    "VIP" -> CyberGold.copy(alpha = 0.2f)
                                                    else -> Color(0xFFFF9800).copy(alpha = 0.2f)
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = when (broadcastCategoryInput) {
                                                "BOOST" -> "🔥"
                                                "INFO" -> "📢"
                                                "VIP" -> "💎"
                                                else -> "⚠️"
                                            },
                                            fontSize = 16.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Anos v3 • MAINTENANT",
                                                color = TextMuted,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(CyberGold.copy(alpha = 0.15f))
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = broadcastCategoryInput,
                                                    color = CyberGold,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                        }

                                        Text(
                                            text = broadcastTitleInput.ifBlank { "Titre de la notification" },
                                            color = TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )

                                        Text(
                                            text = broadcastMessageInput.ifBlank { "Corps du message de notification..." },
                                            color = TextSecondary,
                                            fontSize = 11.sp,
                                            lineHeight = 14.sp,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Action button
                        Button(
                            onClick = {
                                if (broadcastTitleInput.isNotBlank() && broadcastMessageInput.isNotBlank()) {
                                    viewModel.sendBroadcastNotification(
                                        title = broadcastTitleInput,
                                        message = broadcastMessageInput,
                                        category = broadcastCategoryInput
                                    )
                                } else {
                                    Toast.makeText(context, "Veuillez entrer un titre et un message.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("send_broadcast_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberGold)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = null,
                                    tint = DarkBackground,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ENVOYER LA NOTIFICATION À TOUS",
                                    color = DarkBackground,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }
            }

            // History of Broadcasts
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HISTORIQUE DES NOTIFICATIONS DIFFUSÉES",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    if (broadcasts.isNotEmpty()) {
                        Text(
                            text = "Tout effacer",
                            color = CyberCrimson,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { viewModel.clearAllBroadcasts() }
                        )
                    }
                }
            }

            if (broadcasts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(DarkSurface)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aucune notification n'a encore été diffusée.",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                items(broadcasts, key = { it.id }) { broadcast ->
                    ModernBroadcastItemCard(
                        broadcast = broadcast,
                        onDelete = { viewModel.deleteBroadcast(broadcast.id) },
                        onResend = {
                            NotificationHelper.postBroadcastNotification(
                                context = context,
                                title = broadcast.title,
                                message = broadcast.message,
                                category = broadcast.category,
                                notificationId = broadcast.id.toInt()
                            )
                            Toast.makeText(context, "Notification rediffusée !", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }

        // TAB 4: LICENSES MANAGEMENT
        if (activeTab == AdminTab.LICENSES) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Rechercher par clé ou pseudo...", color = TextMuted, fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )
            }

            if (filteredLicenses.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkSurface)
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isBlank()) "Aucune clé créée pour le moment. Allez dans 'Générateur VIP'." else "Aucune clé correspondant à '$searchQuery'",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                items(filteredLicenses, key = { it.keyString }) { license ->
                    ModernLicenseCard(
                        license = license,
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(license.keyString))
                            Toast.makeText(context, "Clé ${license.keyString} copiée !", Toast.LENGTH_SHORT).show()
                        },
                        onShare = {
                            shareLicenseKey(
                                context = context,
                                key = license.keyString,
                                durationHours = license.durationHours
                            )
                        },
                        onRevoke = { viewModel.revokeLicenseKey(license.keyString) },
                        onDelete = { viewModel.deleteLicenseKey(license.keyString) }
                    )
                }
            }
        }

        // TAB 4: SECURITY & PASSWORD CHANGE (Zero display of current password)
        if (activeTab == AdminTab.SECURITY) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(DarkSurface)
                        .border(BorderStroke(1.dp, CyberGold.copy(alpha = 0.4f)), RoundedCornerShape(20.dp))
                        .padding(18.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CyberGold.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = CyberGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "MODIFIER LE MOT DE PASSE ADMIN",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "Votre mot de passe reste strictement confidentiel et masqué",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        OutlinedTextField(
                            value = newPasswordInput,
                            onValueChange = { newPasswordInput = it },
                            label = { Text("Nouveau Mot de Passe Secret", color = TextSecondary, fontSize = 11.sp) },
                            placeholder = { Text("Entrez au moins 4 caractères", color = TextMuted, fontSize = 12.sp) },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DarkSurfaceElevated,
                                unfocusedContainerColor = DarkSurfaceElevated,
                                focusedBorderColor = CyberGold,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = confirmPasswordInput,
                            onValueChange = { confirmPasswordInput = it },
                            label = { Text("Confirmer le Nouveau Mot de Passe", color = TextSecondary, fontSize = 11.sp) },
                            placeholder = { Text("Répétez le mot de passe", color = TextMuted, fontSize = 12.sp) },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DarkSurfaceElevated,
                                unfocusedContainerColor = DarkSurfaceElevated,
                                focusedBorderColor = CyberGold,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val p1 = newPasswordInput.trim()
                                val p2 = confirmPasswordInput.trim()
                                when {
                                    p1.length < 4 -> {
                                        Toast.makeText(context, "Le mot de passe doit contenir au moins 4 caractères", Toast.LENGTH_SHORT).show()
                                    }
                                    p1 != p2 -> {
                                        Toast.makeText(context, "Les mots de passe ne correspondent pas", Toast.LENGTH_SHORT).show()
                                    }
                                    else -> {
                                        val ok = viewModel.updateMasterAdminKey(p1)
                                        if (ok) {
                                            newPasswordInput = ""
                                            confirmPasswordInput = ""
                                            Toast.makeText(context, "Nouveau mot de passe admin enregistré avec succès !", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberGold,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "ENREGISTRER LE NOUVEAU MOT DE PASSE",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricStatCard(
    title: String,
    value: String,
    accentColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurface)
            .border(BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = title,
                color = TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ModernConnectedSessionCard(
    session: ActiveSessionEntity,
    onDisconnect: () -> Unit,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .border(
                BorderStroke(
                    1.dp,
                    if (session.isOnline) CyberNeonGreen.copy(alpha = 0.4f) else DarkBorder
                ),
                RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (session.isOnline) CyberNeonGreen.copy(alpha = 0.15f) else DarkSurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = session.flagEmoji.ifBlank { "🌍" },
                            fontSize = 24.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${session.city.ifBlank { "Localisation Inconnue" }}, ${session.country.ifBlank { "Monde" }}",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (session.isOnline) CyberNeonGreen.copy(alpha = 0.2f) else CyberCrimson.copy(alpha = 0.2f)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (session.isOnline) "EN LIGNE" else "DÉCONNECTÉ",
                                    color = if (session.isOnline) CyberNeonGreen else CyberCrimson,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = session.deviceModel,
                            color = CyberCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (session.isOnline) {
                        IconButton(
                            onClick = onDisconnect,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = "Déconnecter",
                                tint = CyberCrimson,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Supprimer",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Geolocation, ISP, and Telemetry chip box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkBackground.copy(alpha = 0.7f))
                    .border(BorderStroke(0.5.dp, DarkBorder), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "📍 Région / FAI",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "${session.region.ifBlank { session.city }} • ${session.isp.ifBlank { "Réseau Mobile / 5G" }}",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "🌐 IP Publique",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                        Text(
                            text = session.ipAddress,
                            color = CyberGold,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "🔑 Clé utilisée",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                        Text(
                            text = session.licenseKey,
                            color = CyberCyan,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "🎮 Activité en cours",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                        Text(
                            text = session.currentActivity,
                            color = CyberNeonGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernBroadcastItemCard(
    broadcast: BroadcastMessageEntity,
    onDelete: () -> Unit,
    onResend: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE) }
    val dateStr = remember(broadcast.timestamp) { dateFormat.format(Date(broadcast.timestamp)) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurface)
            .border(BorderStroke(1.dp, CyberGold.copy(alpha = 0.3f)), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when (broadcast.category) {
                                    "BOOST" -> CyberCrimson.copy(alpha = 0.2f)
                                    "INFO" -> CyberCyan.copy(alpha = 0.2f)
                                    "VIP" -> CyberGold.copy(alpha = 0.2f)
                                    else -> Color(0xFFFF9800).copy(alpha = 0.2f)
                                }
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = broadcast.category,
                            color = when (broadcast.category) {
                                "BOOST" -> CyberCrimson
                                "INFO" -> CyberCyan
                                "VIP" -> CyberGold
                                else -> Color(0xFFFF9800)
                            },
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = dateStr,
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onResend,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Rediffuser",
                            tint = CyberGold,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Supprimer",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Text(
                text = broadcast.title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = broadcast.message,
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun ModernLicenseCard(
    license: LicenseKeyEntity,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onRevoke: () -> Unit,
    onDelete: () -> Unit
) {
    val isPendingActivation = license.keyType == "TEMPORARY" && license.expiresAt == 0L && license.durationHours > 0L
    val isExpired = license.expiresAt > 0L && System.currentTimeMillis() > license.expiresAt
    val remainingMs = if (license.expiresAt > 0L) license.expiresAt - System.currentTimeMillis() else 0L
    val statusText = when {
        !license.isActive -> "RÉVOQUÉE"
        isExpired -> "EXPIRÉE"
        isPendingActivation -> "NON ACTIVÉE (${license.durationHours}h)"
        license.expiresAt == 0L -> "ILLIMITÉE À VIE"
        else -> {
            val h = remainingMs / 3600000
            val m = (remainingMs % 3600000) / 60000
            if (h > 24) "${h / 24}j ${h % 24}h restantes" else if (h > 0) "$h h $m min" else "$m min restantes"
        }
    }

    val statusColor = when {
        !license.isActive || isExpired -> CyberCrimson
        isPendingActivation -> CyberCyan
        else -> CyberNeonGreen
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurface)
            .border(BorderStroke(1.dp, DarkBorder), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = license.keyString,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(statusColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (license.description.isNotBlank()) license.description else "Licence Joueur Anox v2",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copier",
                        tint = CyberCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Partager",
                        tint = CyberNeonGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(onClick = onRevoke, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = "Révoquer",
                        tint = CyberCrimson,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Supprimer",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private fun shareLicenseKey(context: Context, key: String, durationHours: Long) {
    val durationText = when (durationHours) {
        0L -> "Accès Illimité à Vie"
        1L -> "1 Heure"
        24L -> "24 Heures (1 Jour)"
        168L -> "7 Jours"
        720L -> "30 Jours"
        else -> "$durationHours Heures"
    }
    val message = """
        🔥 ANOS V4 GAMING VIP BOOSTER 🔥
        Voici ta clé d'accès exclusive pour activer le Booster Free Fire :

        🔑 Clé : $key

        ⏳ Validité : $durationText
        (Le décompte commence dès que tu entres la clé sur ton téléphone)

        Installe l'application Anos v4, colle cette clé et active l'Ultra Boost 120 FPS & la fluidité des gestes !
    """.trimIndent()

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Clé Anos v4 VIP Booster")
        putExtra(Intent.EXTRA_TEXT, message)
    }
    context.startActivity(Intent.createChooser(intent, "Partager la clé d'accès"))
}

private fun shareBatchLicenseKeys(context: Context, keys: List<String>, durationHours: Long) {
    val durationText = when (durationHours) {
        0L -> "Accès Illimité à Vie"
        1L -> "1 Heure"
        24L -> "24 Heures (1 Jour)"
        168L -> "7 Jours"
        720L -> "30 Jours"
        else -> "$durationHours Heures"
    }
    val keysListFormatted = keys.mapIndexed { index, k -> "${index + 1}. $k" }.joinToString("\n")
    val message = """
        🔥 LOT DE ${keys.size} CLÉS VIP ANOS V4 🔥
        ⏳ Durée par clé : $durationText

        $keysListFormatted

        Installez l'application Anos v4 et entrez votre clé pour activer le Booster 120 FPS & la fluidité des gestes !
    """.trimIndent()

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Lot de ${keys.size} Clés VIP Anos v4")
        putExtra(Intent.EXTRA_TEXT, message)
    }
    context.startActivity(Intent.createChooser(intent, "Partager les ${keys.size} clés VIP"))
}
