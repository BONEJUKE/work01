package com.example.calendar.ui

import android.content.SharedPreferences
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPermissionPromptTrackerTest {

    private val clock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC)

    @Test
    fun `suppresses prompt with exponential backoff`() {
        val prefs = InMemoryPreferences()
        val tracker = NotificationPermissionPromptTracker(prefs, clock)

        assertTrue(tracker.shouldShow())

        tracker.recordDismiss()
        assertFalse(tracker.shouldShow(clock.instant()))
        advanceTime(prefs, Duration.ofDays(1))
        assertTrue(tracker.shouldShow(clock.instant()))

        tracker.recordInteraction()
        assertFalse(tracker.shouldShow(clock.instant()))
        advanceTime(prefs, Duration.ofDays(3))
        assertTrue(tracker.shouldShow(clock.instant()))

        tracker.recordDismiss()
        assertFalse(tracker.shouldShow(clock.instant()))
        advanceTime(prefs, Duration.ofDays(14))
        assertTrue(tracker.shouldShow(clock.instant()))
    }

    @Test
    fun `clearing suppression resets counters`() {
        val prefs = InMemoryPreferences()
        val tracker = NotificationPermissionPromptTracker(prefs, clock)

        tracker.recordDismiss()
        assertFalse(tracker.shouldShow(clock.instant()))

        tracker.clearSuppression()
        assertTrue(tracker.shouldShow(clock.instant()))
    }

    private fun advanceTime(prefs: InMemoryPreferences, duration: Duration) {
        val next = prefs.map["next_eligible"] as? Long ?: 0L
        prefs.map["next_eligible"] = next - duration.seconds
    }
}

private class InMemoryPreferences : SharedPreferences {
    val map = mutableMapOf<String, Any?>()

    override fun getAll(): MutableMap<String, *> = map

    override fun getString(key: String?, defValue: String?): String? = map[key] as? String ?: defValue

    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
        (map[key] as? MutableSet<String>) ?: defValues

    override fun getInt(key: String?, defValue: Int): Int = map[key] as? Int ?: defValue

    override fun getLong(key: String?, defValue: Long): Long = map[key] as? Long ?: defValue

    override fun getFloat(key: String?, defValue: Float): Float = map[key] as? Float ?: defValue

    override fun getBoolean(key: String?, defValue: Boolean): Boolean = map[key] as? Boolean ?: defValue

    override fun contains(key: String?): Boolean = map.containsKey(key)

    override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
        override fun putString(key: String?, value: String?): SharedPreferences.Editor = apply { if (key != null) map[key] = value }

        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = apply {
            if (key != null) map[key] = values
        }

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor = apply { if (key != null) map[key] = value }

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor = apply { if (key != null) map[key] = value }

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = apply { if (key != null) map[key] = value }

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = apply { if (key != null) map[key] = value }

        override fun remove(key: String?): SharedPreferences.Editor = apply { if (key != null) map.remove(key) }

        override fun clear(): SharedPreferences.Editor = apply { map.clear() }

        override fun commit(): Boolean = true

        override fun apply() {}
    }

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
}
