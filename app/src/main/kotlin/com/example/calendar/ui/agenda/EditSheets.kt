package com.example.calendar.ui.agenda

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.launch
import com.example.calendar.data.CalendarEvent
import com.example.calendar.data.Task

@Composable
fun TaskEditSheet(
    task: Task,
    onSaveTask: suspend (title: String, notes: String?, dueAt: LocalDateTime?) -> Result<Task>,
    onSaved: (Task) -> Unit,
    onClose: () -> Unit
) {
    var title by rememberSaveable(task.id.toString()) { mutableStateOf(task.title) }
    var notes by rememberSaveable(task.id.toString()) { mutableStateOf(task.description.orEmpty()) }
    var dueDateText by rememberSaveable(task.id.toString()) {
        mutableStateOf(task.dueAt?.toLocalDate()?.toString() ?: "")
    }
    var dueTimeText by rememberSaveable(task.id.toString()) {
        mutableStateOf(task.dueAt?.toLocalTime()?.format(QuickAddTimeFormatter) ?: "")
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "할 일 편집",
            style = MaterialTheme.typography.titleLarge
        )
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("제목") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("메모 (선택)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = dueDateText,
            onValueChange = { dueDateText = it },
            label = { Text("마감 날짜 (YYYY-MM-DD)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            supportingText = {
                Text(
                    text = "비워 두면 날짜 없이 저장됩니다.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        )
        OutlinedTextField(
            value = dueTimeText,
            onValueChange = { dueTimeText = it },
            label = { Text("마감 시간 (HH:mm)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            supportingText = {
                Text(
                    text = "날짜 또는 시간을 비워 두면 마감 시간이 제거됩니다.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        )

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
            TextButton(onClick = { if (!isSaving) onClose() }) {
                Text("취소")
            }
            Button(
                onClick = {
                    if (isSaving) return@Button
                    val normalizedTitle = title.trim()
                    if (normalizedTitle.isEmpty()) {
                        errorMessage = "제목을 입력해 주세요."
                        return@Button
                    }
                    val normalizedNotes = notes.trim().takeIf { it.isNotEmpty() }
                    val dateText = dueDateText.trim()
                    val timeText = dueTimeText.trim()

                    val dueAt = if (dateText.isEmpty() && timeText.isEmpty()) {
                        null
                    } else {
                        if (dateText.isEmpty()) {
                            errorMessage = "마감 날짜를 YYYY-MM-DD 형식으로 입력해 주세요."
                            return@Button
                        }
                        val dateResult = runCatching { LocalDate.parse(dateText) }
                        if (dateResult.isFailure) {
                            errorMessage = "마감 날짜는 YYYY-MM-DD 형식이어야 해요."
                            return@Button
                        }
                        if (timeText.isEmpty()) {
                            errorMessage = "마감 시간을 HH:mm 형식으로 입력해 주세요."
                            return@Button
                        }
                        val timeResult = parseInputTime(timeText)
                        if (timeResult.isFailure) {
                            errorMessage = "마감 시간은 HH:mm 형식이어야 해요."
                            return@Button
                        }
                        LocalDateTime.of(dateResult.getOrThrow(), timeResult.getOrThrow())
                    }

                    coroutineScope.launch {
                        isSaving = true
                        errorMessage = null
                        val result = onSaveTask(normalizedTitle, normalizedNotes, dueAt)
                        if (result.isSuccess) {
                            onSaved(result.getOrThrow())
                        } else {
                            errorMessage = result.exceptionOrNull()?.message ?: "저장에 실패했습니다."
                        }
                        isSaving = false
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
fun EventEditSheet(
    event: CalendarEvent,
    onSaveEvent: suspend (
        title: String,
        description: String?,
        location: String?,
        start: LocalDateTime,
        end: LocalDateTime
    ) -> Result<CalendarEvent>,
    onSaved: (CalendarEvent) -> Unit,
    onClose: () -> Unit
) {
    var title by rememberSaveable(event.id.toString()) { mutableStateOf(event.title) }
    var location by rememberSaveable(event.id.toString()) { mutableStateOf(event.location.orEmpty()) }
    var notes by rememberSaveable(event.id.toString()) { mutableStateOf(event.description.orEmpty()) }
    var dateText by rememberSaveable(event.id.toString()) { mutableStateOf(event.start.toLocalDate().toString()) }
    var startTimeText by rememberSaveable(event.id.toString()) {
        mutableStateOf(event.start.toLocalTime().format(QuickAddTimeFormatter))
    }
    var endTimeText by rememberSaveable(event.id.toString()) {
        mutableStateOf(event.end.toLocalTime().format(QuickAddTimeFormatter))
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "일정 편집",
            style = MaterialTheme.typography.titleLarge
        )
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("제목") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("위치 (선택)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("메모 (선택)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = dateText,
            onValueChange = { dateText = it },
            label = { Text("날짜 (YYYY-MM-DD)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = startTimeText,
                onValueChange = { startTimeText = it },
                label = { Text("시작 시간 (HH:mm)") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = endTimeText,
                onValueChange = { endTimeText = it },
                label = { Text("종료 시간 (HH:mm)") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
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
            TextButton(onClick = { if (!isSaving) onClose() }) {
                Text("취소")
            }
            Button(
                onClick = {
                    if (isSaving) return@Button
                    val normalizedTitle = title.trim()
                    if (normalizedTitle.isEmpty()) {
                        errorMessage = "제목을 입력해 주세요."
                        return@Button
                    }
                    val normalizedLocation = location.trim().takeIf { it.isNotEmpty() }
                    val normalizedNotes = notes.trim().takeIf { it.isNotEmpty() }
                    val parsedDate = runCatching { LocalDate.parse(dateText.trim()) }
                    if (parsedDate.isFailure) {
                        errorMessage = "날짜는 YYYY-MM-DD 형식이어야 해요."
                        return@Button
                    }
                    val startResult = parseInputTime(startTimeText.trim())
                    if (startResult.isFailure) {
                        errorMessage = "시작 시간은 HH:mm 형식이어야 해요."
                        return@Button
                    }
                    val endResult = parseInputTime(endTimeText.trim())
                    if (endResult.isFailure) {
                        errorMessage = "종료 시간은 HH:mm 형식이어야 해요."
                        return@Button
                    }

                    val date = parsedDate.getOrThrow()
                    val startDateTime = LocalDateTime.of(date, startResult.getOrThrow())
                    val endDateTime = LocalDateTime.of(date, endResult.getOrThrow())
                    if (endDateTime.isBefore(startDateTime)) {
                        errorMessage = "종료 시간은 시작 시간 이후여야 합니다."
                        return@Button
                    }

                    coroutineScope.launch {
                        isSaving = true
                        errorMessage = null
                        val result = onSaveEvent(
                            normalizedTitle,
                            normalizedNotes,
                            normalizedLocation,
                            startDateTime,
                            endDateTime
                        )
                        if (result.isSuccess) {
                            onSaved(result.getOrThrow())
                        } else {
                            errorMessage = result.exceptionOrNull()?.message ?: "저장에 실패했습니다."
                        }
                        isSaving = false
                    }
                },
                enabled = !isSaving
            ) {
                Text(if (isSaving) "저장 중..." else "저장")
            }
        }
    }
}
