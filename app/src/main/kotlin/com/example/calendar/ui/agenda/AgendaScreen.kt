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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
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
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch
import kotlin.math.min

private val DayFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
private val WeekFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
private val MonthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
private val DateTimeDetailFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a", Locale.getDefault())
private val TimeOnlyFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
private val WeekdayChipFormatter = DateTimeFormatter.ofPattern("EEE d", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaRoute(
    viewModel: AgendaViewModel,
    onEventClick: (CalendarEvent) -> Unit = {},
    onTaskClick: (Task) -> Unit = {},
    onEventEdit: (CalendarEvent) -> Unit = {},
    onTaskEdit: (Task) -> Unit = {}
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
    var showQuickAddDialog by rememberSaveable { mutableStateOf(false) }
    var isQuickAddSaving by remember { mutableStateOf(false) }
    var quickAddType by rememberSaveable { mutableStateOf(QuickAddType.Task) }
    var quickAddTitle by rememberSaveable { mutableStateOf("") }
    var quickAddTitleError by remember { mutableStateOf(false) }

    val openTaskSheet: (Task) -> Unit = { task ->
        sheetContent = AgendaSheetContent.TaskDetail(task)
        onTaskClick(task)
    }
    val openEventSheet: (CalendarEvent) -> Unit = { event ->
        sheetContent = AgendaSheetContent.EventDetail(event)
        onEventClick(event)
    }

    val onDateSelected: (LocalDate) -> Unit = { date ->
        updateFocus(date)
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

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userMessage) {
        val message = uiState.userMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message.message)
            viewModel.consumeUserMessage()
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
        onWeekDaySelected = onDateSelected,
        onMonthDaySelected = onDateSelected,
        focusedDay = focusedDay,
        period = currentPeriod,
        snackbarHostState = snackbarHostState,
        onQuickAddClick = {
            quickAddTitle = ""
            quickAddTitleError = false
            quickAddType = if (selectedTab == AgendaTab.Monthly) QuickAddType.Event else QuickAddType.Task
            showQuickAddDialog = true
        }
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
                onDeleteTask = { task ->
                    viewModel.deleteTask(task)
                    hideSheet()
                },
                onDeleteEvent = { event ->
                    viewModel.deleteEvent(event)
                    hideSheet()
                },
                onEditTask = { task ->
                    onTaskEdit(task)
                    hideSheet()
                },
                onEditEvent = { event ->
                    onEventEdit(event)
                    hideSheet()
                },
                onClose = hideSheet
            )
        }
    }

    if (showQuickAddDialog) {
        QuickAddDialog(
            title = quickAddTitle,
            onTitleChange = {
                quickAddTitle = it
                if (quickAddTitleError && it.isNotBlank()) {
                    quickAddTitleError = false
                }
            },
            isTitleError = quickAddTitleError,
            selectedType = quickAddType,
            onSelectType = { quickAddType = it },
            focusedDay = focusedDay,
            period = currentPeriod,
            isSaving = isQuickAddSaving,
            onDismiss = {
                if (!isQuickAddSaving) {
                    showQuickAddDialog = false
                }
            },
            onConfirm = {
                val trimmed = quickAddTitle.trim()
                if (trimmed.isEmpty()) {
                    quickAddTitleError = true
                    return@QuickAddDialog
                }
                coroutineScope.launch {
                    isQuickAddSaving = true
                    val result = when (quickAddType) {
                        QuickAddType.Task -> viewModel.quickAddTask(
                            title = trimmed,
                            period = currentPeriod,
                            focusedDay = focusedDay
                        )
                        QuickAddType.Event -> viewModel.quickAddEvent(
                            title = trimmed,
                            focusedDay = focusedDay
                        )
                    }
                    isQuickAddSaving = false
                    if (result.isSuccess) {
                        showQuickAddDialog = false
                        quickAddTitle = ""
                        quickAddTitleError = false
                    }
                }
            }
        )
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
    onWeekDaySelected: (LocalDate) -> Unit,
    onMonthDaySelected: (LocalDate) -> Unit,
    focusedDay: LocalDate,
    period: AgendaPeriod,
    snackbarHostState: SnackbarHostState,
    onQuickAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                        selectedTab = selectedTab,
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
}

@Composable
private fun AgendaFab(onClick: () -> Unit, icon: ImageVector = Icons.Filled.Add) {
    FloatingActionButton(onClick = onClick) {
        Icon(imageVector = icon, contentDescription = "빠른 추가")
    }
}

private enum class QuickAddType {
    Task,
    Event
}

@Composable
private fun QuickAddDialog(
    title: String,
    onTitleChange: (String) -> Unit,
    isTitleError: Boolean,
    selectedType: QuickAddType,
    onSelectType: (QuickAddType) -> Unit,
    focusedDay: LocalDate,
    period: AgendaPeriod,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val helperText = remember(selectedType, focusedDay, period) {
        when (selectedType) {
            QuickAddType.Task -> "${DayFormatter.format(focusedDay)} 기준으로 ${periodSummary(period)} 할 일을 오전 9시에 추가합니다."
            QuickAddType.Event -> "${DayFormatter.format(focusedDay)} 오전 9시에 시작하는 1시간짜리 이벤트를 만듭니다."
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "빠른 추가") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                QuickAddTypeRow(selectedType = selectedType, onSelectType = onSelectType)
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (selectedType == QuickAddType.Task) "할 일 제목" else "이벤트 제목") },
                    isError = isTitleError,
                    singleLine = true,
                    supportingText = {
                        if (isTitleError) {
                            Text(text = "제목을 입력해 주세요")
                        }
                    }
                )
                Text(
                    text = helperText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isSaving) {
                Text(if (isSaving) "저장 중..." else "저장")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("취소")
            }
        }
    )
}

@Composable
private fun QuickAddTypeRow(
    selectedType: QuickAddType,
    onSelectType: (QuickAddType) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        QuickAddType.values().forEach { type ->
            FilterChip(
                selected = selectedType == type,
                onClick = { onSelectType(type) },
                label = { Text(if (type == QuickAddType.Task) "할 일" else "이벤트") }
            )
        }
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
                    text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
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
    selectedTab: AgendaTab,
    onToggleTask: (Task) -> Unit,
    onTaskClick: (Task) -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    when (selectedTab) {
        AgendaTab.Daily -> DayAgenda(
            snapshot = snapshot,
            onToggleTask = onToggleTask,
            onTaskClick = onTaskClick,
            onEventClick = onEventClick,
            modifier = modifier
        )

        AgendaTab.Weekly -> WeekAgenda(
            snapshot = snapshot,
            onToggleTask = onToggleTask,
            onTaskClick = onTaskClick,
            onEventClick = onEventClick,
            modifier = modifier
        )

        AgendaTab.Monthly -> MonthAgenda(
            snapshot = snapshot,
            onToggleTask = onToggleTask,
            onTaskClick = onTaskClick,
            onEventClick = onEventClick,
            modifier = modifier
        )
    }
}

@Composable
private fun DayAgenda(
    snapshot: AgendaSnapshot,
    onToggleTask: (Task) -> Unit,
    onTaskClick: (Task) -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    AgendaList(
        snapshot = snapshot,
        summaryTitle = "Daily overview",
        summaryPeriod = DayFormatter.format(snapshot.rangeStart),
        onToggleTask = onToggleTask,
        onTaskClick = onTaskClick,
        onEventClick = onEventClick,
        modifier = modifier
    )
}

@Composable
private fun WeekAgenda(
    snapshot: AgendaSnapshot,
    onToggleTask: (Task) -> Unit,
    onTaskClick: (Task) -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val periodLabel = buildString {
        append(WeekFormatter.format(snapshot.rangeStart))
        snapshot.rangeEnd.takeIf { it != snapshot.rangeStart }?.let { end ->
            append(" – ")
            append(WeekFormatter.format(end))
        }
    }
    AgendaList(
        snapshot = snapshot,
        summaryTitle = "Weekly focus",
        summaryPeriod = periodLabel,
        onToggleTask = onToggleTask,
        onTaskClick = onTaskClick,
        onEventClick = onEventClick,
        modifier = modifier
    )
}

@Composable
private fun MonthAgenda(
    snapshot: AgendaSnapshot,
    onToggleTask: (Task) -> Unit,
    onTaskClick: (Task) -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val monthLabel = MonthFormatter.format(snapshot.rangeStart)
    AgendaList(
        snapshot = snapshot,
        summaryTitle = "Monthly outlook",
        summaryPeriod = monthLabel,
        onToggleTask = onToggleTask,
        onTaskClick = onTaskClick,
        onEventClick = onEventClick,
        modifier = modifier
    )
}

@Composable
private fun AgendaList(
    snapshot: AgendaSnapshot,
    summaryTitle: String,
    summaryPeriod: String,
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
            AgendaSummaryCard(
                title = summaryTitle,
                period = summaryPeriod,
                snapshot = snapshot
            )
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
private fun AgendaSummaryCard(
    title: String,
    period: String,
    snapshot: AgendaSnapshot,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
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
    val actionLabel = if (task.status.isDone()) "Mark as pending" else "Mark as complete"
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
                stateDescription = if (task.status.isDone()) "Completed" else "Not completed"
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
                    text = "Due ${dueAt.format(DateTimeDetailFormatter)}",
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
}

@Composable
private fun AgendaDetailSheet(
    content: AgendaSheetContent,
    onToggleTask: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onDeleteEvent: (CalendarEvent) -> Unit,
    onEditTask: (Task) -> Unit,
    onEditEvent: (CalendarEvent) -> Unit,
    onClose: () -> Unit
) {
    when (content) {
        is AgendaSheetContent.TaskDetail -> TaskDetailSheet(
            task = content.task,
            onToggleTask = onToggleTask,
            onDeleteTask = onDeleteTask,
            onEditTask = onEditTask,
            onClose = onClose
        )
        is AgendaSheetContent.EventDetail -> EventDetailSheet(
            event = content.event,
            onDeleteEvent = onDeleteEvent,
            onEditEvent = onEditEvent,
            onClose = onClose
        )
    }
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
                text = "Due ${dueAt.format(DateTimeDetailFormatter)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "Status: ${task.status.displayName()}",
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
                Text("Edit")
            }
            TextButton(
                onClick = { onDeleteTask(task) },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete")
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
                Text("Edit")
            }
            TextButton(
                onClick = { onDeleteEvent(event) },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete")
            }
            TextButton(onClick = onClose) {
                Text("Close")
            }
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

