package com.professor1416.phittoos

import android.app.Application
import androidx.work.Configuration
import com.professor1416.phittoos.data.db.AppDatabase
import com.professor1416.phittoos.data.preferences.UserPreferences
import com.professor1416.phittoos.data.repository.PhittoosRepository
import com.professor1416.phittoos.reminder.ReminderNotificationHelper
import com.professor1416.phittoos.reminder.SmartReminderScheduler

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
