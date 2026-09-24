package com.professor1416.phittoos.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.professor1416.phittoos.R
import com.professor1416.phittoos.data.preferences.AppThemeMode
import com.professor1416.phittoos.export.PhittoosCsvExporter
import com.professor1416.phittoos.ui.theme.CoralOrange
import com.professor1416.phittoos.ui.theme.CoralOrangeDark
import com.professor1416.phittoos.ui.theme.EmeraldGreen
import com.professor1416.phittoos.ui.theme.EmeraldGreenDark
import com.professor1416.phittoos.ui.theme.Slate200
import com.professor1416.phittoos.ui.theme.Slate100
import com.professor1416.phittoos.ui.theme.Slate400
import com.professor1416.phittoos.ui.theme.Slate500
import com.professor1416.phittoos.ui.theme.Slate700
import com.professor1416.phittoos.ui.theme.Slate800
import com.professor1416.phittoos.ui.theme.Slate900
import com.professor1416.phittoos.ui.viewmodel.SettingsViewModel
import com.professor1416.phittoos.export.PhittoosBackupManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onDataCleared: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var showEditNameDialog by remember { mutableStateOf(false) }
    var editNameInput by remember { mutableStateOf("") }
    var showClearDataDialog by remember { mutableStateOf(false) }

    var pendingCsvData by remember { mutableStateOf<String?>(null) }
    var pendingBackupData by remember { mutableStateOf<String?>(null) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var pendingRestoreJson by remember { mutableStateOf<String?>(null) }
    var backupCreatedAt by remember { mutableStateOf<Long?>(null) }
    var backupFriendCount by remember { mutableStateOf<Int?>(null) }
    var backupTransactionCount by remember { mutableStateOf<Int?>(null) }
    var backupProfileName by remember { mutableStateOf<String?>(null) }

    // CSV Document Creator Launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            val csv = pendingCsvData
            if (csv != null) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(csv.toByteArray(Charsets.UTF_8))
                        outputStream.flush()
                    }
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Export completed successfully")
                    }
                } catch (e: Exception) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Export failed: ${e.message}")
                    }
                } finally {
                    pendingCsvData = null
                }
            }
        } else {
            // User cancelled file save dialog - clean up silently
            pendingCsvData = null
        }
    }

    // Backup Document Creator Launcher
    val backupCreateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            val json = pendingBackupData
            if (json != null) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(json.toByteArray(Charsets.UTF_8))
                        outputStream.flush()
                    }
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.updateLastBackupTime(System.currentTimeMillis())
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Backup created successfully")
                    }
                } catch (e: Exception) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Backup creation failed: ${e.message}")
                    }
                } finally {
                    pendingBackupData = null
                }
            }
        } else {
            pendingBackupData = null
        }
    }

    // Backup Document Opener Launcher
    val backupRestoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val jsonContent = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.readBytes().toString(Charsets.UTF_8)
                    }
                    if (jsonContent.isNullOrBlank()) {
                        snackbarHostState.showSnackbar("Failed to read backup: empty file")
                        return@launch
                    }

                    val validation = com.professor1416.phittoos.export.PhittoosBackupManager.validateBackup(jsonContent)
                    if (validation.isSuccess) {
                        val summary = validation.getOrNull()!!
                        backupProfileName = summary.profileName
                        backupCreatedAt = summary.createdAt
                        backupFriendCount = summary.friendCount
                        backupTransactionCount = summary.transactionCount
                        pendingRestoreJson = jsonContent
                        showRestoreConfirmDialog = true
                    } else {
                        val errorMsg = validation.exceptionOrNull()?.message ?: "Unknown error"
                        snackbarHostState.showSnackbar(errorMsg)
                    }
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("This isn’t a valid Phittoos backup file. Select a backup created using ‘Back up your data’.")
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshState(context)
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Slate800
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. PROFILE SECTION
            SettingsSection(title = "PROFILE") {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = EmeraldGreen,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Name",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate500,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = uiState.profileName.ifBlank { "Not set" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        TextButton(
                            onClick = {
                                editNameInput = uiState.profileName
                                viewModel.clearNameError()
                                showEditNameDialog = true
                            },
                            modifier = Modifier.testTag("btn_edit_profile_name")
                        ) {
                            Text(
                                text = "Edit",
                                fontWeight = FontWeight.SemiBold,
                                color = EmeraldGreenDark
                            )
                        }
                    }
                }
            }

            // 2. APPEARANCE / THEME SECTION
            SettingsSection(title = stringResource(R.string.settings_section_appearance)) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Header info row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.BrightnessMedium,
                                contentDescription = null,
                                tint = EmeraldGreen,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.settings_theme_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = when (uiState.themeMode) {
                                        AppThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system_desc)
                                        AppThemeMode.LIGHT -> stringResource(R.string.settings_theme_light_desc)
                                        AppThemeMode.DARK -> stringResource(R.string.settings_theme_dark_desc)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate500
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        ThemeOptionRow(
                            title = stringResource(R.string.settings_theme_system),
                            subtitle = stringResource(R.string.settings_theme_system_desc),
                            icon = Icons.Default.BrightnessAuto,
                            isSelected = uiState.themeMode == AppThemeMode.SYSTEM,
                            testTag = "row_theme_system",
                            radioTag = "radio_theme_system",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.updateThemeMode(AppThemeMode.SYSTEM)
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        ThemeOptionRow(
                            title = stringResource(R.string.settings_theme_light),
                            subtitle = stringResource(R.string.settings_theme_light_desc),
                            icon = Icons.Default.LightMode,
                            isSelected = uiState.themeMode == AppThemeMode.LIGHT,
                            testTag = "row_theme_light",
                            radioTag = "radio_theme_light",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.updateThemeMode(AppThemeMode.LIGHT)
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        ThemeOptionRow(
                            title = stringResource(R.string.settings_theme_dark),
                            subtitle = stringResource(R.string.settings_theme_dark_desc),
                            icon = Icons.Default.DarkMode,
                            isSelected = uiState.themeMode == AppThemeMode.DARK,
                            testTag = "row_theme_dark",
                            radioTag = "radio_theme_dark",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.updateThemeMode(AppThemeMode.DARK)
                            }
                        )
                    }
                }
            }

            // 2. REMINDERS SECTION
            SettingsSection(title = "REMINDERS") {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .toggleable(
                                    value = uiState.remindersEnabled,
                                    role = Role.Switch,
                                    onValueChange = { enabled ->
                                        viewModel.toggleReminders(enabled, context)
                                    }
                                )
                                .padding(16.dp)
                                .testTag("row_repayment_reminders"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = EmeraldGreen,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.padding(end = 8.dp)) {
                                    Text(
                                        text = "Repayment reminders",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Slate900
                                    )
                                    Text(
                                        text = "Periodic notifications for overdue money lent",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate500
                                    )
                                }
                            }
                            Switch(
                                checked = uiState.remindersEnabled,
                                onCheckedChange = null,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = EmeraldGreen
                                ),
                                modifier = Modifier
                                    .testTag("switch_repayment_reminders")
                                    .clearAndSetSemantics { }
                            )
                        }

                        if (!uiState.isNotificationPermissionGranted && uiState.remindersEnabled) {
                            HorizontalDivider(color = Slate200)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = CoralOrangeDark,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Notifications are disabled in Android settings.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CoralOrangeDark,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // 3. YOUR DATA SECTION
            SettingsSection(title = "YOUR DATA") {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Back up your data
                        SettingsClickableRow(
                            icon = Icons.Default.FileDownload,
                            iconTint = EmeraldGreen,
                            title = "Back up your data",
                            subtitle = "Keep a copy you can restore later",
                            testTag = "btn_create_backup",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                coroutineScope.launch {
                                    val (json, msg) = viewModel.generateBackupJson()
                                    if (json == null) {
                                        snackbarHostState.showSnackbar(msg)
                                    } else {
                                        pendingBackupData = json
                                        val defaultName = PhittoosBackupManager.generateDefaultFileName()
                                        backupCreateLauncher.launch(defaultName)
                                    }
                                }
                            }
                        )

                        HorizontalDivider(color = Slate200)

                        // Restore from backup
                        SettingsClickableRow(
                            icon = Icons.Default.Storage,
                            iconTint = EmeraldGreen,
                            title = "Restore from backup",
                            subtitle = "Replace current data with a previous backup",
                            testTag = "btn_restore_backup",
                            onClick = {
                                backupRestoreLauncher.launch(arrayOf("*/*"))
                            }
                        )

                        HorizontalDivider(color = Slate200)

                        // Export as CSV
                        SettingsClickableRow(
                            icon = Icons.Default.FileDownload,
                            iconTint = EmeraldGreen,
                            title = "Export as CSV",
                            subtitle = "Save a spreadsheet copy for viewing or sharing",
                            testTag = "btn_export_data",
                            onClick = {
                                coroutineScope.launch {
                                    val (csv, msg) = viewModel.generateCsvExport()
                                    if (csv == null) {
                                        snackbarHostState.showSnackbar(msg)
                                    } else {
                                        pendingCsvData = csv
                                        val defaultName = PhittoosCsvExporter.generateDefaultFileName()
                                        exportLauncher.launch(defaultName)
                                    }
                                }
                            }
                        )

                        if (uiState.lastBackupTime > 0L) {
                            val lastBackupStr = remember(uiState.lastBackupTime) {
                                try {
                                    java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(uiState.lastBackupTime))
                                } catch (e: Exception) {
                                    "Unknown"
                                }
                            }
                            HorizontalDivider(color = Slate200)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = EmeraldGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Last backup: $lastBackupStr",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate500
                                )
                            }
                        }
                    }
                }
            }

            // 4. ABOUT SECTION
            SettingsSection(title = "ABOUT") {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // App Version Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Slate700,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = "App version",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Slate900,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = "v${uiState.appVersion}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Slate500,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        HorizontalDivider(color = Slate200)

                        // Privacy & Storage Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = Slate700,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Privacy & Storage",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Slate900,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Your Phittoos data is stored on this device.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate500
                                )
                            }
                        }
                    }
                }
            }

            // 5. DANGER ZONE SECTION
            SettingsSection(title = "DANGER ZONE") {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SettingsClickableRow(
                            icon = Icons.Default.DeleteForever,
                            iconTint = MaterialTheme.colorScheme.error,
                            title = "Clear all data",
                            subtitle = "Permanently delete everything from this device",
                            titleColor = MaterialTheme.colorScheme.error,
                            testTag = "btn_clear_all_data",
                            onClick = {
                                showClearDataDialog = true
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Edit Profile Name Dialog
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = {
                showEditNameDialog = false
                viewModel.clearNameError()
            },
            title = {
                Text(
                    text = "Edit Profile Name",
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedTextField(
                        value = editNameInput,
                        onValueChange = {
                            editNameInput = it
                            if (uiState.nameError != null) {
                                viewModel.clearNameError()
                            }
                        },
                        label = { Text("Your Name") },
                        isError = uiState.nameError != null,
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_edit_name_input")
                    )
                    if (uiState.nameError != null) {
                        Text(
                            text = uiState.nameError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val success = viewModel.updateProfileName(editNameInput)
                        if (success) {
                            showEditNameDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                    modifier = Modifier.testTag("dialog_edit_name_save")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showEditNameDialog = false
                        viewModel.clearNameError()
                    },
                    modifier = Modifier.testTag("dialog_edit_name_cancel")
                ) {
                    Text("Cancel", color = Slate700)
                }
            }
        )
    }

    // Clear All Data Confirmation Dialog
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = {
                Text(
                    text = "Clear all data from this phone?",
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "This permanently deletes your friends, transactions and history from this device. This cannot be undone unless you created a Phittoos backup earlier.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate700
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearDataDialog = false
                        coroutineScope.launch {
                            viewModel.clearAllData(context, onDataCleared)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("dialog_clear_data_confirm")
                ) {
                    Text("Clear all data")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearDataDialog = false },
                    modifier = Modifier.testTag("dialog_clear_data_cancel")
                ) {
                    Text("Cancel", color = Slate700)
                }
            }
        )
    }

    // Restore Backup Confirmation Dialog
    if (showRestoreConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                showRestoreConfirmDialog = false
                pendingRestoreJson = null
            },
            title = {
                Text(
                    text = androidx.compose.ui.res.stringResource(id = com.professor1416.phittoos.R.string.dialog_restore_title),
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "This will completely replace your current local Phittoos data. Existing local data will be permanently removed.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate700
                    )

                    if (backupFriendCount != null && backupTransactionCount != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Slate100),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Backup Contents",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                val dateStr = if (backupCreatedAt != null && backupCreatedAt != 0L) {
                                    try {
                                        java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(backupCreatedAt!!))
                                    } catch (e: Exception) {
                                        "Unknown Date"
                                    }
                                } else {
                                    "Unknown Date"
                                }
                                if (backupProfileName != null && backupProfileName!!.isNotBlank()) {
                                    Text(
                                        text = "• Profile: $backupProfileName",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Slate700
                                    )
                                }
                                Text(
                                    text = "• Created: $dateStr",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Slate700
                                )
                                Text(
                                    text = "• Friends: $backupFriendCount",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Slate700
                                )
                                Text(
                                    text = "• Transactions: $backupTransactionCount",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Slate700
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreConfirmDialog = false
                        val jsonContent = pendingRestoreJson
                        if (jsonContent != null) {
                            coroutineScope.launch {
                                try {
                                    val app = context.applicationContext as com.professor1416.phittoos.PhittoosApplication
                                    val result = viewModel.restoreBackupJson(context, jsonContent, app.database)
                                    if (result.isSuccess) {
                                        snackbarHostState.showSnackbar("Data restored successfully")
                                        viewModel.refreshState(context)
                                    } else {
                                        val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                                        snackbarHostState.showSnackbar("Restore failed: $errorMsg")
                                    }
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Restore failed: ${e.message}")
                                } finally {
                                    pendingRestoreJson = null
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CoralOrange
                    ),
                    modifier = Modifier.testTag("dialog_restore_confirm")
                ) {
                    Text("Restore & Replace")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirmDialog = false
                        pendingRestoreJson = null
                    },
                    modifier = Modifier.testTag("dialog_restore_cancel")
                ) {
                    Text("Cancel", color = Slate700)
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Slate500,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp)
        )
        content()
    }
}

@Composable
private fun SettingsClickableRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit,
    titleColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = Slate400,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ThemeOptionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    testTag: String,
    radioTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) EmeraldGreen else Slate400,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) EmeraldGreenDark else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500
                )
            }
        }
        RadioButton(
            selected = isSelected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = EmeraldGreen,
                unselectedColor = Slate400
            ),
            modifier = Modifier
                .size(24.dp)
                .testTag(radioTag)
        )
    }
}
