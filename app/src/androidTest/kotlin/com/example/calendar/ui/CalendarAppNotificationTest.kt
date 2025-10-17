package com.example.calendar.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.calendar.MainActivity
import com.example.calendar.ui.theme.CalendarTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CalendarAppNotificationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun notificationCard_showsRequestCtaWhenPermissionCanBeRequested() {
        val controller = NotificationPermissionController(
            isSupported = true,
            isGranted = false,
            canRequest = true,
            requestPermission = {},
            openSettings = {}
        )

        composeRule.setContent {
            CalendarTheme {
                NotificationPermissionCard(
                    controller = controller,
                    onDismiss = {},
                    onRequestPermission = {},
                    onOpenSettings = {}
                )
            }
        }

        composeRule.onNodeWithText("알림 권한을 허용해 주세요").assertIsDisplayed()
        composeRule.onNodeWithText("알림 허용").assertIsDisplayed()
    }

    @Test
    fun notificationCard_showsSettingsCtaWhenRequestDisabled() {
        val controller = NotificationPermissionController(
            isSupported = true,
            isGranted = false,
            canRequest = false,
            requestPermission = {},
            openSettings = {}
        )

        composeRule.setContent {
            CalendarTheme {
                NotificationPermissionCard(
                    controller = controller,
                    onDismiss = {},
                    onRequestPermission = {},
                    onOpenSettings = {}
                )
            }
        }

        composeRule.onNodeWithText("앱 설정에서 알림을 허용하면 일정 전에 리마인더를 받을 수 있어요.").assertIsDisplayed()
        composeRule.onNodeWithText("설정 열기").assertIsDisplayed()
    }
}
