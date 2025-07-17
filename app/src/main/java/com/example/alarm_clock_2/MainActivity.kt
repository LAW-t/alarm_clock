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
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalUriHandler

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Calendar : Screen("calendar", "日历", Icons.Default.DateRange)
    object Alarms : Screen("alarms", "闹钟", Icons.Default.Alarm)
    object Settings : Screen("settings", "设置", Icons.Default.Settings)
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ensure precise-alarm permission on Android 12+
        ExactAlarmPermission.request(this)
        setContent {
            AppTheme {
                val navController = rememberNavController()
                val items = listOf(
                    Screen.Calendar,
                    Screen.Alarms,
                    Screen.Settings
                )
                Scaffold(
                    bottomBar = {
                        NavigationBar {
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
                                    icon = { Icon(screen.icon, contentDescription = null) },
                                    label = { Text(screen.label) }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    // Developer info dialog moved to Settings screen
                    Surface(modifier = Modifier.padding(innerPadding), color = MaterialTheme.colorScheme.background) {
                        NavHost(navController, startDestination = Screen.Calendar.route) {
                            composable(Screen.Calendar.route) { CalendarScreen() }
                            composable(Screen.Alarms.route) { AlarmsScreen() }
                            composable(Screen.Settings.route) { SettingsScreen() }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        content = content
    )
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    AppTheme {
        Text("Hello")
    }
} 