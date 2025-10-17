package com.example.calendar.ui.agenda

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

internal val QuickAddTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("H:mm", Locale.KOREAN)

internal fun parseInputTime(value: String): Result<LocalTime> {
    return runCatching { LocalTime.parse(value.trim(), QuickAddTimeFormatter) }
}
