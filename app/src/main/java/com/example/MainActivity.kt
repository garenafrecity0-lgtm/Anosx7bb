package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.components.BoostDialog
import com.example.ui.screens.AdminPanelScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.GamesScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberGold
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextMuted
import com.example.ui.viewmodel.BoosterViewModel

enum class NavigationTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    DASHBOARD("Booster", Icons.Filled.RocketLaunch, Icons.Outlined.RocketLaunch, "tab_dashboard"),
    GAMES("Jeux", Icons.Filled.SportsEsports, Icons.Outlined.SportsEsports, "tab_games"),
    TOOLS("Outils", Icons.Filled.Tune, Icons.Outlined.Tune, "tab_tools"),
    STATS("Stats", Icons.Filled.Assessment, Icons.Outlined.Assessment, "tab_stats"),
    ADMIN("Admin", Icons.Filled.AdminPanelSettings, Icons.Outlined.AdminPanelSettings, "tab_admin")
}

class MainActivity : ComponentActivity() {
    private val viewModel: BoosterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        viewModel.unlock120HzAndFluidity(window)
        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                val notificationLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { _ -> }
                )

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                val authStatus by viewModel.authStatus.collectAsState()

                if (!authStatus.isAuthenticated) {
                    AuthScreen(viewModel = viewModel)
                } else {
                    MainAppScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: BoosterViewModel) {
    val authStatus by viewModel.authStatus.collectAsState()
    var selectedTab by rememberSaveable { mutableStateOf(NavigationTab.DASHBOARD) }
    val isBoosting by viewModel.isBoosting.collectAsState()
    val isBoostComplete by viewModel.isBoostComplete.collectAsState()
    val boostStep by viewModel.boostStep.collectAsState()
    val boostStepLabel by viewModel.boostStepLabel.collectAsState()
    val lastFreedMb by viewModel.lastFreedMb.collectAsState()
    val lastOldPercent by viewModel.lastOldPercent.collectAsState()
    val lastNewPercent by viewModel.lastNewPercent.collectAsState()
    val selectedMode by viewModel.selectedMode.collectAsState()

    val haptic = LocalHapticFeedback.current

    LaunchedEffect(isBoostComplete) {
        if (isBoostComplete) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val visibleTabs = if (authStatus.isAdmin) {
        NavigationTab.entries
    } else {
        NavigationTab.entries.filter { it != NavigationTab.ADMIN }
    }

    // Safety if user was on Admin tab and logged out or role changed
    if (!authStatus.isAdmin && selectedTab == NavigationTab.ADMIN) {
        selectedTab = NavigationTab.DASHBOARD
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        containerColor = DarkBackground,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                NavigationBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(DarkSurface)
                        .border(1.dp, DarkBorder, RoundedCornerShape(20.dp)),
                    containerColor = DarkSurface,
                    tonalElevation = 0.dp
                ) {
                    visibleTabs.forEach { tab ->
                        val isSelected = selectedTab == tab
                        val isTabAdmin = tab == NavigationTab.ADMIN
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { selectedTab = tab },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = if (isTabAdmin) CyberGold else CyberCyan,
                                selectedTextColor = if (isTabAdmin) CyberGold else CyberCyan,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted,
                                indicatorColor = (if (isTabAdmin) CyberGold else CyberCyan).copy(alpha = 0.12f)
                            ),
                            modifier = Modifier.testTag(tab.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(targetState = selectedTab, label = "screenTransition") { tab ->
                when (tab) {
                    NavigationTab.DASHBOARD -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToGames = { selectedTab = NavigationTab.GAMES },
                        onOpenAdmin = { selectedTab = NavigationTab.ADMIN }
                    )
                    NavigationTab.GAMES -> GamesScreen(viewModel = viewModel)
                    NavigationTab.TOOLS -> ToolsScreen(viewModel = viewModel)
                    NavigationTab.STATS -> StatsScreen(viewModel = viewModel)
                    NavigationTab.ADMIN -> AdminPanelScreen(viewModel = viewModel)
                }
            }

            // Global Boost sci-fi dialog
            if (isBoosting || isBoostComplete) {
                BoostDialog(
                    isBoosting = isBoosting,
                    isComplete = isBoostComplete,
                    currentStep = boostStep,
                    currentStepLabel = boostStepLabel,
                    freedMb = lastFreedMb,
                    oldPercent = lastOldPercent,
                    newPercent = lastNewPercent,
                    modeName = selectedMode.title,
                    onDismiss = { viewModel.dismissBoostDialog() }
                )
            }
        }
    }
}
