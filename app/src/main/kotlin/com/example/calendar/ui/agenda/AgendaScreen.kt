package com.example.calendar.ui.agenda

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.FilterChip
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.calendar.data.AgendaPeriod
import com.example.calendar.data.CalendarEvent
import com.example.calendar.data.Recurrence
import com.example.calendar.data.RecurrenceRule
import com.example.calendar.data.Task
import com.example.calendar.data.TaskStatus
import com.example.calendar.scheduler.AgendaSnapshot
import com.example.calendar.ui.AgendaFilters
import com.example.calendar.ui.AgendaUiState
import com.example.calendar.ui.AgendaUserMessage
import com.example.calendar.ui.AgendaViewModel
import com.example.calendar.ui.CompletedTaskFilter
import com.example.calendar.ui.QuickAddType
import com.example.calendar.ui.theme.CalendarTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch
import kotlin.math.min

private val DayFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일 (E)", Locale.KOREAN)
private val WeekFormatter = DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN)
private val MonthFormatter = DateTimeFormatter.ofPattern("yyyy년 M월", Locale.KOREAN)
private val WeekdayChipFormatter = DateTimeFormatter.ofPattern("E d일", Locale.KOREAN)
private val DateTimeDetailFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일 a h시 mm분", Locale.KOREAN)
private val TimeOnlyFormatter = DateTimeFormatter.ofPattern("a h시 mm분", Locale.KOREAN)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaRoute(
    viewModel: AgendaViewModel,
    onEventClick: (CalendarEvent) -> Unit = {},
    onTaskClick: (Task) -> Unit = {},
    onEventEdit: (CalendarEvent) -> Unit = {},
    onTaskEdit: (Task) -> Unit = {},
    notificationPermissionCard: (@Composable () -> Unit)? = null
) {
    val uiState by viewModel.state.collectAsState()

    val navigationState = rememberAgendaNavigationState()
    val selectedTab = navigationState.currentTab
    var selectedDayIso by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var selectedWeekIso by rememberSaveable { mutableStateOf(LocalDate.now().startOfWeek().toString()) }
    var selectedMonthIso by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }

    val focusedDay = remember(selectedDayIso) { LocalDate.parse(selectedDayIso) }

    fun updateFocus(date: LocalDate) {
        selectedDayIso = date.toString()
        selectedWeekIso = date.startOfWeek().toString()
        selectedMonthIso = YearMonth.of(date.year, date.monthValue).toString()
    }

    val currentPeriod = remember(selectedTab, selectedDayIso, selectedWeekIso, selectedMonthIso) {
        when (selectedTab) {
            AgendaTab.Daily -> AgendaPeriod.Day(focusedDay)
            AgendaTab.Weekly -> AgendaPeriod.Week(LocalDate.parse(selectedWeekIso))
            AgendaTab.Monthly -> YearMonth.parse(selectedMonthIso).let { month ->
                AgendaPeriod.Month(month.year, month.monthValue)
            }
        }
    }

    LaunchedEffect(currentPeriod) {
        viewModel.setPeriod(currentPeriod)
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    var sheetContent by remember { mutableStateOf<AgendaSheetContent?>(null) }

    val openTaskSheet: (Task) -> Unit = { task ->
        sheetContent = AgendaSheetContent.TaskDetail(task)
        onTaskClick(task)
    }
    val openEventSheet: (CalendarEvent) -> Unit = { event ->
        sheetContent = AgendaSheetContent.EventDetail(event)
        onEventClick(event)
    }

    val hideSheet: () -> Unit = {
        coroutineScope.launch {
            try {
                sheetState.hide()
            } finally {
                sheetContent = null
            }
        }
    }

    LaunchedEffect(sheetContent) {
        if (sheetContent != null) {
            sheetState.show()
        }
    }

    LaunchedEffect(uiState.snapshot, sheetContent) {
        val snapshot = uiState.snapshot
        val currentContent = sheetContent
        if (snapshot != null && currentContent != null) {
            when (currentContent) {
                is AgendaSheetContent.TaskDetail -> {
                    val updated = snapshot.tasks.find { it.id == currentContent.task.id }
                    if (updated != null && updated != currentContent.task) {
                        sheetContent = AgendaSheetContent.TaskDetail(updated)
                    } else if (updated == null) {
                        hideSheet()
                    }
                }
                is AgendaSheetContent.EventDetail -> {
                    val updated = snapshot.events.find {
                        it.id == currentContent.event.id && it.start == currentContent.event.start
                    } ?: snapshot.events.find { it.id == currentContent.event.id }
                    if (updated != null && updated != currentContent.event) {
                        sheetContent = AgendaSheetContent.EventDetail(updated)
                    } else if (updated == null) {
                        hideSheet()
                    }
                }
                is AgendaSheetContent.QuickAdd -> Unit
            }
        }
    }

    AgendaScreen(
        uiState = uiState,
        selectedTab = selectedTab,
        onTabSelected = { tab ->
            navigationState.navigateTo(tab)
            updateFocus(focusedDay)
        },
        onPreviousPeriod = {
            when (selectedTab) {
                AgendaTab.Daily -> updateFocus(focusedDay.minusDays(1))
                AgendaTab.Weekly -> updateFocus(focusedDay.minusWeeks(1))
                AgendaTab.Monthly -> {
                    val currentMonth = YearMonth.parse(selectedMonthIso)
                    val previousMonth = currentMonth.minusMonths(1)
                    val day = min(focusedDay.dayOfMonth, previousMonth.lengthOfMonth())
                    updateFocus(previousMonth.atDay(day))
                }
            }
        },
        onNextPeriod = {
            when (selectedTab) {
                AgendaTab.Daily -> updateFocus(focusedDay.plusDays(1))
                AgendaTab.Weekly -> updateFocus(focusedDay.plusWeeks(1))
                AgendaTab.Monthly -> {
                    val currentMonth = YearMonth.parse(selectedMonthIso)
                    val nextMonth = currentMonth.plusMonths(1)
                    val day = min(focusedDay.dayOfMonth, nextMonth.lengthOfMonth())
                    updateFocus(nextMonth.atDay(day))
                }
            }
        },
        onToggleTask = viewModel::toggleTask,
        onTaskClick = openTaskSheet,
        onEventClick = openEventSheet,
        onCycleCompletedTaskFilter = viewModel::cycleCompletedTaskFilter,
        onToggleShowRecurringEvents = viewModel::toggleShowRecurringEvents,
        onWeekDaySelected = { date ->
            updateFocus(date)
            navigationState.navigateTo(AgendaTab.Daily)
        },
        onMonthDaySelected = { date ->
            updateFocus(date)
            navigationState.navigateTo(AgendaTab.Daily)
        },
        focusedDay = focusedDay,
        period = currentPeriod,
        onQuickAddClick = {
            sheetContent = AgendaSheetContent.QuickAdd(
                initialType = QuickAddType.Task,
                focusDate = focusedDay,
                period = currentPeriod
            )
        },
        onUserMessageShown = viewModel::clearUserMessage,
        notificationPrompt = notificationPermissionCard
    )

    sheetContent?.let { content ->
        ModalBottomSheet(
            onDismissRequest = hideSheet,
            sheetState = sheetState
        ) {
            when (content) {
                is AgendaSheetContent.TaskDetail -> TaskDetailSheet(
                    task = content.task,
                    onToggleTask = { task ->
                        viewModel.toggleTask(task)
                        sheetContent = AgendaSheetContent.TaskDetail(task.toggleCompletion())
                    },
                    onDeleteTask = {
                        viewModel.deleteTask(it)
                        hideSheet()
                    },
                    onEditTask = {
                        onTaskEdit(it)
                        hideSheet()
                    },
                    onClose = hideSheet
                )
                is AgendaSheetContent.EventDetail -> EventDetailSheet(
                    event = content.event,
                    onDeleteEvent = {
                        viewModel.deleteEvent(it)
                        hideSheet()
                    },
                    onEditEvent = {
                        onEventEdit(it)
                        hideSheet()
                    },
                    onClose = hideSheet
                )
                is AgendaSheetContent.QuickAdd -> QuickAddSheet(
                    content = content,
                    onCreateTask = { input ->
                        viewModel
                            .quickAddTask(
                                title = input.title,
                                focusDate = content.focusDate,
                                period = content.period,
                                description = input.notes,
                                dueTime = input.dueTime
                            )
                            .map {}
                    },
                    onCreateEvent = { input ->
                        viewModel
                            .quickAddEvent(
                                title = input.title,
                                focusDate = content.focusDate,
                                period = content.period,
                                description = input.notes,
                                location = input.location,
                                startTime = input.startTime,
                                endTime = input.endTime
                            )
                            .map {}
                    },
                    onDismiss = hideSheet
                )
            }
        }
    }

enum class AgendaTab {
    Daily,
    Weekly,
    Monthly
}

class AgendaNavigationState internal constructor(
    private val selectedTabState: MutableState<AgendaTab>
) {
    val currentTab: AgendaTab
        get() = selectedTabState.value

    fun navigateTo(tab: AgendaTab) {
        selectedTabState.value = tab
    }
}

@Composable
fun rememberAgendaNavigationState(
    initialTab: AgendaTab = AgendaTab.Daily
): AgendaNavigationState {
    val selectedTabState = rememberSaveable { mutableStateOf(initialTab) }
    return remember(selectedTabState) { AgendaNavigationState(selectedTabState) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaScreen(
    uiState: AgendaUiState,
    selectedTab: AgendaTab,
    onTabSelected: (AgendaTab) -> Unit,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onToggleTask: (Task) -> Unit,
    onTaskClick: (Task) -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
    onCycleCompletedTaskFilter: () -> Unit,
    onToggleShowRecurringEvents: () -> Unit,
    onWeekDaySelected: (LocalDate) -> Unit,
    onMonthDaySelected: (LocalDate) -> Unit,
    focusedDay: LocalDate,
    period: AgendaPeriod,
    onQuickAddClick: () -> Unit = {},
    onUserMessageShown: () -> Unit = {},
    notificationPrompt: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message.toSnackbarMessage(),
                withDismissAction = true,
                duration = SnackbarDuration.Short
            )
            onUserMessageShown()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            AgendaFab(onClick = onQuickAddClick)
        },
        topBar = {
            AgendaTopBar(
                period = period,
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                onPreviousPeriod = onPreviousPeriod,
                onNextPeriod = onNextPeriod
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            AgendaPeriodSelector(
                selectedTab = selectedTab,
                period = period,
                focusedDay = focusedDay,
                onWeekDaySelected = onWeekDaySelected,
                onMonthDaySelected = onMonthDaySelected
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true)
            ) {
                when {
                    uiState.isLoading -> AgendaLoading()
                    uiState.error != null -> AgendaError(uiState.error)
                    uiState.snapshot != null -> AgendaSnapshotContent(
                        snapshot = uiState.snapshot,
                        filters = uiState.filters,
                        onToggleTask = onToggleTask,
                        onTaskClick = onTaskClick,
                        onEventClick = onEventClick,
                        onCycleCompletedTaskFilter = onCycleCompletedTaskFilter,
                        onToggleShowRecurringEvents = onToggleShowRecurringEvents,
                        selectedTab = selectedTab,
                        modifier = Modifier.fillMaxSize()
                    )
                    else -> AgendaEmptyState(onQuickAddClick = onQuickAddClick)
                }
            }
            if (notificationPrompt != null) {
                Spacer(modifier = Modifier.height(16.dp))
                notificationPrompt()
            }
        }
    )
}

@Composable
private fun AgendaFab(onClick: () -> Unit) {
    FloatingActionButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "새 일정 또는 할 일 추가"
        )
    }
}

private fun periodSummary(period: AgendaPeriod): String = when (period) {
    is AgendaPeriod.Day -> "하루"
    is AgendaPeriod.Week -> "한 주"
    is AgendaPeriod.Month -> "한 달"
}

@Composable
private fun AgendaTopBar(
    period: AgendaPeriod,
    selectedTab: AgendaTab,
    onTabSelected: (AgendaTab) -> Unit,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TopAppBar(
            title = {
                Text(
                    text = periodLabel(period),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            actions = {
                PeriodControls(
                    onPreviousPeriod = onPreviousPeriod,
                    onNextPeriod = onNextPeriod
                )
            }
        )
        AgendaTabRow(
            selectedTab = selectedTab,
            onTabSelected = onTabSelected
        )
    }
}

@Composable
private fun AgendaTabRow(
    selectedTab: AgendaTab,
    onTabSelected: (AgendaTab) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        AgendaTab.values().forEach { tab ->
            val isSelected = tab == selectedTab
            TextButton(
                onClick = { onTabSelected(tab) },
                modifier = Modifier.semantics {
                    contentDescription = tab.accessibleLabel()
                    role = Role.Tab
                    stateDescription = if (isSelected) "선택됨" else "선택되지 않음"
                }
            ) {
                Text(
                    text = tab.displayLabel(),
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    HorizontalDivider()
}

@Composable
private fun AgendaPeriodSelector(
    selectedTab: AgendaTab,
    period: AgendaPeriod,
    focusedDay: LocalDate,
    onWeekDaySelected: (LocalDate) -> Unit,
    onMonthDaySelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    when (selectedTab) {
        AgendaTab.Weekly -> {
            val week = period as? AgendaPeriod.Week ?: return
            WeekdaySelector(
                weekStart = week.start,
                focusedDay = focusedDay,
                onDaySelected = onWeekDaySelected,
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        AgendaTab.Monthly -> {
            val month = period as? AgendaPeriod.Month ?: return
            MonthDayGrid(
                month = YearMonth.of(month.year, month.month),
                focusedDay = focusedDay,
                onDaySelected = onMonthDaySelected,
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        AgendaTab.Daily -> Unit
    }
}

@Composable
private fun WeekdaySelector(
    weekStart: LocalDate,
    focusedDay: LocalDate,
    onDaySelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(7) { offset ->
            val date = weekStart.plusDays(offset.toLong())
            FilterChip(
                selected = date == focusedDay,
                onClick = { onDaySelected(date) },
                label = {
                    Text(
                        text = date.format(WeekdayChipFormatter),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MonthDayGrid(
    month: YearMonth,
    focusedDay: LocalDate,
    onDaySelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DayOfWeek.values().forEach { dayOfWeek ->
                Text(
                    text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        val firstDay = month.atDay(1)
        val daysInMonth = month.lengthOfMonth()
        val leadingEmpty = (firstDay.dayOfWeek.value + 6) % 7
        val totalCells = leadingEmpty + daysInMonth
        val rowCount = (totalCells + 6) / 7
        repeat(rowCount) { rowIndex ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(7) { columnIndex ->
                    val cellIndex = rowIndex * 7 + columnIndex
                    val dayNumber = cellIndex - leadingEmpty + 1
                    if (dayNumber in 1..daysInMonth) {
                        val date = month.atDay(dayNumber)
                        MonthDayCell(
                            date = date,
                            isSelected = date == focusedDay,
                            onClick = { onDaySelected(date) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Box(modifier = Modifier.weight(1f).padding(vertical = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthDayCell(
    date: LocalDate,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    }
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier = modifier
            .padding(vertical = 4.dp)
            .clip(MaterialTheme.shapes.small)
            .background(containerColor)
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = date.format(DayFormatter)
                selected = isSelected
                stateDescription = if (isSelected) "선택됨" else "선택되지 않음"
            }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PeriodControls(
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit
) {
    Row {
        IconButton(onClick = onPreviousPeriod) {
            Icon(
                imageVector = Icons.Filled.ChevronLeft,
                contentDescription = "이전 기간으로 이동"
            )
        }
        IconButton(onClick = onNextPeriod) {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "다음 기간으로 이동"
            )
        }
    }
}

@Composable
private fun AgendaSnapshotContent(
    snapshot: AgendaSnapshot,
    filters: AgendaFilters,
    selectedTab: AgendaTab,
    onToggleTask: (Task) -> Unit,
    onTaskClick: (Task) -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
    onCycleCompletedTaskFilter: () -> Unit,
    onToggleShowRecurringEvents: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (selectedTab) {
        AgendaTab.Daily -> DayAgenda(
            snapshot = snapshot,
            filters = filters,
            onToggleTask = onToggleTask,
            onTaskClick = onTaskClick,
            onEventClick = onEventClick,
            onCycleCompletedTaskFilter = onCycleCompletedTaskFilter,
            onToggleShowRecurringEvents = onToggleShowRecurringEvents,
            modifier = modifier
        )

        AgendaTab.Weekly -> WeekAgenda(
            snapshot = snapshot,
            filters = filters,
            onToggleTask = onToggleTask,
            onTaskClick = onTaskClick,
            onEventClick = onEventClick,
            onCycleCompletedTaskFilter = onCycleCompletedTaskFilter,
            onToggleShowRecurringEvents = onToggleShowRecurringEvents,
            modifier = modifier
        )

        AgendaTab.Monthly -> MonthAgenda(
            snapshot = snapshot,
            filters = filters,
            onToggleTask = onToggleTask,
            onTaskClick = onTaskClick,
            onEventClick = onEventClick,
            onCycleCompletedTaskFilter = onCycleCompletedTaskFilter,
            onToggleShowRecurringEvents = onToggleShowRecurringEvents,
            modifier = modifier
        )
    }
}

@Composable
private fun DayAgenda(
    snapshot: AgendaSnapshot,
    filters: AgendaFilters,
    onToggleTask: (Task) -> Unit,
    onTaskClick: (Task) -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
    onCycleCompletedTaskFilter: () -> Unit,
    onToggleShowRecurringEvents: () -> Unit,
    modifier: Modifier = Modifier
) {
    AgendaList(
        snapshot = snapshot,
        summaryTitle = "일일 요약",
        summaryPeriod = DayFormatter.format(snapshot.rangeStart),
        filters = filters,
        onToggleTask = onToggleTask,
        onTaskClick = onTaskClick,
        onEventClick = onEventClick,
        onCycleCompletedTaskFilter = onCycleCompletedTaskFilter,
        onToggleShowRecurringEvents = onToggleShowRecurringEvents,
        modifier = modifier
    )
}

@Composable
private fun WeekAgenda(
    snapshot: AgendaSnapshot,
    filters: AgendaFilters,
    onToggleTask: (Task) -> Unit,
    onTaskClick: (Task) -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
    onCycleCompletedTaskFilter: () -> Unit,
    onToggleShowRecurringEvents: () -> Unit,
    modifier: Modifier = Modifier
) {
    val periodLabel = buildString {
        append(WeekFormatter.format(snapshot.rangeStart))
        snapshot.rangeEnd.takeIf { it != snapshot.rangeStart }?.let { end ->
            append(" ~ ")
            append(WeekFormatter.format(end))
        }
    }
    AgendaList(
        snapshot = snapshot,
        summaryTitle = "주간 요약",
        summaryPeriod = periodLabel,
        filters = filters,
        onToggleTask = onToggleTask,
        onTaskClick = onTaskClick,
        onEventClick = onEventClick,
        onCycleCompletedTaskFilter = onCycleCompletedTaskFilter,
        onToggleShowRecurringEvents = onToggleShowRecurringEvents,
        modifier = modifier
    )
}

@Composable
private fun MonthAgenda(
    snapshot: AgendaSnapshot,
    filters: AgendaFilters,
    onToggleTask: (Task) -> Unit,
    onTaskClick: (Task) -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
    onCycleCompletedTaskFilter: () -> Unit,
    onToggleShowRecurringEvents: () -> Unit,
    modifier: Modifier = Modifier
) {
    val monthLabel = MonthFormatter.format(snapshot.rangeStart)
    AgendaList(
        snapshot = snapshot,
        summaryTitle = "월간 요약",
        summaryPeriod = monthLabel,
        filters = filters,
        onToggleTask = onToggleTask,
        onTaskClick = onTaskClick,
        onEventClick = onEventClick,
        onCycleCompletedTaskFilter = onCycleCompletedTaskFilter,
        onToggleShowRecurringEvents = onToggleShowRecurringEvents,
        modifier = modifier
    )
}

@Composable
private fun AgendaList(
    snapshot: AgendaSnapshot,
    summaryTitle: String,
    summaryPeriod: String,
    filters: AgendaFilters,
    onToggleTask: (Task) -> Unit,
    onTaskClick: (Task) -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
    onCycleCompletedTaskFilter: () -> Unit,
    onToggleShowRecurringEvents: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleEvents = remember(snapshot.events, filters.showRecurringEvents) {
        if (filters.showRecurringEvents) snapshot.events else snapshot.events.filter { it.recurrence == null }
    }
    val visibleTasks = remember(snapshot.tasks, filters.completedTaskFilter) {
        when (filters.completedTaskFilter) {
            CompletedTaskFilter.All -> snapshot.tasks
            CompletedTaskFilter.HideCompleted -> snapshot.tasks.filterNot { it.status.isDone() }
            CompletedTaskFilter.CompletedOnly -> snapshot.tasks.filter { it.status.isDone() }
        }
    }
    val hiddenRecurringCount = snapshot.events.size - visibleEvents.size
    val hiddenSummary = remember(filters, snapshot, hiddenRecurringCount) {
        hiddenItemsSummary(filters, snapshot, hiddenRecurringCount)
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AgendaSummaryCard(
                title = summaryTitle,
                period = summaryPeriod,
                snapshot = snapshot,
                filters = filters,
                visibleEventCount = visibleEvents.size,
                visibleTaskCount = visibleTasks.size,
                hiddenRecurringCount = hiddenRecurringCount
            )
        }
        item {
            SectionTitle(text = "일정")
        }
        item {
            AgendaFilterRow(
                filters = filters,
                onToggleShowRecurringEvents = onToggleShowRecurringEvents,
                onCycleCompletedTaskFilter = onCycleCompletedTaskFilter
            )
        }
        hiddenSummary?.let { message ->
            item {
                FilterNotice(message = message)
            }
        }
        if (visibleEvents.isEmpty()) {
            item {
                EmptySectionMessage(message = "표시할 일정이 없어요. 필터를 확인해 보세요.")
            }
        } else {
            items(
                items = visibleEvents,
                key = { "${'$'}{it.id}-${'$'}{it.start}" }
            ) { event ->
                val hasConflict = snapshot.conflictingEventIds.contains(event.id)
                EventCard(
                    event = event,
                    hasConflict = hasConflict,
                    onClick = { onEventClick(event) }
                )
            }
        }
        item {
            SectionTitle(text = "할 일")
        }
        if (visibleTasks.isEmpty()) {
            item {
                EmptySectionMessage(message = "표시할 할 일이 없어요. 필터를 확인해 보세요.")
            }
        } else {
            items(visibleTasks, key = { it.id }) { task ->
                SwipeableTaskRow(
                    task = task,
                    onToggleTask = { onToggleTask(task) },
                    onTaskClick = { onTaskClick(task) }
                )
            }
        }
    }
}

@Composable
private fun AgendaFilterRow(
    filters: AgendaFilters,
    onToggleShowRecurringEvents: () -> Unit,
    onCycleCompletedTaskFilter: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FilterChip(
            selected = filters.showRecurringEvents,
            onClick = onToggleShowRecurringEvents,
            label = { Text("반복 일정 표시") }
        )
        FilterChip(
            selected = filters.completedTaskFilter != CompletedTaskFilter.All,
            onClick = onCycleCompletedTaskFilter,
            label = { Text(filters.completedTaskFilter.label()) },
            modifier = Modifier.semantics {
                stateDescription = filters.completedTaskFilter.accessibilityDescription()
            }
        )
    }
}

@Composable
private fun AgendaSummaryCard(
    title: String,
    period: String,
    snapshot: AgendaSnapshot,
    filters: AgendaFilters,
    visibleEventCount: Int,
    visibleTaskCount: Int,
    hiddenRecurringCount: Int,
    modifier: Modifier = Modifier
) {
    val hiddenSummary = hiddenItemsSummary(filters, snapshot, hiddenRecurringCount)
    val conflictCount = snapshot.conflictingEventIds.size
    val baseSummary = buildString {
        append(title)
        append(". ")
        append(period)
        append(" 일정 요약. 전체 일정 ")
        append(snapshot.events.size)
        append("건, 진행 중인 할 일 ")
        append(snapshot.pendingCount)
        append("건, 완료된 할 일 ")
        append(snapshot.completedCount)
        append("건입니다.")
        if (snapshot.overdueTasks.isNotEmpty()) {
            append(' ')
            append("마감이 지난 할 일 ")
            append(snapshot.overdueTasks.size)
            append("건이 있어요.")
        }
        hiddenSummary?.let {
            append(' ')
            append(it)
        }
    }
    val summaryWithConflicts = if (conflictCount > 0) {
        buildString {
            append(baseSummary)
            append(' ')
            append("시간이 겹치는 일정 ")
            append(conflictCount)
            append("건이 있어요.")
        }
    } else {
        baseSummary
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = summaryWithConflicts
            }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = period,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "전체 일정 ${snapshot.events.size}건",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "진행 중 ${snapshot.pendingCount}건 · 완료 ${snapshot.completedCount}건",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "목록에 표시 중: 일정 ${visibleEventCount}건 · 할 일 ${visibleTaskCount}건",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            hiddenSummary?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (snapshot.overdueTasks.isNotEmpty()) {
                Text(
                    text = "마감 지남 ${snapshot.overdueTasks.size}건",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (conflictCount > 0) {
                Text(
                    text = "시간이 겹치는 일정 ${conflictCount}건",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun CompletedTaskFilter.label(): String = when (this) {
    CompletedTaskFilter.All -> "할 일: 전체"
    CompletedTaskFilter.HideCompleted -> "할 일: 완료 숨김"
    CompletedTaskFilter.CompletedOnly -> "할 일: 완료만"
}

private fun CompletedTaskFilter.accessibilityDescription(): String = when (this) {
    CompletedTaskFilter.All -> "모든 할 일을 표시합니다"
    CompletedTaskFilter.HideCompleted -> "완료된 할 일을 숨깁니다"
    CompletedTaskFilter.CompletedOnly -> "완료된 할 일만 표시합니다"
}

private fun hiddenItemsSummary(
    filters: AgendaFilters,
    snapshot: AgendaSnapshot,
    hiddenRecurringCount: Int
): String? {
    val details = mutableListOf<String>()
    if (!filters.showRecurringEvents && hiddenRecurringCount > 0) {
        details += "반복 일정 ${hiddenRecurringCount}건"
    }
    when (filters.completedTaskFilter) {
        CompletedTaskFilter.All -> Unit
        CompletedTaskFilter.HideCompleted -> if (snapshot.completedCount > 0) {
            details += "완료된 할 일 ${snapshot.completedCount}건"
        }
        CompletedTaskFilter.CompletedOnly -> if (snapshot.pendingCount > 0) {
            details += "진행 중인 할 일 ${snapshot.pendingCount}건"
        }
    }
    if (details.isEmpty()) return null
    return "필터로 숨겨진 항목: ${details.joinToString(", ")}."
}

@Composable
private fun FilterNotice(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = message
            }
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.semantics { role = Role.Header }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventCard(event: CalendarEvent, hasConflict: Boolean, onClick: () -> Unit) {
    val colors = if (hasConflict) {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
    } else {
        CardDefaults.cardColors()
    }

    val description = buildString {
        append(event.title)
        append(" 일정")
        if (hasConflict) {
            append(", 다른 일정과 시간이 겹칩니다")
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = description },
        colors = colors,
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (hasConflict) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null
                    )
                    Text(
                        text = "다른 일정과 시간이 겹쳐요",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                EventTimeBadge(text = eventTimeRange(event))
            }
            event.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = eventTimeRange(event),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            event.recurrence?.let { recurrence ->
                Spacer(modifier = Modifier.height(8.dp))
                RecurrenceBadge(recurrence = recurrence)
            }
            event.location?.let { location ->
                Text(
                    text = location,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableTaskRow(
    task: Task,
    onToggleTask: () -> Unit,
    onTaskClick: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState { value ->
        if (value != SwipeToDismissBoxValue.Settled) {
            onToggleTask()
            true
        } else {
            false
        }
    }
    val actionLabel = if (task.status.isDone()) "다시 미완료로 표시" else "완료로 표시"
    val containerColor = if (task.status.isDone()) {
        MaterialTheme.colorScheme.tertiaryContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val contentColor = if (task.status.isDone()) {
        MaterialTheme.colorScheme.onTertiaryContainer
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            DismissBackground(
                label = actionLabel,
                containerColor = containerColor,
                contentColor = contentColor
            )
        },
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true
    ) {
        TaskRow(
            task = task,
            onToggleTask = onToggleTask,
            onTaskClick = onTaskClick
        )
    }

    LaunchedEffect(task.status) {
        dismissState.snapTo(SwipeToDismissBoxValue.Settled)
    }
}

@Composable
private fun DismissBackground(
    label: String,
    containerColor: Color,
    contentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(containerColor)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = contentColor, style = MaterialTheme.typography.bodyMedium)
        Text(text = label, color = contentColor, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun EventTimeBadge(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(999.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}


@Composable
private fun TaskRow(
    task: Task,
    onToggleTask: () -> Unit,
    onTaskClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onTaskClick)
            .semantics(mergeDescendants = true) {
                contentDescription = task.accessibleDescription()
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = task.status.isDone(),
            onCheckedChange = { onToggleTask() },
            modifier = Modifier.semantics {
                role = Role.Checkbox
                stateDescription = if (task.status.isDone()) "완료됨" else "미완료"
            }
        )
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                TaskStatusBadge(status = task.status)
            }
            task.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            task.dueAt?.let { dueAt ->
                Text(
                    text = "마감 ${dueAt.format(DateTimeDetailFormatter)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TaskStatusBadge(status: TaskStatus) {
    val colors = MaterialTheme.colorScheme
    val (containerColor, contentColor) = when (status) {
        TaskStatus.Pending -> colors.secondaryContainer to colors.onSecondaryContainer
        TaskStatus.InProgress -> colors.tertiaryContainer to colors.onTertiaryContainer
        TaskStatus.Completed -> colors.primaryContainer to colors.onPrimaryContainer
    }
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(999.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Text(
            text = status.displayName(),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

private sealed interface AgendaSheetContent {
    data class TaskDetail(val task: Task) : AgendaSheetContent
    data class EventDetail(val event: CalendarEvent) : AgendaSheetContent
    data class QuickAdd(
        val initialType: QuickAddType,
        val focusDate: LocalDate,
        val period: AgendaPeriod
    ) : AgendaSheetContent
}

@Composable
private fun TaskDetailSheet(
    task: Task,
    onToggleTask: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onEditTask: (Task) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = task.title,
            style = MaterialTheme.typography.titleLarge
        )
        task.description?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        task.dueAt?.let { dueAt ->
            Text(
                text = "마감 ${dueAt.format(DateTimeDetailFormatter)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "상태: ${task.status.displayName()}",
            style = MaterialTheme.typography.bodyMedium
        )
        Button(onClick = { onToggleTask(task) }) {
            Text(task.status.toggleLabel())
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
        ) {
            OutlinedButton(onClick = { onEditTask(task) }) {
                Text("편집")
            }
            TextButton(
                onClick = { onDeleteTask(task) },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("삭제")
            }
            TextButton(onClick = onClose) {
                Text("닫기")
            }
        }
    }
}

@Composable
private fun EventDetailSheet(
    event: CalendarEvent,
    onDeleteEvent: (CalendarEvent) -> Unit,
    onEditEvent: (CalendarEvent) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = event.title,
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = eventTimeRange(event),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        event.location?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        event.description?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
        ) {
            OutlinedButton(onClick = { onEditEvent(event) }) {
                Text("편집")
            }
            TextButton(
                onClick = { onDeleteEvent(event) },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("삭제")
            }
            TextButton(onClick = onClose) {
                Text("닫기")
            }
        }
    }
}

@Composable
private fun QuickAddSheet(
    content: AgendaSheetContent.QuickAdd,
    onCreateTask: suspend (QuickAddTaskInput) -> Result<Unit>,
    onCreateEvent: suspend (QuickAddEventInput) -> Result<Unit>,
    onDismiss: () -> Unit
) {
    var selectedType by rememberSaveable { mutableStateOf(content.initialType) }
    var taskTitle by rememberSaveable { mutableStateOf("") }
    var taskNotes by rememberSaveable { mutableStateOf("") }
    var taskTime by rememberSaveable { mutableStateOf(DEFAULT_TASK_TIME_TEXT) }

    var eventTitle by rememberSaveable { mutableStateOf("") }
    var eventLocation by rememberSaveable { mutableStateOf("") }
    var eventNotes by rememberSaveable { mutableStateOf("") }
    var eventStartTime by rememberSaveable { mutableStateOf(DEFAULT_EVENT_START_TEXT) }
    var eventEndTime by rememberSaveable { mutableStateOf(DEFAULT_EVENT_END_TEXT) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(selectedType) {
        errorMessage = null
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "빠른 추가",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "${periodSummary(content.period)} · ${content.focusDate.format(DayFormatter)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilterChip(
                selected = selectedType == QuickAddType.Task,
                onClick = { selectedType = QuickAddType.Task },
                label = { Text("할 일") }
            )
            FilterChip(
                selected = selectedType == QuickAddType.Event,
                onClick = { selectedType = QuickAddType.Event },
                label = { Text("일정") }
            )
        }

        when (selectedType) {
            QuickAddType.Task -> {
                OutlinedTextField(
                    value = taskTitle,
                    onValueChange = { taskTitle = it },
                    label = { Text("제목") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quickAddTaskTitle")
                )
                OutlinedTextField(
                    value = taskNotes,
                    onValueChange = { taskNotes = it },
                    label = { Text("메모 (선택)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quickAddTaskNotes")
                )
                OutlinedTextField(
                    value = taskTime,
                    onValueChange = { taskTime = it },
                    label = { Text("마감 시간 (HH:mm)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quickAddTaskDueTime"),
                    supportingText = {
                        Text(
                            text = "비워 두면 시간 없이 저장됩니다.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                )
            }
            QuickAddType.Event -> {
                OutlinedTextField(
                    value = eventTitle,
                    onValueChange = { eventTitle = it },
                    label = { Text("제목") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quickAddEventTitle")
                )
                OutlinedTextField(
                    value = eventLocation,
                    onValueChange = { eventLocation = it },
                    label = { Text("위치 (선택)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quickAddEventLocation")
                )
                OutlinedTextField(
                    value = eventStartTime,
                    onValueChange = { eventStartTime = it },
                    label = { Text("시작 시간 (HH:mm)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quickAddEventStart")
                )
                OutlinedTextField(
                    value = eventEndTime,
                    onValueChange = { eventEndTime = it },
                    label = { Text("종료 시간 (HH:mm)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quickAddEventEnd")
                )
                OutlinedTextField(
                    value = eventNotes,
                    onValueChange = { eventNotes = it },
                    label = { Text("메모 (선택)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quickAddEventNotes")
                )
            }
        }

        errorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
        ) {
            TextButton(onClick = {
                if (!isSaving) {
                    onDismiss()
                }
            }) {
                Text("취소")
            }
            Button(
                onClick = {
                    if (isSaving) return@Button
                    errorMessage = null
                    when (selectedType) {
                        QuickAddType.Task -> {
                            val title = taskTitle.trim()
                            if (title.isEmpty()) {
                                errorMessage = "제목을 입력해 주세요."
                                return@Button
                            }
                            val dueTimeResult = taskTime.takeIf { it.isNotBlank() }?.let { value ->
                                parseInputTime(value).onFailure {
                                    errorMessage = "시간 형식은 HH:mm 이어야 해요."
                                }
                            }
                            if (errorMessage != null) return@Button

                            val dueTimeValue = dueTimeResult?.getOrNull()

                            coroutineScope.launch {
                                isSaving = true
                                val result = onCreateTask(
                                    QuickAddTaskInput(
                                        title = title,
                                        notes = taskNotes.takeIf { it.isNotBlank() },
                                        dueTime = dueTimeValue
                                    )
                                )
                                if (result.isSuccess) {
                                    onDismiss()
                                } else {
                                    errorMessage = result.exceptionOrNull()?.message
                                        ?: "저장에 실패했습니다."
                                }
                                isSaving = false
                            }
                        }
                        QuickAddType.Event -> {
                            val title = eventTitle.trim()
                            if (title.isEmpty()) {
                                errorMessage = "제목을 입력해 주세요."
                                return@Button
                            }
                            val startResult = parseInputTime(eventStartTime).onFailure {
                                errorMessage = "시작 시간을 HH:mm 형식으로 입력하세요."
                            }
                            if (errorMessage != null) return@Button
                            val endResult = parseInputTime(eventEndTime).onFailure {
                                errorMessage = "종료 시간을 HH:mm 형식으로 입력하세요."
                            }
                            if (errorMessage != null) return@Button

                            val startValue = startResult.getOrNull()
                            val endValue = endResult.getOrNull()
                            if (startValue == null || endValue == null) {
                                errorMessage = "시간을 다시 확인해 주세요."
                                return@Button
                            }

                            coroutineScope.launch {
                                isSaving = true
                                val result = onCreateEvent(
                                    QuickAddEventInput(
                                        title = title,
                                        location = eventLocation.takeIf { it.isNotBlank() },
                                        notes = eventNotes.takeIf { it.isNotBlank() },
                                        startTime = startValue,
                                        endTime = endValue
                                    )
                                )
                                if (result.isSuccess) {
                                    onDismiss()
                                } else {
                                    errorMessage = result.exceptionOrNull()?.message
                                        ?: "저장에 실패했습니다."
                                }
                                isSaving = false
                            }
                        }
                    }
                },
                enabled = !isSaving
            ) {
                Text(if (isSaving) "저장 중..." else "저장")
            }
        }
    }
}



@Composable
private fun AgendaLoading() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                contentDescription = "일정을 불러오는 중입니다"
                liveRegion = LiveRegionMode.Polite
            },
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun AgendaError(error: Throwable) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                role = Role.Alert
                contentDescription = error.localizedMessage
                    ?: "일정을 불러오는 중 문제가 발생했어요"
                liveRegion = LiveRegionMode.Assertive
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = error.localizedMessage ?: "일정을 불러오는 중 문제가 발생했어요",
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AgendaEmptyState(onQuickAddClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = "아직 일정이 없어요. 빠른 추가 버튼으로 오늘의 첫 일정이나 할 일을 만들어 보세요."
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Inbox,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "아직 일정이 없어요",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = "빠른 추가 버튼으로 오늘의 첫 일정이나 할 일을 만들어 보세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(onClick = onQuickAddClick) {
                Text("빠른 추가 열기")
            }
        }
    }
}

private fun TaskStatus.displayName(): String = when (this) {
    TaskStatus.Pending -> "대기 중"
    TaskStatus.InProgress -> "진행 중"
    TaskStatus.Completed -> "완료"
}

private fun TaskStatus.toggleLabel(): String = when (this) {
    TaskStatus.Completed -> "대기 중으로 표시"
    else -> "완료로 표시"
}

private fun eventTimeRange(event: CalendarEvent): String {
    val sameDay = event.start.toLocalDate() == event.end.toLocalDate()
    return if (sameDay) {
        val date = event.start.format(DayFormatter)
        val startTime = event.start.format(TimeOnlyFormatter)
        val endTime = event.end.format(TimeOnlyFormatter)
        "$date · $startTime ~ $endTime"
    } else {
        "${event.start.format(DateTimeDetailFormatter)} ~ ${event.end.format(DateTimeDetailFormatter)}"
    }
}

@Composable
private fun RecurrenceBadge(recurrence: Recurrence) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Text(
            text = formatRecurrence(recurrence),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

private fun formatRecurrence(recurrence: Recurrence): String {
    val interval = recurrence.interval.coerceAtLeast(1)
    val (baseLabel, unitLabel) = when (recurrence.rule) {
        RecurrenceRule.Daily -> "매일" to "일"
        RecurrenceRule.Weekly -> "매주" to "주"
        RecurrenceRule.Monthly -> "매월" to "달"
        RecurrenceRule.Yearly -> "매년" to "년"
    }

    return if (interval == 1) {
        baseLabel
    } else {
        "매 ${interval}${unitLabel}"
    }
}

private fun AgendaUserMessage.toSnackbarMessage(): String = when (this) {
    is AgendaUserMessage.QuickAddSuccess -> when (type) {
        QuickAddType.Task -> "할 일을 추가했어요."
        QuickAddType.Event -> "일정을 추가했어요."
    }
    is AgendaUserMessage.QuickAddFailure -> reason
}

private fun AgendaTab.displayLabel(): String = when (this) {
    AgendaTab.Daily -> "일간"
    AgendaTab.Weekly -> "주간"
    AgendaTab.Monthly -> "월간"
}

private fun AgendaTab.accessibleLabel(): String = when (this) {
    AgendaTab.Daily -> "일간 아젠다 탭"
    AgendaTab.Weekly -> "주간 아젠다 탭"
    AgendaTab.Monthly -> "월간 아젠다 탭"
}

private fun periodLabel(period: AgendaPeriod): String = when (period) {
    is AgendaPeriod.Day -> DayFormatter.format(period.date)
    is AgendaPeriod.Week -> {
        val start = WeekFormatter.format(period.start)
        val end = WeekFormatter.format(period.end)
        "$start ~ $end"
    }
    is AgendaPeriod.Month -> MonthFormatter.format(
        YearMonth.of(period.year, period.month).atDay(1)
    )
}

@Preview(name = "일간 아젠다 – 데이터 로드 완료", showBackground = true)
@Composable
private fun AgendaScreenLoadedPreview() {
    CalendarTheme {
        AgendaScreen(
            uiState = previewAgendaUiState(),
            selectedTab = AgendaTab.Daily,
            onTabSelected = {},
            onPreviousPeriod = {},
            onNextPeriod = {},
            onToggleTask = {},
            onTaskClick = {},
            onEventClick = {},
            onCycleCompletedTaskFilter = {},
            onToggleShowRecurringEvents = {},
            onWeekDaySelected = {},
            onMonthDaySelected = {},
            focusedDay = PreviewAgendaDate,
            period = AgendaPeriod.Day(PreviewAgendaDate),
            onQuickAddClick = {}
        )
    }
}

@Preview(name = "일간 아젠다 – 비어 있는 상태", showBackground = true)
@Composable
private fun AgendaScreenEmptyPreview() {
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
            onCycleCompletedTaskFilter = {},
            onToggleShowRecurringEvents = {},
            onWeekDaySelected = {},
            onMonthDaySelected = {},
            focusedDay = PreviewAgendaDate,
            period = AgendaPeriod.Day(PreviewAgendaDate),
            onQuickAddClick = {}
        )
    }
}

internal fun previewAgendaUiState(): AgendaUiState {
    return AgendaUiState(
        snapshot = previewAgendaSnapshot(),
        isLoading = false,
        error = null,
        userMessage = null
    )
}

internal fun previewAgendaSnapshot(): AgendaSnapshot {
    return AgendaSnapshot(
        rangeStart = PreviewAgendaDate,
        tasks = previewAgendaTasks,
        events = previewAgendaEvents
    )
}

private val PreviewAgendaDate: LocalDate = LocalDate.of(2024, 5, 21)

private val previewAgendaTasks: List<Task> = listOf(
    Task(
        title = "디자인 시안 검토",
        description = "UX 다듬기 피드백 정리",
        status = TaskStatus.InProgress,
        dueAt = LocalDateTime.of(PreviewAgendaDate, LocalTime.of(11, 0)),
        period = AgendaPeriod.Day(PreviewAgendaDate)
    ),
    Task(
        title = "출시 체크리스트 업데이트",
        status = TaskStatus.Completed,
        dueAt = LocalDateTime.of(PreviewAgendaDate, LocalTime.of(15, 30)),
        period = AgendaPeriod.Day(PreviewAgendaDate)
    ),
    Task(
        title = "품질 점검 버그 분류",
        status = TaskStatus.Pending,
        dueAt = LocalDateTime.of(PreviewAgendaDate, LocalTime.of(17, 0)),
        period = AgendaPeriod.Week(PreviewAgendaDate.startOfWeek())
    )
)

private val previewAgendaEvents: List<CalendarEvent> = listOf(
    CalendarEvent(
        title = "팀 스탠드업",
        description = "목표 공유",
        start = LocalDateTime.of(PreviewAgendaDate, LocalTime.of(9, 30)),
        end = LocalDateTime.of(PreviewAgendaDate, LocalTime.of(10, 0)),
        location = "회의실 A",
        recurrence = Recurrence(rule = RecurrenceRule.Daily, interval = 1)
    ),
    CalendarEvent(
        title = "디자인 리뷰",
        start = LocalDateTime.of(PreviewAgendaDate, LocalTime.of(13, 0)),
        end = LocalDateTime.of(PreviewAgendaDate, LocalTime.of(14, 0)),
        location = "온라인 미팅"
    ),
    CalendarEvent(
        title = "회고 준비",
        start = LocalDateTime.of(PreviewAgendaDate, LocalTime.of(16, 0)),
        end = LocalDateTime.of(PreviewAgendaDate, LocalTime.of(16, 30)),
        location = "회의실 B",
        recurrence = Recurrence(rule = RecurrenceRule.Weekly, interval = 1)
    )
)

private data class QuickAddTaskInput(
    val title: String,
    val notes: String?,
    val dueTime: LocalTime?
)

private data class QuickAddEventInput(
    val title: String,
    val location: String?,
    val notes: String?,
    val startTime: LocalTime,
    val endTime: LocalTime
)

private fun parseInputTime(value: String): Result<LocalTime> {
    return runCatching { LocalTime.parse(value.trim(), QuickAddTimeFormatter) }
}

private const val DEFAULT_TASK_TIME_TEXT = "09:00"
private const val DEFAULT_EVENT_START_TEXT = "09:00"
private const val DEFAULT_EVENT_END_TEXT = "10:00"
private val QuickAddTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mm", Locale.KOREAN)

private fun LocalDate.startOfWeek(): LocalDate {
    var date = this
    while (date.dayOfWeek != DayOfWeek.MONDAY) {
        date = date.minusDays(1)
    }
    return date
}

private fun Task.accessibleDescription(): String {
    val statusText = when (status) {
        TaskStatus.Pending -> "대기 중"
        TaskStatus.InProgress -> "진행 중"
        TaskStatus.Completed -> "완료"
    }
    return "$statusText 할 일: $title"
}

@Composable
private fun EmptySectionMessage(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.semantics {
            liveRegion = LiveRegionMode.Polite
            contentDescription = message
        }
    )
}

