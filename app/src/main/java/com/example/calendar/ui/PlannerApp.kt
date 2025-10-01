package com.example.calendar.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendar.data.PlannerTaskEntity
import com.example.calendar.data.RepeatCadence
import com.example.calendar.ui.theme.CalendarPlannerTheme
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun PlannerApp(
    uiState: PlannerUiState,
    onSelectDate: (LocalDate) -> Unit,
    onSelectScope: (PlannerScope) -> Unit,
    onToggleTask: (PlannerTaskEntity) -> Unit,
    onAddTask: (String, String, LocalDate, LocalTime, Int, RepeatCadence) -> Unit
) {
    CalendarPlannerTheme {
        val showTaskDialog = rememberSaveable { mutableStateOf(false) }

        Scaffold(
            floatingActionButton = {
                FloatingActionButton(onClick = { showTaskDialog.value = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Task")
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                PlannerHeader(uiState, onSelectDate, onSelectScope)
                Spacer(modifier = Modifier.height(16.dp))
                TaskList(uiState.tasks, onToggleTask)
            }
        }

        if (showTaskDialog.value) {
            TaskEditorDialog(
                initialDate = uiState.selectedDate,
                onDismiss = { showTaskDialog.value = false },
                onSave = { title, description, date, time, reminder, cadence ->
                    onAddTask(title, description, date, time, reminder, cadence)
                    showTaskDialog.value = false
                }
            )
        }
    }
}

@Composable
private fun PlannerHeader(
    uiState: PlannerUiState,
    onSelectDate: (LocalDate) -> Unit,
    onSelectScope: (PlannerScope) -> Unit
) {
    Column {
        Text(
            text = "나의 일정 플래너",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(8.dp))
        DateSelector(uiState.selectedDate, onSelectDate)
        Spacer(modifier = Modifier.height(12.dp))
        ScopeSelector(uiState.selectedScope, onSelectScope)
    }
}

@Composable
private fun DateSelector(selectedDate: LocalDate, onSelectDate: (LocalDate) -> Unit) {
    val formatter = remember { DateTimeFormatter.ofPattern("yyyy년 MM월 dd일") }
    val showDialog = remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )

    Column {
        Text(text = "선택된 날짜", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(4.dp))
        Button(onClick = { showDialog.value = true }) {
            Text(text = selectedDate.format(formatter))
        }
    }

    if (showDialog.value) {
        DatePickerDialog(
            onDismissRequest = { showDialog.value = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                            onSelectDate(date)
                        }
                        showDialog.value = false
                    }
                ) {
                    Text("확인")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog.value = false }) {
                    Text("취소")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun ScopeSelector(selectedScope: PlannerScope, onSelectScope: (PlannerScope) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PlannerScope.values().forEach { scope ->
            FilterChip(
                selected = selectedScope == scope,
                onClick = { onSelectScope(scope) },
                label = { Text(scopeLabel(scope)) },
                colors = FilterChipDefaults.filterChipColors()
            )
        }
    }
}

private fun scopeLabel(scope: PlannerScope): String = when (scope) {
    PlannerScope.DAY -> "하루"
    PlannerScope.WEEK -> "1주"
    PlannerScope.MONTH -> "한 달"
}

@Composable
private fun TaskList(tasks: List<PlannerTaskEntity>, onToggleTask: (PlannerTaskEntity) -> Unit) {
    if (tasks.isEmpty()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "등록된 일정이 없습니다.", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "오른쪽 아래 + 버튼을 눌러 할 일을 추가하세요.", fontSize = 14.sp)
            }
        }
        return
    }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("MM월 dd일 (E)") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("a hh:mm") }
    val zone = remember { ZoneId.systemDefault() }

    val groupedTasks = tasks.groupBy {
        LocalDateTime.ofInstant(Instant.ofEpochMilli(it.scheduledAtEpochMillis), zone).toLocalDate()
    }.toSortedMap()

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        groupedTasks.forEach { (date, dayTasks) ->
            item(key = date) {
                Text(
                    text = date.format(dateFormatter),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            items(dayTasks, key = { it.id }) { task ->
                TaskRow(task = task, timeFormatter = timeFormatter, zone = zone, onToggleTask = onToggleTask)
            }
        }
    }
}

@Composable
private fun TaskRow(
    task: PlannerTaskEntity,
    timeFormatter: DateTimeFormatter,
    zone: ZoneId,
    onToggleTask: (PlannerTaskEntity) -> Unit
) {
    val dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(task.scheduledAtEpochMillis), zone)
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = task.isCompleted, onCheckedChange = { onToggleTask(task) })
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(text = task.title, style = MaterialTheme.typography.titleMedium)
                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = dateTime.format(timeFormatter), style = MaterialTheme.typography.labelMedium)
                if (task.repeatCadence != RepeatCadence.NONE) {
                    Text(
                        text = "반복: ${repeatLabel(task.repeatCadence)}",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

private fun repeatLabel(cadence: RepeatCadence): String = when (cadence) {
    RepeatCadence.NONE -> "없음"
    RepeatCadence.DAILY -> "매일"
    RepeatCadence.WEEKLY -> "매주"
    RepeatCadence.MONTHLY -> "매월"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskEditorDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onSave: (String, String, LocalDate, LocalTime, Int, RepeatCadence) -> Unit
) {
    val titleState = rememberSaveable { mutableStateOf("") }
    val descriptionState = rememberSaveable { mutableStateOf("") }
    val reminderState = rememberSaveable { mutableStateOf("30") }
    var selectedDate by rememberSaveable { mutableStateOf(initialDate) }
    val timePickerState = rememberTimePickerState(hour = LocalTime.now().hour, minute = LocalTime.now().minute)
    val showDatePicker = remember { mutableStateOf(false) }
    var selectedCadence by rememberSaveable { mutableStateOf(RepeatCadence.NONE) }

    val formatter = remember { DateTimeFormatter.ofPattern("yyyy년 MM월 dd일") }

    if (showDatePicker.value) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker.value = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker.value = false
                }) {
                    Text("선택")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker.value = false }) {
                    Text("취소")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "일정 추가") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = titleState.value,
                    onValueChange = { titleState.value = it },
                    label = { Text("제목") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = descriptionState.value,
                    onValueChange = { descriptionState.value = it },
                    label = { Text("설명") }
                )
                Button(onClick = { showDatePicker.value = true }) {
                    Text(text = selectedDate.format(formatter))
                }
                TimePicker(state = timePickerState)
                OutlinedTextField(
                    value = reminderState.value,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                            reminderState.value = newValue.take(3)
                        }
                    },
                    label = { Text("알림 (분 전)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                RepeatCadenceSelector(selectedCadence) { selectedCadence = it }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (titleState.value.isBlank()) return@TextButton
                val reminderMinutes = reminderState.value.toIntOrNull() ?: 0
                val time = LocalTime.of(timePickerState.hour, timePickerState.minute)
                onSave(
                    titleState.value,
                    descriptionState.value,
                    selectedDate,
                    time,
                    reminderMinutes,
                    selectedCadence
                )
                titleState.value = ""
                descriptionState.value = ""
                reminderState.value = "30"
                selectedCadence = RepeatCadence.NONE
                selectedDate = initialDate
                onDismiss()
            }) {
                Text("저장")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("닫기") }
        }
    )
}

@Composable
private fun RepeatCadenceSelector(selected: RepeatCadence, onSelect: (RepeatCadence) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "반복 설정", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RepeatCadence.values().forEach { cadence ->
                FilterChip(
                    selected = selected == cadence,
                    onClick = { onSelect(cadence) },
                    label = { Text(repeatLabel(cadence)) }
                )
            }
        }
    }
}
