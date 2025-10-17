package com.example.calendar.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.time.Clock
import java.time.Duration
import java.time.Instant

private const val PREF_NAME = "notification_permission_prompt"
private const val KEY_DISMISS_COUNT = "dismiss_count"
private const val KEY_NEXT_ELIGIBLE = "next_eligible"

class NotificationPermissionPromptTracker @VisibleForTesting constructor(
    private val preferences: SharedPreferences,
    private val clock: Clock = Clock.systemUTC()
) {
    fun shouldShow(now: Instant = clock.instant()): Boolean {
        val nextEligible = preferences.getLong(KEY_NEXT_ELIGIBLE, 0L)
        return now.epochSecond >= nextEligible
    }

    fun recordDismiss(now: Instant = clock.instant()) {
        updateNextEligible(now)
    }

    fun recordInteraction(now: Instant = clock.instant()) {
        updateNextEligible(now)
    }

    fun clearSuppression() {
        preferences.edit()
            .remove(KEY_DISMISS_COUNT)
            .remove(KEY_NEXT_ELIGIBLE)
            .apply()
    }

    private fun updateNextEligible(now: Instant) {
        val count = preferences.getInt(KEY_DISMISS_COUNT, 0) + 1
        val delay = when {
            count <= 1 -> Duration.ofDays(1)
            count == 2 -> Duration.ofDays(3)
            else -> Duration.ofDays(14)
        }
        val nextEligible = now.plus(delay).epochSecond
        preferences.edit()
            .putInt(KEY_DISMISS_COUNT, count)
            .putLong(KEY_NEXT_ELIGIBLE, nextEligible)
            .apply()
    }
}

@Composable
fun rememberNotificationPromptTracker(): NotificationPermissionPromptTracker {
    val context = LocalContext.current
    return remember(context) {
        NotificationPermissionPromptTracker(
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        )
    }
}
