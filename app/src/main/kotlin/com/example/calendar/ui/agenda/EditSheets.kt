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
            text = AgendaText.TaskEdit.title,
            style = MaterialTheme.typography.titleLarge
        )
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(AgendaText.Common.titleLabel) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text(AgendaText.Common.notesOptionalLabel) },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = dueDateText,
            onValueChange = { dueDateText = it },
            label = { Text(AgendaText.TaskEdit.dueDateLabel) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            supportingText = {
                Text(
                    text = AgendaText.TaskEdit.dueDateHint,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        )
        OutlinedTextField(
            value = dueTimeText,
            onValueChange = { dueTimeText = it },
            label = { Text(AgendaText.TaskEdit.dueTimeLabel) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            supportingText = {
                Text(
                    text = AgendaText.TaskEdit.dueTimeHint,
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
                Text(AgendaText.Common.cancel)
            }
            Button(
                onClick = {
                    if (isSaving) return@Button
                    val normalizedTitle = title.trim()
                    if (normalizedTitle.isEmpty()) {
                        errorMessage = AgendaText.Common.titleRequired
                        return@Button
                    }
                    val normalizedNotes = notes.trim().takeIf { it.isNotEmpty() }
                    val dateText = dueDateText.trim()
                    val timeText = dueTimeText.trim()

                    val dueAt = if (dateText.isEmpty() && timeText.isEmpty()) {
                        null
                    } else {
                        if (dateText.isEmpty()) {
                            errorMessage = AgendaText.TaskEdit.dueDateRequired
                            return@Button
                        }
                        val dateResult = runCatching { LocalDate.parse(dateText) }
                        if (dateResult.isFailure) {
                            errorMessage = AgendaText.TaskEdit.dueDateFormatError
                            return@Button
                        }
                        if (timeText.isEmpty()) {
                            errorMessage = AgendaText.TaskEdit.dueTimeRequired
                            return@Button
                        }
                        val timeResult = parseInputTime(timeText)
                        if (timeResult.isFailure) {
                            errorMessage = AgendaText.TaskEdit.dueTimeFormatError
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
                            errorMessage = result.exceptionOrNull()?.message ?: AgendaText.Common.saveFailed
                        }
                        isSaving = false
                    }
                },
                enabled = !isSaving
            ) {
                Text(if (isSaving) AgendaText.Common.saving else AgendaText.Common.save)
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
            text = AgendaText.EventEdit.title,
            style = MaterialTheme.typography.titleLarge
        )
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(AgendaText.Common.titleLabel) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text(AgendaText.Common.locationOptionalLabel) },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text(AgendaText.Common.notesOptionalLabel) },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = dateText,
            onValueChange = { dateText = it },
            label = { Text(AgendaText.EventEdit.dateLabel) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = startTimeText,
                onValueChange = { startTimeText = it },
                label = { Text(AgendaText.EventEdit.startTimeLabel) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = endTimeText,
                onValueChange = { endTimeText = it },
                label = { Text(AgendaText.EventEdit.endTimeLabel) },
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
                Text(AgendaText.Common.cancel)
            }
            Button(
                onClick = {
                    if (isSaving) return@Button
                    val normalizedTitle = title.trim()
                    if (normalizedTitle.isEmpty()) {
                        errorMessage = AgendaText.Common.titleRequired
                        return@Button
                    }
                    val normalizedLocation = location.trim().takeIf { it.isNotEmpty() }
                    val normalizedNotes = notes.trim().takeIf { it.isNotEmpty() }
                    val parsedDate = runCatching { LocalDate.parse(dateText.trim()) }
                    if (parsedDate.isFailure) {
                        errorMessage = AgendaText.EventEdit.dateFormatError
                        return@Button
                    }
                    val startResult = parseInputTime(startTimeText.trim())
                    if (startResult.isFailure) {
                        errorMessage = AgendaText.EventEdit.startTimeFormatError
                        return@Button
                    }
                    val endResult = parseInputTime(endTimeText.trim())
                    if (endResult.isFailure) {
                        errorMessage = AgendaText.EventEdit.endTimeFormatError
                        return@Button
                    }

                    val date = parsedDate.getOrThrow()
                    val startDateTime = LocalDateTime.of(date, startResult.getOrThrow())
                    val endDateTime = LocalDateTime.of(date, endResult.getOrThrow())
                    if (endDateTime.isBefore(startDateTime)) {
                        errorMessage = AgendaText.EventEdit.endTimeBeforeStart
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
                            errorMessage = result.exceptionOrNull()?.message ?: AgendaText.Common.saveFailed
                        }
                        isSaving = false
                    }
                },
                enabled = !isSaving
            ) {
                Text(if (isSaving) AgendaText.Common.saving else AgendaText.Common.save)
            }
        }
    }
}
