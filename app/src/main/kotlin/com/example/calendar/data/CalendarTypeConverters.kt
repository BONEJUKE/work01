package com.example.calendar.data

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class CalendarTypeConverters {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    @TypeConverter
    fun fromUuid(value: UUID?): String? = value?.toString()

    @TypeConverter
    fun toUuid(value: String?): UUID? = value?.let(UUID::fromString)

    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? = value?.format(dateFormatter)

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it, dateFormatter) }

    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): String? = value?.format(dateTimeFormatter)

    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? =
        value?.let { LocalDateTime.parse(it, dateTimeFormatter) }

    @TypeConverter
    fun fromTaskStatus(value: TaskStatus?): String? = value?.name

    @TypeConverter
    fun toTaskStatus(value: String?): TaskStatus? = value?.let(TaskStatus::valueOf)

    @TypeConverter
    fun fromAgendaPeriod(value: AgendaPeriod?): String? = when (value) {
        is AgendaPeriod.Day -> "DAY;${value.date.format(dateFormatter)}"
        is AgendaPeriod.Week -> "WEEK;${value.start.format(dateFormatter)}"
        is AgendaPeriod.Month -> "MONTH;${value.year};${value.month}"
        null -> null
    }

    @TypeConverter
    fun toAgendaPeriod(value: String?): AgendaPeriod? {
        if (value.isNullOrBlank()) return null
        val parts = value.split(';')
        if (parts.isEmpty()) return null
        return when (parts[0]) {
            "DAY" -> parts.getOrNull(1)?.let { LocalDate.parse(it, dateFormatter) }?.let(AgendaPeriod::Day)
            "WEEK" -> parts.getOrNull(1)?.let { LocalDate.parse(it, dateFormatter) }?.let(AgendaPeriod::Week)
            "MONTH" -> {
                val year = parts.getOrNull(1)?.toIntOrNull()
                val month = parts.getOrNull(2)?.toIntOrNull()
                if (year != null && month != null) AgendaPeriod.Month(year, month) else null
            }
            else -> null
        }
    }

    @TypeConverter
    fun fromReminders(value: List<Reminder>?): String? = value
        ?.joinToString(separator = "|") { reminder ->
            listOf(reminder.minutesBefore.toString(), if (reminder.allowSnooze) "1" else "0").joinToString(",")
        }

    @TypeConverter
    fun toReminders(value: String?): List<Reminder> {
        if (value.isNullOrBlank()) return emptyList()
        return value.split('|').mapNotNull { entry ->
            val parts = entry.split(',')
            val minutes = parts.getOrNull(0)?.toLongOrNull() ?: return@mapNotNull null
            val allowSnooze = parts.getOrNull(1)?.let { it == "1" || it.equals("true", ignoreCase = true) } ?: true
            Reminder(minutesBefore = minutes, allowSnooze = allowSnooze)
        }
    }

    @TypeConverter
    fun fromTags(value: Set<String>?): String? = value?.joinToString(separator = "|")

    @TypeConverter
    fun toTags(value: String?): Set<String> {
        if (value.isNullOrBlank()) return emptySet()
        return value.split('|').mapNotNull { it.trim().takeIf(String::isNotEmpty) }.toSet()
    }

    @TypeConverter
    fun fromRecurrence(value: Recurrence?): String? = value?.let {
        buildString {
            append(it.rule.name)
            append(';')
            append(it.interval)
            append(';')
            append(it.occurrences ?: "")
        }
    }

    @TypeConverter
    fun toRecurrence(value: String?): Recurrence? {
        if (value.isNullOrBlank()) return null
        val parts = value.split(';')
        val rule = parts.getOrNull(0)?.let(RecurrenceRule::valueOf) ?: return null
        val interval = parts.getOrNull(1)?.toIntOrNull() ?: 1
        val occurrences = parts.getOrNull(2)?.takeIf { it.isNotBlank() }?.toIntOrNull()
        return Recurrence(rule = rule, interval = interval, occurrences = occurrences)
    }

    @TypeConverter
    fun fromRecurrenceExceptions(value: List<RecurrenceException>?): String? {
        if (value.isNullOrEmpty()) return null
        val array = JSONArray()
        value.forEach { exception ->
            array.put(
                JSONObject().apply {
                    put("originalStart", exception.originalStart.format(dateTimeFormatter))
                    put("isCancelled", exception.isCancelled)
                    put("overrideStart", exception.overrideStart?.format(dateTimeFormatter))
                    put("overrideEnd", exception.overrideEnd?.format(dateTimeFormatter))
                    put("overrideTitle", exception.overrideTitle)
                    put("overrideDescription", exception.overrideDescription)
                    put("overrideLocation", exception.overrideLocation)
                    put("overrideRecurrence", fromRecurrence(exception.nextRecurrenceOverride))
                }
            )
        }
        return array.toString()
    }

    @TypeConverter
    fun toRecurrenceExceptions(value: String?): List<RecurrenceException> {
        if (value.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(value)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val originalStart = item.getString("originalStart")
                    val overrideStart = item.optString("overrideStart").takeIf { it.isNotBlank() }
                    val overrideEnd = item.optString("overrideEnd").takeIf { it.isNotBlank() }
                    val recurrence = item.optString("overrideRecurrence").takeIf { it.isNotBlank() }
                    add(
                        RecurrenceException(
                            originalStart = LocalDateTime.parse(originalStart, dateTimeFormatter),
                            isCancelled = item.optBoolean("isCancelled", false),
                            overrideStart = overrideStart?.let { LocalDateTime.parse(it, dateTimeFormatter) },
                            overrideEnd = overrideEnd?.let { LocalDateTime.parse(it, dateTimeFormatter) },
                            overrideTitle = item.optString("overrideTitle").takeIf { it.isNotBlank() },
                            overrideDescription = item.optString("overrideDescription").takeIf { it.isNotBlank() },
                            overrideLocation = item.optString("overrideLocation").takeIf { it.isNotBlank() },
                            nextRecurrenceOverride = recurrence?.let(::toRecurrence)
                        )
                    )
                }
            }
        } catch (_: JSONException) {
            emptyList()
        }
    }
}
