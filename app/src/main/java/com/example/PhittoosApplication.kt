package com.example

import android.app.Application
import com.example.data.db.AppDatabase
import com.example.data.preferences.UserPreferences
import com.example.data.repository.PhittoosRepository

class PhittoosApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val repository: PhittoosRepository by lazy {
        PhittoosRepository(database.friendDao(), database.transactionDao(), database.activityDao())
    }
    val userPreferences: UserPreferences by lazy { UserPreferences(this) }
}
