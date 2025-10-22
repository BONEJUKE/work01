package com.example.calendar.ui.agenda

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.calendar.data.AgendaPeriod
import com.example.calendar.data.TaskStatus
import com.example.calendar.ui.AgendaUiState
import com.example.calendar.ui.AgendaUserMessage
import com.example.calendar.ui.QuickAddType
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

    @Test
    fun agendaScreen_talkBackFocusAndDismiss_updatesAccessibilityState() {
        val initialState = previewAgendaUiState().copy(
            userMessage = AgendaUserMessage.QuickAddSuccess(QuickAddType.Task)
        )
        val initialSnapshot = requireNotNull(initialState.snapshot)
        val firstTask = initialSnapshot.tasks.first()
        val initialStatusLabel = when (firstTask.status) {
            TaskStatus.Pending -> AgendaText.Agenda.statusPending
            TaskStatus.InProgress -> AgendaText.Agenda.statusInProgress
            TaskStatus.Completed -> AgendaText.Agenda.statusCompleted
        }
        val firstTaskInitialDescription = AgendaText.Agenda.accessibleTask(
            initialStatusLabel,
            firstTask.title
        )
        val firstTaskCompletedDescription = AgendaText.Agenda.accessibleTask(
            AgendaText.Agenda.statusCompleted,
            firstTask.title
        )

        composeTestRule.setContent {
            var state by remember { mutableStateOf(initialState) }

            CalendarTheme {
                AgendaScreen(
                    uiState = state,
                    selectedTab = AgendaTab.Daily,
                    onTabSelected = {},
                    onPreviousPeriod = {},
                    onNextPeriod = {},
                    onToggleTask = { task ->
                        state = state.copy(
                            snapshot = state.snapshot?.let { snapshot ->
                                val updatedTask = task.toggleCompletion()
                                snapshot.copy(
                                    tasks = snapshot.tasks.map { existing ->
                                        if (existing.id == task.id) updatedTask else existing
                                    }
                                )
                            }
                        )
                    },
                    onTaskClick = {},
                    onEventClick = {},
                    onCycleCompletedTaskFilter = {
                        state = state.copy(
                            filters = state.filters.copy(
                                completedTaskFilter = state.filters.completedTaskFilter.next()
                            )
                        )
                    },
                    onToggleShowRecurringEvents = {},
                    onWeekDaySelected = {},
                    onMonthDaySelected = {},
                    focusedDay = initialSnapshot.rangeStart,
                    period = AgendaPeriod.Day(initialSnapshot.rangeStart),
                    onQuickAddClick = {},
                    onUserMessageShown = {
                        state = state.copy(userMessage = null)
                    }
                )
            }
        }

        val fabDescription = AgendaText.Agenda.fabDescription
        val fabNode = composeTestRule.onNodeWithContentDescription(fabDescription)
        fabNode.performSemanticsAction(SemanticsActions.RequestFocus)
        fabNode.assertIsFocused()

        val taskNode = composeTestRule.onNodeWithContentDescription(firstTaskInitialDescription)
        taskNode.assertContentDescriptionEquals(firstTaskInitialDescription)
        taskNode.performSemanticsAction(SemanticsActions.Dismiss)

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithContentDescription(firstTaskCompletedDescription)
            .assertIsDisplayed()

        composeTestRule.onNode(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                AgendaText.Agenda.statusCompletedDesc
            ).and(hasParent(hasContentDescription(firstTaskCompletedDescription)))
        ).assertExists()

        composeTestRule.onNodeWithText(AgendaText.Agenda.filterAllTasks)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    AgendaText.Agenda.filterAllTasksDescription
                )
            )
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(AgendaText.Agenda.filterHideCompleted)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    AgendaText.Agenda.filterHideCompletedDescription
                )
            )

        composeTestRule.onNodeWithText(AgendaText.QuickAddResult.taskSuccess)
            .assertIsDisplayed()
    }

    private companion object {
        val TestDate: LocalDate = LocalDate.of(2024, 5, 21)
    }
}
