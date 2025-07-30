package com.example.alarm_clock_2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.alarm_clock_2.ui.SettingsScreen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.alarm_clock_2.ui.AlarmsScreen
import com.example.alarm_clock_2.ui.CalendarScreen
import dagger.hilt.android.AndroidEntryPoint
import com.example.alarm_clock_2.util.ExactAlarmPermission
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.size
import androidx.compose.ui.platform.LocalUriHandler
import androidx.navigation.compose.navigation
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Density
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import com.example.alarm_clock_2.ui.LocalWindowSizeClass
import com.example.alarm_clock_2.update.*
import com.example.alarm_clock_2.worker.UpdateCheckWorker
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Calendar : Screen("calendar", "日历", Icons.Default.DateRange)
    object Alarms : Screen("alarms", "闹钟", Icons.Default.Alarm)
    object Settings : Screen("settings", "设置", Icons.Default.Settings)
}

// 单一 NavGraph 的字符串常量，便于多处复用
private const val MAIN_GRAPH_ROUTE = "main_graph"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ensure precise-alarm permission on Android 12+
        ExactAlarmPermission.request(this)
        // Request notification runtime permission on Android 13+
        com.example.alarm_clock_2.util.NotificationPermission.request(this)
        // Reschedule alarms on app startup
        com.example.alarm_clock_2.worker.RescheduleAlarmsWorker.enqueue(this)
        // Start periodic update checking
        UpdateCheckWorker.enqueue(this)
        setContent {
            // 计算窗口尺寸等级并注入 CompositionLocal，供各屏幕自适应使用
            val windowSizeClass = calculateWindowSizeClass(this)
            CompositionLocalProvider(LocalWindowSizeClass provides windowSizeClass) {
                AppTheme {
                    val navController = rememberNavController()
                    val items = listOf(
                        Screen.Calendar,
                        Screen.Alarms,
                        Screen.Settings
                    )

                    // Update system integration
                    val updateViewModel: UpdateViewModel = hiltViewModel()
                    val updateState by updateViewModel.updateState.collectAsState()
                    val showUpdateDialog by updateViewModel.showUpdateDialog.collectAsState()
                    val showDownloadDialog by updateViewModel.showDownloadDialog.collectAsState()
                    val showInstallDialog by updateViewModel.showInstallDialog.collectAsState()

                    // Check for updates on app startup
                    LaunchedEffect(Unit) {
                        val currentVersion = UpdateUtils.getCurrentVersion(this@MainActivity)
                        updateViewModel.checkForUpdates(currentVersion)
                    }
                    Scaffold(
                        bottomBar = {
                            // iOS风格的底部导航栏
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                tonalElevation = 8.dp
                            ) {
                                val navBackStackEntry by navController.currentBackStackEntryAsState()
                                val currentRoute = navBackStackEntry?.destination?.route
                                items.forEach { screen ->
                                    NavigationBarItem(
                                        selected = currentRoute == screen.route,
                                        onClick = {
                                            if (currentRoute != screen.route) {
                                                navController.navigate(screen.route) {
                                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                screen.icon,
                                                contentDescription = null,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        },
                                        label = {
                                            Text(
                                                screen.label,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 12.sp
                                                )
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                        )
                                    )
                                }
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.background
                    ) { innerPadding ->
                        // Developer info dialog moved to Settings screen
                        Surface(modifier = Modifier.padding(innerPadding), color = MaterialTheme.colorScheme.background) {
                            NavHost(
                                navController = navController,
                                startDestination = MAIN_GRAPH_ROUTE
                            ) {
                                navigation(
                                    startDestination = Screen.Calendar.route,
                                    route = MAIN_GRAPH_ROUTE
                                ) {
                                    composable(Screen.Calendar.route) { backStackEntry ->
                                        // 使用图级别的 ViewModelStoreOwner，确保 CalendarViewModel 长驻
                                        val parentEntry = remember(backStackEntry) {
                                            navController.getBackStackEntry(MAIN_GRAPH_ROUTE)
                                        }
                                        val viewModel: com.example.alarm_clock_2.ui.CalendarViewModel =
                                            androidx.hilt.navigation.compose.hiltViewModel(parentEntry)
                                        CalendarScreen(viewModel)
                                    }

                                    composable(Screen.Alarms.route) { AlarmsScreen() }
                                    composable(Screen.Settings.route) { SettingsScreen() }
                                }
                            }
                        }
                    }

                    // Update dialogs
                    if (showUpdateDialog) {
                        when (val state = updateState) {
                            is UpdateState.UpdateAvailable -> {
                                UpdateAvailableBottomSheet(
                                    updateInfo = state.updateInfo,
                                    onDismiss = { updateViewModel.dismissUpdateDialog() },
                                    onDownload = { updateViewModel.startDownload(state.updateInfo) },
                                    onPostpone = { updateViewModel.postponeUpdate(state.updateInfo.version) }
                                )
                            }
                            else -> { /* No dialog needed */ }
                        }
                    }

                    if (showDownloadDialog) {
                        when (val state = updateState) {
                            is UpdateState.Downloading -> {
                                DownloadProgressBottomSheet(
                                    progress = state.progress,
                                    onCancel = { updateViewModel.cancelDownload() },
                                    onDismiss = { updateViewModel.dismissDownloadDialog() }
                                )
                            }
                            else -> { /* No dialog needed */ }
                        }
                    }

                    if (showInstallDialog) {
                        when (val state = updateState) {
                            is UpdateState.DownloadComplete -> {
                                InstallPromptBottomSheet(
                                    filePath = state.filePath,
                                    onInstall = {
                                        try {
                                            UpdateUtils.installApk(this@MainActivity, state.filePath)
                                            updateViewModel.dismissInstallDialog()
                                        } catch (e: Exception) {
                                            // Handle installation error
                                            updateViewModel.dismissInstallDialog()
                                        }
                                    },
                                    onLater = { updateViewModel.dismissInstallDialog() },
                                    onDismiss = { updateViewModel.dismissInstallDialog() }
                                )
                            }
                            else -> { /* No dialog needed */ }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppTheme(
    minFontScale: Float = 1f,
    maxFontScale: Float = 1.2f,
    content: @Composable () -> Unit
) {
    val current = LocalDensity.current
    // 将系统 fontScale 限制在给定区间内
    val cappedDensity = remember(current, minFontScale, maxFontScale) {
        val scale = current.fontScale.coerceIn(minFontScale, maxFontScale)
        Density(current.density, fontScale = scale)
    }

    CompositionLocalProvider(LocalDensity provides cappedDensity) {
        MaterialTheme(content = content)
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    AppTheme {
        Text("Hello")
    }
} 