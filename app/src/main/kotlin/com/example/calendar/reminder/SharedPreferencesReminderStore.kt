package com.example.calendar.reminder

import android.content.SharedPreferences
import com.example.calendar.data.Reminder
import java.time.LocalDateTime
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

private const val KEY_PREFIX = "reminder-"

class SharedPreferencesReminderStore(
    private val preferences: SharedPreferences
) : ReminderStore {

    override fun write(baseId: String, reminders: List<StoredReminder>) {
        if (reminders.isEmpty()) {
            remove(baseId)
            return
        }

        preferences.edit()
            .putString(key(baseId), reminders.toJson())
            .apply()
    }

    override fun read(baseId: String): List<StoredReminder> {
        val serialized = preferences.getString(key(baseId), null) ?: return emptyList()
        return serialized.fromJson()
    }

    override fun readAll(): Map<String, List<StoredReminder>> {
        return preferences.all.mapNotNull { (key, value) ->
            if (!key.startsWith(KEY_PREFIX)) return@mapNotNull null
            val baseId = key.removePrefix(KEY_PREFIX)
            val serialized = value as? String ?: return@mapNotNull null
            val reminders = serialized.fromJson()
            baseId to reminders
        }.toMap()
    }

    override fun remove(baseId: String) {
        preferences.edit()
            .remove(key(baseId))
            .apply()
    }

    private fun key(baseId: String): String = KEY_PREFIX + baseId
}

private fun List<StoredReminder>.toJson(): String {
    val array = JSONArray()
    forEach { reminder ->
        array.put(
            JSONObject().apply {
                put("id", reminder.id)
                put("triggerAt", reminder.triggerAt.toString())
                put("minutesBefore", reminder.reminder.minutesBefore)
                put("allowSnooze", reminder.reminder.allowSnooze)
                put("payloadTitle", reminder.payload.title)
                put("payloadMessage", reminder.payload.message)
                put("payloadDeepLink", reminder.payload.deepLink)
                put("payloadAllowSnooze", reminder.payload.allowSnooze)
                put("payloadTaskId", reminder.payload.taskId)
                put("payloadBaseId", reminder.payload.baseId)
                put("payloadSnoozeMinutes", reminder.payload.snoozeMinutes)
            }
        )
    }
    return array.toString()
}

private fun String.fromJson(): List<StoredReminder> {
    return try {
        val array = JSONArray(this)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    StoredReminder(
                        id = item.getString("id"),
                        triggerAt = LocalDateTime.parse(item.getString("triggerAt")),
                        reminder = Reminder(
                            minutesBefore = item.getLong("minutesBefore"),
                            allowSnooze = item.optBoolean("allowSnooze", true)
                        ),
                        payload = ReminderPayload(
                            title = item.optString("payloadTitle"),
                            message = item.optString("payloadMessage"),
                            deepLink = item.optString("payloadDeepLink"),
                            allowSnooze = item.optBoolean("payloadAllowSnooze", false),
                            taskId = item.optString("payloadTaskId").takeIf { it.isNotBlank() },
                            baseId = item.optString("payloadBaseId"),
                            snoozeMinutes = item.optLong(
                                "payloadSnoozeMinutes",
                                ReminderPayload.DEFAULT_SNOOZE_MINUTES
                            )
                        )
                    )
                )
            }
        }
    } catch (_: JSONException) {
        emptyList()
    }
}
