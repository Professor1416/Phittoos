package com.example.ui.screens.frienddetail

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TransactionDirection
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionStatus
import com.example.ui.components.AvatarInitial
import com.example.ui.theme.CoralOrange
import com.example.ui.theme.CoralOrangeDark
import com.example.ui.theme.CoralOrangeSurface
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.EmeraldGreenSurface
import com.example.ui.theme.Red100
import com.example.ui.theme.Red600
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.util.Formatters
import com.example.ui.viewmodel.FriendDetailViewModel
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendDetailScreen(
    viewModel: FriendDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAddTransaction: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.friend?.name ?: "Friend Details",
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = EmeraldGreen)
            }
        } else {
            val friend = uiState.friend ?: return@Scaffold
            val net = uiState.netBalance

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Header: Avatar, Name, Balance, Reliability Tag
                item {
                    FriendDetailHeader(
                        friendName = friend.name,
                        netBalance = net
                    )
                }

                // Action Row: "Add new transaction" & "Mark as paid"
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    ActionRow(
                        hasOpenBalance = uiState.openTransactionsCount > 0,
                        onAddTransaction = { onNavigateToAddTransaction(friend.id) },
                        onMarkAsPaid = { viewModel.markAllAsPaid() }
                    )
                }

                // Timeline Section Header
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TRANSACTION TIMELINE (${uiState.timeline.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate500
                        )
                        if (uiState.openTransactionsCount > 0) {
                            Text(
                                text = "${uiState.openTransactionsCount} open",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = EmeraldGreenDark
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Timeline Items
                if (uiState.timeline.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No transactions yet with ${friend.name}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Slate500,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { onNavigateToAddTransaction(friend.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add Transaction", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    items(
                        items = uiState.timeline,
                        key = { "timeline_${it.id}" }
                    ) { tx ->
                        TimelineTransactionItem(
                            tx = tx,
                            onMarkPaid = { viewModel.markTransactionAsPaid(tx.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FriendDetailHeader(
    friendName: String,
    netBalance: Double
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("friend_detail_header")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AvatarInitial(name = friendName, size = 64.dp)
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = friendName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Slate900,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Current Balance (large and clear)
            when {
                netBalance > 0 -> {
                    Text(
                        text = "owes you",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate500,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = Formatters.formatCurrency(netBalance),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = EmeraldGreenDark
                    )
                }
                netBalance < 0 -> {
                    Text(
                        text = "you owe",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate500,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = Formatters.formatCurrency(abs(netBalance)),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = CoralOrangeDark
                    )
                }
                else -> {
                    Text(
                        text = "All settled up",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate500,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "₹0 (Phittoos!)",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGreenDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Slate200.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(14.dp))

            // Reliability Tag Area (Placeholder "New" for Phase 1 per PRD)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Slate100)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = EmeraldGreen
                ) {
                    Text(
                        text = "New",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Usually pays back within 4 days (Placeholder)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate700,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ActionRow(
    hasOpenBalance: Boolean,
    onAddTransaction: () -> Unit,
    onMarkAsPaid: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // "Add new transaction" button (functional)
        Button(
            onClick = onAddTransaction,
            colors = ButtonDefaults.buttonColors(containerColor = Slate900),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .weight(1f)
                .height(50.dp)
                .testTag("button_detail_add_transaction")
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Add Loan", fontWeight = FontWeight.Bold)
        }

        // "Mark as paid" button (UI wired to flip status to CONFIRMED)
        OutlinedButton(
            onClick = onMarkAsPaid,
            enabled = hasOpenBalance,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = EmeraldGreenDark
            ),
            modifier = Modifier
                .weight(1f)
                .height(50.dp)
                .testTag("button_detail_mark_paid")
        ) {
            Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Mark as Paid", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TimelineTransactionItem(
    tx: TransactionEntity,
    onMarkPaid: () -> Unit
) {
    val isLent = tx.direction == TransactionDirection.LENT
    val isConfirmed = tx.status == TransactionStatus.CONFIRMED
    val isOverdue = !isConfirmed && tx.dueDate != null && tx.dueDate < System.currentTimeMillis()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .testTag("timeline_tx_${tx.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Direction icon
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isLent) EmeraldGreenSurface else CoralOrangeSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isLent) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = null,
                        tint = if (isLent) EmeraldGreenDark else CoralOrangeDark,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isLent) "You lent" else "You borrowed",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    Text(
                        text = Formatters.formatFullDate(tx.createdDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = Formatters.formatCurrency(tx.amount),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isLent) EmeraldGreenDark else CoralOrangeDark
                    )

                    // Status Badge
                    when {
                        isConfirmed -> {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = EmeraldGreenSurface,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = EmeraldGreenDark,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Confirmed",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldGreenDark
                                    )
                                }
                            }
                        }
                        isOverdue -> {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Red100,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = Red600,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Overdue",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Red600
                                    )
                                }
                            }
                        }
                        else -> {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Slate100,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        Icons.Default.HourglassTop,
                                        contentDescription = null,
                                        tint = Slate500,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Open",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Slate700
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Note line
            if (!tx.note.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Slate100,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Note: ${tx.note}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate700,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Due date & Individual Mark Paid button
            if (!isConfirmed) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (tx.dueDate != null) {
                        Text(
                            text = "Due: ${Formatters.formatFullDate(tx.dueDate)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isOverdue) Red600 else Slate500,
                            fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal
                        )
                    } else {
                        Text(
                            text = "No due date",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate400
                        )
                    }

                    Text(
                        text = "Settle this",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGreenDark,
                        modifier = Modifier
                            .clickable(onClick = onMarkPaid)
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}
