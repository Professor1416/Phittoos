package com.example.ui.viewmodel

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.UserPreferences
import com.example.data.repository.PhittoosRepository
import com.example.export.PhittoosCsvExporter
import com.example.reminder.ReminderNotificationHelper
import com.example.reminder.SmartReminderScheduler
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
    val message: String? = null
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
                appVersion = version
            )
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

        if (enabled && context != null) {
            // Ensure unique periodic work is scheduled (ExistingPeriodicWorkPolicy.KEEP ensures idempotency)
            SmartReminderScheduler.schedulePeriodicReminderCheck(context)
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

    suspend fun clearAllData(context: Context, onCleared: () -> Unit = {}) {
        _uiState.update { it.copy(isClearingData = true) }
        try {
            // 1. Clear database tables
            repository.clearAllData()
            // 2. Clear user preferences and reset onboarding
            userPreferences.clearAll()
            // 3. Cancel any outstanding system notifications
            ReminderNotificationHelper.cancelAllNotifications(context)
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
                packageInfo.versionName ?: "1.0"
            } catch (e: Exception) {
                "1.0"
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
