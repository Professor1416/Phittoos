package com.example.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.PhittoosApplication

class SmartReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val app = applicationContext as? PhittoosApplication
                ?: return Result.failure()

            if (!app.userPreferences.remindersEnabled) {
                return Result.success() // Safe no-op when repayment reminders are toggled off by user
            }

            SmartReminderEngine.checkAndSendReminders(
                repository = app.repository,
                context = applicationContext,
                remindersEnabled = true
            )

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
