package com.example.ui.screens.addtransaction

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Friend
import com.example.data.model.TransactionDirection
import com.example.domain.DueDateHelper
import com.example.ui.components.AvatarInitial
import com.example.ui.theme.CoralOrange
import com.example.ui.theme.CoralOrangeDark
import com.example.ui.theme.CoralOrangeLight
import com.example.ui.theme.CoralOrangeSurface
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.EmeraldGreenLight
import com.example.ui.theme.EmeraldGreenSurface
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.util.Formatters
import com.example.ui.viewmodel.AddTransactionViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionScreen(
    viewModel: AddTransactionViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    // Auto-focus amount field if friend is already selected (or after friend is chosen)
    LaunchedEffect(uiState.selectedFriend) {
        if (uiState.selectedFriend != null && uiState.amount.isEmpty()) {
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) {
                // Focus request safe guard
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Add Transaction",
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("button_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Slate900
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            // One-tap Save Button
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    val currentDueDate = uiState.dueDate
                    val isDueDateValid = currentDueDate == null || !DueDateHelper.isPastDate(currentDueDate)
                    val isReady = uiState.selectedFriend != null &&
                        uiState.amount.toDoubleOrNull()?.let { it > 0 } == true &&
                        isDueDateValid

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.saveTransaction { toastMessage ->
                                Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
                                onNavigateBack()
                            }
                        },
                        enabled = isReady && !uiState.isSaving,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.direction == TransactionDirection.LENT) EmeraldGreen else CoralOrange,
                            disabledContainerColor = Slate200
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("button_save_transaction")
                    ) {
                        Text(
                            text = if (uiState.isSaving) "Saving..." else "Save Transaction",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isReady) Color.White else Slate400
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Error Message (if any)
            if (uiState.errorMessage != null) {
                Surface(
                    color = Color(0xFFFEE2E2),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = uiState.errorMessage ?: "",
                        color = Color(0xFFDC2626),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // 1. SELECT FRIEND SECTION
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "1. SELECT FRIEND",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Slate500
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (uiState.selectedFriend != null) {
                        // Selected friend card
                        val friend = uiState.selectedFriend!!
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Slate100)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AvatarInitial(name = friend.name, size = 42.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = friend.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                Text(
                                    text = "Selected",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldGreenDark,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            IconButton(
                                onClick = { viewModel.updateFriendSearch("") },
                                modifier = Modifier.testTag("button_change_friend")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Change friend",
                                    tint = Slate500
                                )
                            }
                        }
                    }

                    // Recent Friends as Chips (Fast 1-tap selection)
                    if (uiState.recentFriends.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Recent Friends",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate400
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.recentFriends.forEach { friend ->
                                val isSelected = uiState.selectedFriend?.id == friend.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.selectFriend(friend) },
                                    label = { Text(friend.name, fontWeight = FontWeight.Medium) },
                                    leadingIcon = {
                                        AvatarInitial(name = friend.name, size = 20.dp)
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Slate900,
                                        selectedLabelColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Friend search / input field
                    OutlinedTextField(
                        value = uiState.friendSearchQuery,
                        onValueChange = { viewModel.updateFriendSearch(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_friend_search"),
                        placeholder = { Text("Search or type new friend's name", color = Slate400, fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Slate400)
                        },
                        trailingIcon = {
                            if (uiState.friendSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateFriendSearch("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Slate400)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Slate900,
                            unfocusedTextColor = Slate900,
                            cursorColor = EmeraldGreen,
                            focusedBorderColor = EmeraldGreen,
                            unfocusedBorderColor = Slate200,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedPlaceholderColor = Slate400,
                            unfocusedPlaceholderColor = Slate400
                        )
                    )

                    // Matching friends or inline "Add new friend" option
                    val query = uiState.friendSearchQuery.trim()
                    if (query.isNotEmpty()) {
                        val matches = uiState.allFriends.filter {
                            it.name.contains(query, ignoreCase = true)
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            matches.forEach { friend ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.selectFriend(friend) }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AvatarInitial(name = friend.name, size = 32.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = friend.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Slate800
                                    )
                                }
                            }

                            // Inline "+ Add new friend 'XYZ'" button
                            val exactMatch = matches.any { it.name.equals(query, ignoreCase = true) }
                            if (!exactMatch) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.addNewFriend(query) }
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(EmeraldGreenLight),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PersonAdd,
                                            contentDescription = null,
                                            tint = EmeraldGreenDark,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "+ Add \"$query\" as new friend",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldGreenDark
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. TOGGLE: "I LENT" / "I BORROWED" (Large, thumb-friendly)
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "2. TRANSACTION TYPE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Slate500
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // "I Lent" Button
                        val isLent = uiState.direction == TransactionDirection.LENT
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isLent) EmeraldGreen else Slate100)
                                .clickable { viewModel.setDirection(TransactionDirection.LENT) }
                                .padding(vertical = 16.dp, horizontal = 12.dp)
                                .testTag("toggle_lent"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = if (isLent) Color.White else Slate500,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "I Lent",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isLent) Color.White else Slate700
                                )
                            }
                        }

                        // "I Borrowed" Button
                        val isBorrowed = uiState.direction == TransactionDirection.BORROWED
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isBorrowed) CoralOrange else Slate100)
                                .clickable { viewModel.setDirection(TransactionDirection.BORROWED) }
                                .padding(vertical = 16.dp, horizontal = 12.dp)
                                .testTag("toggle_borrowed"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                                    contentDescription = null,
                                    tint = if (isBorrowed) Color.White else Slate500,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "I Borrowed",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isBorrowed) Color.White else Slate700
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. AMOUNT FIELD (Numeric keypad auto-opening)
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "3. AMOUNT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Slate500
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "₹",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.direction == TransactionDirection.LENT) EmeraldGreenDark else CoralOrangeDark,
                            modifier = Modifier.padding(end = 8.dp)
                        )

                        OutlinedTextField(
                            value = uiState.amount,
                            onValueChange = { viewModel.setAmount(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .testTag("input_amount"),
                            placeholder = { Text("0", fontSize = 32.sp, color = Slate400) },
                            textStyle = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Slate900,
                                fontSize = 32.sp
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Slate900,
                                unfocusedTextColor = Slate900,
                                cursorColor = if (uiState.direction == TransactionDirection.LENT) EmeraldGreen else CoralOrange,
                                focusedBorderColor = if (uiState.direction == TransactionDirection.LENT) EmeraldGreen else CoralOrange,
                                unfocusedBorderColor = Slate200,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedPlaceholderColor = Slate400,
                                unfocusedPlaceholderColor = Slate400
                            )
                        )
                    }

                    // Fast Quick-amount chips (+100, +200, +500, +1000, +2000)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(100, 200, 500, 1000, 2000, 5000).forEach { quickAmt ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Slate100,
                                modifier = Modifier.clickable {
                                    val current = uiState.amount.toDoubleOrNull() ?: 0.0
                                    val next = current + quickAmt
                                    viewModel.setAmount(next.toInt().toString())
                                }
                            ) {
                                Text(
                                    text = "+₹$quickAmt",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Slate700,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. OPTIONAL NOTE FIELD
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "4. NOTE (OPTIONAL)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Slate500
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = uiState.note,
                        onValueChange = { viewModel.setNote(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_note"),
                        placeholder = { Text("What's this for? (e.g. Dinner, cab, grocery)", color = Slate400, fontSize = 14.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Slate900,
                            unfocusedTextColor = Slate900,
                            cursorColor = EmeraldGreen,
                            focusedBorderColor = EmeraldGreen,
                            unfocusedBorderColor = Slate200,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedPlaceholderColor = Slate400,
                            unfocusedPlaceholderColor = Slate400
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. OPTIONAL DUE DATE PICKER (Defaults to "no due date")
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "5. DUE DATE (OPTIONAL)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Slate500
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val isDueDateSelected = uiState.dueDate != null
                    val isPast = isDueDateSelected && DueDateHelper.isPastDate(uiState.dueDate!!)

                    fun showDatePicker() {
                        val cal = Calendar.getInstance()
                        if (uiState.dueDate != null) {
                            cal.timeInMillis = uiState.dueDate!!
                        }
                        val picker = DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                val selectedCal = Calendar.getInstance().apply {
                                    set(Calendar.YEAR, year)
                                    set(Calendar.MONTH, month)
                                    set(Calendar.DAY_OF_MONTH, day)
                                    set(Calendar.HOUR_OF_DAY, 12)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                viewModel.setDueDate(selectedCal.timeInMillis)
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        )
                        picker.datePicker.minDate = System.currentTimeMillis() - 1000
                        picker.show()
                    }

                    if (!isDueDateSelected) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Slate100,
                                modifier = Modifier.testTag("chip_no_due_date")
                            ) {
                                Text(
                                    text = "No due date",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Slate600,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }

                            OutlinedButton(
                                onClick = { showDatePicker() },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("button_pick_due_date")
                            ) {
                                Icon(
                                    Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Slate700
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Pick date",
                                    fontWeight = FontWeight.SemiBold,
                                    color = Slate800
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { showDatePicker() },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isPast) Color(0xFFFEE2E2) else EmeraldGreenSurface
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isPast) Color(0xFFDC2626) else EmeraldGreen
                                ),
                                modifier = Modifier.testTag("button_pick_due_date")
                            ) {
                                Icon(
                                    Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isPast) Color(0xFFDC2626) else EmeraldGreenDark
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = Formatters.formatFullDate(uiState.dueDate),
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPast) Color(0xFFDC2626) else EmeraldGreenDark
                                )
                            }

                            TextButton(
                                onClick = { viewModel.setDueDate(null) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("button_remove_due_date")
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove due date",
                                    modifier = Modifier.size(16.dp),
                                    tint = Slate500
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Remove",
                                    color = Slate600,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (isPast) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Due date cannot be in the past.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFDC2626),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
