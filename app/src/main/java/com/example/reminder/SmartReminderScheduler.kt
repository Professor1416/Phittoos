package com.example.reminder

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object SmartReminderScheduler {
    const val PERIODIC_WORK_NAME = "phittoos_smart_reminder_check"
    const val IMMEDIATE_WORK_NAME = "phittoos_smart_reminder_check_immediate"

    /**
     * Schedules daily local reminder checks.
     * Idempotent: Uses ExistingPeriodicWorkPolicy.KEEP so it preserves current execution cadence.
     */
    fun schedulePeriodicReminderCheck(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<SmartReminderWorker>(
            1, TimeUnit.DAYS
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    /**
     * Triggers an immediate one-time reminder check.
     * Useful for debugging, testing, or user-initiated sync.
     */
    fun triggerImmediateReminderCheck(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<SmartReminderWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    /**
     * Cancels any scheduled reminder checks.
     */
    fun cancelReminderChecks(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
    }
}
