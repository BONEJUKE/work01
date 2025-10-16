package com.example.calendar.ui

import android.content.Context
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.waitUntil
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.calendar.MainActivity
import com.example.calendar.data.CalendarDatabase
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuickAddFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun clearDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(CalendarDatabase.DATABASE_NAME)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun quickAddTask_createsTaskAndShowsInAgenda() {
        composeRule.onNodeWithContentDescription("새 일정 또는 할 일 추가").performClick()

        composeRule.onNodeWithText("빠른 추가").assertIsDisplayed()

        composeRule.onNodeWithTag("quickAddTaskTitle", useUnmergedTree = true)
            .performTextInput("회고 준비")
        composeRule.onNodeWithTag("quickAddTaskNotes", useUnmergedTree = true)
            .performTextInput("안건 정리")
        composeRule.onNodeWithTag("quickAddTaskDueTime", useUnmergedTree = true)
            .performTextClearance()
        composeRule.onNodeWithTag("quickAddTaskDueTime", useUnmergedTree = true)
            .performTextInput("11:30")

        composeRule.onNodeWithText("저장").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("빠른 추가").fetchSemanticsNodes().isEmpty()
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("회고 준비").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("회고 준비").assertIsDisplayed()
        composeRule.onNodeWithText("할 일을 추가했어요.").assertIsDisplayed()
    }
}
