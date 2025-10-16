package com.example.calendar.reminder

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.WorkManager

/**
 * Receives boot completed broadcasts and re-schedules any reminders that were
 * persisted before the device rebooted. This keeps alarm and WorkManager based
 * reminders aligned with the SharedPreferences backing store.
 */
class ReminderBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (!isBootCompletedAction(action)) {
            return
        }

        val appContext = context.applicationContext
        val rescheduler = ReminderRescheduler(appContext)
        rescheduler.restore()
    }

    companion object {
        private val bootActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED
        )

        private fun isBootCompletedAction(action: String): Boolean = bootActions.contains(action)
    }
}

internal class ReminderRescheduler(
    private val context: Context,
    private val schedulerFactory: (Context) -> ReminderScheduler = { createScheduler(it) },
    private val storeFactory: (Context) -> ReminderStore = { createStore(it) }
) {
    fun restore() {
        val appContext = context.applicationContext
        val scheduler = schedulerFactory(appContext)
        val store = storeFactory(appContext)
        ReminderOrchestrator(
            scheduler = scheduler,
            store = store
        )
    }
}

private fun createScheduler(context: Context): ReminderScheduler {
    val alarmManager = context.getSystemService(AlarmManager::class.java)
        ?: context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val workManager = WorkManager.getInstance(context)
    return AndroidReminderScheduler(
        context = context,
        alarmManager = alarmManager,
        workManager = workManager
    )
}

private fun createStore(context: Context): ReminderStore {
    val storageContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        if (context.isDeviceProtectedStorage) {
            context
        } else {
            context.createDeviceProtectedStorageContext()?.also { deviceContext ->
                deviceContext.moveSharedPreferencesFrom(context, REMINDER_STORE_NAME)
            } ?: context
        }
    } else {
        context
    }

    return SharedPreferencesReminderStore(
        storageContext.getSharedPreferences(REMINDER_STORE_NAME, Context.MODE_PRIVATE)
    )
}
