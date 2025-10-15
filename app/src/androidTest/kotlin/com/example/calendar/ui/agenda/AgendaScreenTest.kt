package com.example.calendar.ui.agenda

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.calendar.data.AgendaPeriod
import com.example.calendar.ui.AgendaUiState
import com.example.calendar.ui.theme.CalendarTheme
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AgendaScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun agendaScreen_showsEmptyStateWhenSnapshotMissing() {
        composeTestRule.setContent {
            CalendarTheme {
                AgendaScreen(
                    uiState = AgendaUiState(
                        snapshot = null,
                        isLoading = false,
                        error = null,
                        userMessage = null
                    ),
                    selectedTab = AgendaTab.Daily,
                    onTabSelected = {},
                    onPreviousPeriod = {},
                    onNextPeriod = {},
                    onToggleTask = {},
                    onTaskClick = {},
                    onEventClick = {},
                    onWeekDaySelected = {},
                    onMonthDaySelected = {},
                    focusedDay = TestDate,
                    period = AgendaPeriod.Day(TestDate),
                    onQuickAddClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("No agenda items available").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("새 일정 또는 할 일 추가").assertIsDisplayed()
    }

    @Test
    fun agendaScreen_showsSnapshotContent() {
        val previewState = previewAgendaUiState()
        val previewDate = previewState.snapshot!!.rangeStart

        composeTestRule.setContent {
            CalendarTheme {
                AgendaScreen(
                    uiState = previewState,
                    selectedTab = AgendaTab.Daily,
                    onTabSelected = {},
                    onPreviousPeriod = {},
                    onNextPeriod = {},
                    onToggleTask = {},
                    onTaskClick = {},
                    onEventClick = {},
                    onWeekDaySelected = {},
                    onMonthDaySelected = {},
                    focusedDay = previewDate,
                    period = AgendaPeriod.Day(previewDate),
                    onQuickAddClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Daily overview").assertIsDisplayed()
        composeTestRule.onNodeWithText("디자인 시안 검토").assertIsDisplayed()
        composeTestRule.onNodeWithText("팀 스탠드업").assertIsDisplayed()
    }

    private companion object {
        val TestDate: LocalDate = LocalDate.of(2024, 5, 21)
    }
}
