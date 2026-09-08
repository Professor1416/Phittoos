package com.example

import android.app.Application
import androidx.work.Configuration
import com.example.data.db.AppDatabase
import com.example.data.preferences.UserPreferences
import com.example.data.repository.PhittoosRepository
import com.example.reminder.ReminderNotificationHelper
import com.example.reminder.SmartReminderScheduler

class PhittoosApplication : Application(), Configuration.Provider {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val repository: PhittoosRepository by lazy {
        PhittoosRepository(
            friendDao = database.friendDao(),
            transactionDao = database.transactionDao(),
            activityDao = database.activityDao(),
            database = database
        )
    }
    val userPreferences: UserPreferences by lazy { UserPreferences(this) }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()

    override fun onCreate() {
        super.onCreate()
        ReminderNotificationHelper.ensureNotificationChannel(this)
        try {
            SmartReminderScheduler.schedulePeriodicReminderCheck(this)
        } catch (e: Throwable) {
            // In unit tests/Robolectric environments where WorkManager is not active, fail gracefully
        }
    }
}
