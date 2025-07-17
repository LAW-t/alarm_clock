package com.example.alarm_clock_2.ui

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.alarm_clock_2.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlarmsScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun addAlarm_flow_updatesList() {
        // Navigate to Alarms tab (label "闹钟")
        composeTestRule.onNode(hasText("闹钟") and hasClickAction())
            .performClick()

        // Ensure list initially empty (optional)
        // Click FAB to open dialog
        composeTestRule.onNodeWithContentDescription("添加闹钟")
            .performClick()

        // In dialog, select first shift chip (默认 DAY) 不用操作
        // Confirm
        composeTestRule.onNodeWithText("确定")
            .performClick()

        // Expect at least one alarm item displayed (time text exists)
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithContentDescription("删除").fetchSemanticsNodes().isNotEmpty()
        }
    }
} 