package com.example.alarm_clock_2.ui

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Provide WindowSizeClass across app so composables can adapt to screen width categories.
 */
val LocalWindowSizeClass = staticCompositionLocalOf<WindowSizeClass> {
    error("No WindowSizeClass provided")
} 