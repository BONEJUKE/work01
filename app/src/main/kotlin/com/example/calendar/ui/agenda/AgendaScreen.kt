package com.example.calendar.ui.agenda

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.height
import com.example.calendar.data.AgendaPeriod
import com.example.calendar.data.CalendarEvent
import com.example.calendar.data.Task
import com.example.calendar.data.TaskStatus
import com.example.calendar.scheduler.AgendaSnapshot
import com.example.calendar.ui.AgendaUiState
import com.example.calendar.ui.AgendaViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

private val DayFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
private val WeekFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
private val MonthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
private val DateTimeDetailFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a", Locale.getDefault())
private val TimeOnlyFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaRoute(
    viewModel: AgendaViewModel,
    onEventClick: (CalendarEvent) -> Unit = {},
    onTaskClick: (Task) -> Unit = {}
) {
    val uiState by viewModel.state.collectAsState()

    val navigationState = rememberAgendaNavigationState()
    val selectedTab = navigationState.currentTab
    var selectedDayIso by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var selectedWeekIso by rememberSaveable { mutableStateOf(LocalDate.now().startOfWeek().toString()) }
    var selectedMonthIso by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }

    val currentPeriod = remember(selectedTab, selectedDayIso, selectedWeekIso, selectedMonthIso) {
        when (selectedTab) {
            AgendaTab.Daily -> AgendaPeriod.Day(LocalDate.parse(selectedDayIso))
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
                    val updated = snapshot.events.find { it.id == currentContent.event.id }
                    if (updated != null && updated != currentContent.event) {
                        sheetContent = AgendaSheetContent.EventDetail(updated)
                    } else if (updated == null) {
                        hideSheet()
                    }
                }
            }
        }
    }

    AgendaScreen(
        uiState = uiState,
        selectedTab = selectedTab,
        onTabSelected = { tab ->
            navigationState.navigateTo(tab)
            when (tab) {
                AgendaTab.Daily -> {
                    // Keep the previously selected day when returning to the daily view.
                }
                AgendaTab.Weekly -> {
                    val weekStart = LocalDate.parse(selectedDayIso).startOfWeek()
                    selectedWeekIso = weekStart.toString()
                    selectedDayIso = weekStart.toString()
                }
                AgendaTab.Monthly -> {
                    val month = monthFromDay(selectedDayIso)
                    selectedMonthIso = month.toString()
                    selectedDayIso = month.atDay(1).toString()
                }
            }
        },
        onPreviousPeriod = {
            when (selectedTab) {
                AgendaTab.Daily -> selectedDayIso = LocalDate.parse(selectedDayIso).minusDays(1).toString()
                AgendaTab.Weekly -> {
                    val weekStart = LocalDate.parse(selectedWeekIso).minusWeeks(1)
                    selectedWeekIso = weekStart.toString()
                    selectedDayIso = weekStart.toString()
                }
                AgendaTab.Monthly -> {
                    val month = YearMonth.parse(selectedMonthIso).minusMonths(1)
                    selectedMonthIso = month.toString()
                    selectedDayIso = month.atDay(1).toString()
                }
            }
        },
        onNextPeriod = {
            when (selectedTab) {
                AgendaTab.Daily -> selectedDayIso = LocalDate.parse(selectedDayIso).plusDays(1).toString()
                AgendaTab.Weekly -> {
                    val weekStart = LocalDate.parse(selectedWeekIso).plusWeeks(1)
                    selectedWeekIso = weekStart.toString()
                    selectedDayIso = weekStart.toString()
                }
                AgendaTab.Monthly -> {
                    val month = YearMonth.parse(selectedMonthIso).plusMonths(1)
                    selectedMonthIso = month.toString()
                    selectedDayIso = month.atDay(1).toString()
                }
            }
        },
        onToggleTask = viewModel::toggleTask,
        onTaskClick = openTaskSheet,
        onEventClick = openEventSheet,
        period = currentPeriod
    )

    sheetContent?.let { content ->
        ModalBottomSheet(
            onDismissRequest = hideSheet,
            sheetState = sheetState
        ) {
            AgendaDetailSheet(
                content = content,
                onToggleTask = { task ->
                    viewModel.toggleTask(task)
                    sheetContent = AgendaSheetContent.TaskDetail(task.toggleCompletion())
                },
                onClose = hideSheet
            )
        }
    }
}

private fun monthFromDay(dayIso: String): YearMonth {
    val day = LocalDate.parse(dayIso)
    return YearMonth.of(day.year, day.month)
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
    period: AgendaPeriod,
    modifier: Modifier = Modifier
) {
    Scaffold(
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
        Box(modifier = modifier.padding(padding).fillMaxSize()) {
            when {
                uiState.isLoading -> AgendaLoading()
                uiState.error != null -> AgendaError(uiState.error)
                uiState.snapshot != null -> AgendaSnapshotContent(
                    snapshot = uiState.snapshot,
                    onToggleTask = onToggleTask,
                    onTaskClick = onTaskClick,
                    onEventClick = onEventClick,
                    modifier = Modifier.fillMaxSize()
                )
                else -> AgendaEmptyState()
            }
        }
    }
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
                    stateDescription = if (isSelected) "Selected" else "Not selected"
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
private fun PeriodControls(
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit
) {
    Row {
        TextButton(
            onClick = onPreviousPeriod,
            modifier = Modifier.semantics { contentDescription = "Previous period" }
        ) {
            Text("◀")
        }
        TextButton(
            onClick = onNextPeriod,
            modifier = Modifier.semantics { contentDescription = "Next period" }
        ) {
            Text("▶")
        }
    }
}

@Composable
private fun AgendaSnapshotContent(
    snapshot: AgendaSnapshot,
    onToggleTask: (Task) -> Unit,
    onTaskClick: (Task) -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AgendaSummaryCard(snapshot)
        }
        item {
            SectionTitle(text = "Events")
        }
        if (snapshot.events.isEmpty()) {
            item { EmptySectionMessage(message = "No events scheduled") }
        } else {
            items(snapshot.events) { event ->
                EventCard(event = event, onClick = { onEventClick(event) })
            }
        }
        item {
            SectionTitle(text = "Tasks")
        }
        if (snapshot.tasks.isEmpty()) {
            item { EmptySectionMessage(message = "No tasks for this period") }
        } else {
            items(snapshot.tasks, key = { it.id }) { task ->
                TaskRow(
                    task = task,
                    onToggleTask = { onToggleTask(task) },
                    onTaskClick = { onTaskClick(task) }
                )
            }
        }
    }
}

@Composable
private fun AgendaSummaryCard(snapshot: AgendaSnapshot) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Overview",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${snapshot.events.size} events",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${snapshot.pendingCount} pending · ${snapshot.completedCount} completed",
                style = MaterialTheme.typography.bodyMedium
            )
            if (snapshot.overdueTasks.isNotEmpty()) {
                Text(
                    text = "${snapshot.overdueTasks.size} overdue",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
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
private fun EventCard(event: CalendarEvent, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "${event.title} event" },
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
            event.location?.let { location ->
                Text(
                    text = location,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
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
            .semantics(mergeDescendants = true) {
                contentDescription = task.accessibleDescription()
                onClick(label = "Open task details") {
                    onTaskClick()
                    true
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = task.status.isDone(),
            onCheckedChange = { onToggleTask() },
            modifier = Modifier.semantics {
                role = Role.Checkbox
                stateDescription = if (task.status.isDone()) "Completed" else "Not completed"
            }
        )
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
                    text = "Due ${dueAt.format(DateTimeDetailFormatter)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        TextButton(onClick = onTaskClick) {
            Text("Open")
        }
    }
}

private sealed interface AgendaSheetContent {
    data class TaskDetail(val task: Task) : AgendaSheetContent
    data class EventDetail(val event: CalendarEvent) : AgendaSheetContent
}

@Composable
private fun AgendaDetailSheet(
    content: AgendaSheetContent,
    onToggleTask: (Task) -> Unit,
    onClose: () -> Unit
) {
    when (content) {
        is AgendaSheetContent.TaskDetail -> TaskDetailSheet(
            task = content.task,
            onToggleTask = onToggleTask,
            onClose = onClose
        )
        is AgendaSheetContent.EventDetail -> EventDetailSheet(
            event = content.event,
            onClose = onClose
        )
    }
}

@Composable
private fun TaskDetailSheet(
    task: Task,
    onToggleTask: (Task) -> Unit,
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
                text = "Due ${dueAt.format(DateTimeDetailFormatter)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "Status: ${task.status.displayName()}",
            style = MaterialTheme.typography.bodyMedium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(onClick = { onToggleTask(task) }) {
                Text(task.status.toggleLabel())
            }
            TextButton(onClick = onClose) {
                Text("Close")
            }
        }
    }
}

@Composable
private fun EventDetailSheet(
    event: CalendarEvent,
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
        TextButton(onClick = onClose) {
            Text("Close")
        }
    }
}

@Composable
private fun AgendaLoading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun AgendaError(error: Throwable) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = error.localizedMessage ?: "Something went wrong",
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun AgendaEmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("No agenda items available")
    }
}

private fun TaskStatus.displayName(): String = when (this) {
    TaskStatus.Pending -> "Pending"
    TaskStatus.InProgress -> "In progress"
    TaskStatus.Completed -> "Completed"
}

private fun TaskStatus.toggleLabel(): String = when (this) {
    TaskStatus.Completed -> "Mark as pending"
    else -> "Mark as complete"
}

private fun eventTimeRange(event: CalendarEvent): String {
    val sameDay = event.start.toLocalDate() == event.end.toLocalDate()
    return if (sameDay) {
        val date = event.start.format(DayFormatter)
        val startTime = event.start.format(TimeOnlyFormatter)
        val endTime = event.end.format(TimeOnlyFormatter)
        "$date · $startTime – $endTime"
    } else {
        "${event.start.format(DateTimeDetailFormatter)} – ${event.end.format(DateTimeDetailFormatter)}"
    }
}

private fun AgendaTab.displayLabel(): String = when (this) {
    AgendaTab.Daily -> "Daily"
    AgendaTab.Weekly -> "Weekly"
    AgendaTab.Monthly -> "Monthly"
}

private fun AgendaTab.accessibleLabel(): String = when (this) {
    AgendaTab.Daily -> "Daily agenda tab"
    AgendaTab.Weekly -> "Weekly agenda tab"
    AgendaTab.Monthly -> "Monthly agenda tab"
}

private fun periodLabel(period: AgendaPeriod): String = when (period) {
    is AgendaPeriod.Day -> DayFormatter.format(period.date)
    is AgendaPeriod.Week -> {
        val start = WeekFormatter.format(period.start)
        val end = WeekFormatter.format(period.end)
        "$start – $end"
    }
    is AgendaPeriod.Month -> MonthFormatter.format(
        YearMonth.of(period.year, period.month).atDay(1)
    )
}

private fun LocalDate.startOfWeek(): LocalDate {
    var date = this
    while (date.dayOfWeek != DayOfWeek.MONDAY) {
        date = date.minusDays(1)
    }
    return date
}

private fun Task.accessibleDescription(): String {
    val statusText = when (status) {
        TaskStatus.Pending -> "Pending"
        TaskStatus.InProgress -> "In progress"
        TaskStatus.Completed -> "Completed"
    }
    return "$statusText task: $title"
}

@Composable
private fun EmptySectionMessage(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

