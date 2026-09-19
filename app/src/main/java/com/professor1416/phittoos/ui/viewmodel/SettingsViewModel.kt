package com.professor1416.phittoos.ui.viewmodel

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.professor1416.phittoos.data.db.AppDatabase
import com.professor1416.phittoos.data.preferences.UserPreferences
import com.professor1416.phittoos.data.repository.PhittoosRepository
import com.professor1416.phittoos.export.PhittoosCsvExporter
import com.professor1416.phittoos.export.PhittoosBackupManager
import com.professor1416.phittoos.reminder.ReminderNotificationHelper
import com.professor1416.phittoos.reminder.SmartReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val profileName: String = "",
    val remindersEnabled: Boolean = true,
    val isNotificationPermissionGranted: Boolean = true,
    val nameError: String? = null,
    val isExporting: Boolean = false,
    val isClearingData: Boolean = false,
    val appVersion: String = "1.0",
    val message: String? = null,
    val lastBackupTime: Long = 0L
)

class SettingsViewModel(
    private val repository: PhittoosRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            profileName = userPreferences.userName,
            remindersEnabled = userPreferences.remindersEnabled
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun refreshState(context: Context? = null) {
        val permissionGranted = if (context != null) {
            ReminderNotificationHelper.canPostNotifications(context)
        } else {
            true
        }

        val version = if (context != null) {
            getAppVersion(context)
        } else {
            "1.0"
        }

        _uiState.update {
            it.copy(
                profileName = userPreferences.userName,
                remindersEnabled = userPreferences.remindersEnabled,
                isNotificationPermissionGranted = permissionGranted,
                appVersion = version,
                lastBackupTime = userPreferences.lastBackupTime
            )
        }
    }

    fun updateLastBackupTime(time: Long) {
        userPreferences.lastBackupTime = time
        _uiState.update {
            it.copy(lastBackupTime = time)
        }
    }

    fun updateProfileName(newName: String): Boolean {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) {
            _uiState.update { it.copy(nameError = "Name cannot be blank") }
            return false
        }

        userPreferences.userName = trimmed
        _uiState.update {
            it.copy(
                profileName = trimmed,
                nameError = null,
                message = "Name saved"
            )
        }
        return true
    }

    fun clearNameError() {
        _uiState.update { it.copy(nameError = null) }
    }

    fun toggleReminders(enabled: Boolean, context: Context? = null) {
        userPreferences.remindersEnabled = enabled
        _uiState.update { it.copy(remindersEnabled = enabled) }

        if (context != null) {
            if (enabled) {
                // Ensure unique periodic work is scheduled (ExistingPeriodicWorkPolicy.KEEP ensures idempotency)
                SmartReminderScheduler.schedulePeriodicReminderCheck(context)
            } else {
                SmartReminderScheduler.cancelReminderChecks(context)
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    suspend fun generateCsvExport(): Pair<String?, String> {
        val transactions = repository.getAllTransactionsForExport()
        if (transactions.isEmpty()) {
            return Pair(null, "No transactions to export")
        }
        val friends = repository.getAllFriendsForExport()
        val csv = PhittoosCsvExporter.buildCsv(transactions, friends)
        return Pair(csv, "Ready to export")
    }

    suspend fun generateBackupJson(): Pair<String?, String> {
        return try {
            val json = PhittoosBackupManager.exportBackup(repository, userPreferences)
            Pair(json, "Backup ready")
        } catch (e: Exception) {
            Pair(null, "Failed to generate backup: ${e.message}")
        }
    }

    suspend fun restoreBackupJson(context: Context, jsonString: String, database: AppDatabase): Result<Unit> {
        val result = PhittoosBackupManager.restoreBackup(jsonString, repository, userPreferences, database)
        if (result.isSuccess) {
            ReminderNotificationHelper.cancelAllNotifications(context)
            if (userPreferences.remindersEnabled) {
                SmartReminderScheduler.schedulePeriodicReminderCheck(context)
            } else {
                SmartReminderScheduler.cancelReminderChecks(context)
            }
            _uiState.update {
                it.copy(
                    profileName = userPreferences.userName,
                    remindersEnabled = userPreferences.remindersEnabled,
                    message = "Restore completed successfully"
                )
            }
        }
        return result
    }

    suspend fun clearAllData(context: Context, onCleared: () -> Unit = {}) {
        _uiState.update { it.copy(isClearingData = true) }
        try {
            // 1. Clear database tables
            repository.clearAllData()
            // 2. Clear user preferences and reset onboarding
            userPreferences.clearAll()
            // 3. Cancel any outstanding system notifications and background workers
            ReminderNotificationHelper.cancelAllNotifications(context)
            SmartReminderScheduler.cancelReminderChecks(context)
            _uiState.update { it.copy(isClearingData = false, message = "All data cleared") }
            onCleared()
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isClearingData = false,
                    message = "Failed to clear data: ${e.message}"
                )
            }
        }
    }

    companion object {
        fun getAppVersion(context: Context): String {
            return try {
                val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageInfo(
                        context.packageName,
                        PackageManager.PackageInfoFlags.of(0)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(context.packageName, 0)
                }
                packageInfo.versionName ?: "1.0.0"
            } catch (e: Exception) {
                "1.0.0"
            }
        }
    }
}

class SettingsViewModelFactory(
    private val repository: PhittoosRepository,
    private val userPreferences: UserPreferences
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            return SettingsViewModel(repository, userPreferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
