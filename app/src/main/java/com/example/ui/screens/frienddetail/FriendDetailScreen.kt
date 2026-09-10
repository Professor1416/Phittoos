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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import com.example.domain.ReliabilityInfo
import com.example.R
import com.example.ui.components.FriendDetailReliabilitySection
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TransactionDirection
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionStatus
import com.example.data.model.dueInfo
import com.example.data.model.effectivePaidAmount
import com.example.data.model.effectiveRemainingAmount
import com.example.domain.DueDateHelper
import com.example.domain.DueState
import com.example.ui.components.AvatarInitial
import com.example.ui.theme.Amber100
import com.example.ui.theme.Amber700
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
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.util.Formatters
import com.example.ui.util.UiMessage
import com.example.ui.viewmodel.BulkSettlementEligibility
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

    LifecycleResumeEffect(viewModel) {
        viewModel.onScreenResumed()
        onPauseOrDispose {
            viewModel.onScreenPaused()
        }
    }

    var txToSettle by remember { mutableStateOf<TransactionEntity?>(null) }
    var txToRepay by remember { mutableStateOf<TransactionEntity?>(null) }
    var showBulkSettlementDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { msg ->
            Toast.makeText(context, msg.asString(context), Toast.LENGTH_SHORT).show()
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
        } else if (uiState.isFriendNotFound || uiState.friend == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Slate400,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Friend not found",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This friend may have been deleted or your local data was reset.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate500,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onNavigateBack,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Return to Home")
                    }
                }
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
                        netBalance = net,
                        openTransactionsCount = uiState.openTransactionsCount,
                        reliabilityInfo = uiState.reliabilityInfo
                    )
                }

                // Action Row: "Add loan" & "Settle all" / "Settle individually"
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    ActionRow(
                        eligibility = uiState.bulkSettlementEligibility,
                        onAddTransaction = { onNavigateToAddTransaction(friend.id) },
                        onRequestBulkSettle = { showBulkSettlementDialog = true }
                    )
                }

                // Mixed-Direction Guidance Notice
                if (uiState.bulkSettlementEligibility == BulkSettlementEligibility.MIXED_DIRECTIONS) {
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("card_mixed_settlement_notice"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Slate100)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Slate700,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Settle individually",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate900
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "You have unsettled transactions in both directions. Settle them individually to keep your records accurate.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate700,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Settlement Celebration Card (immediately above Transaction Timeline header)
                uiState.celebrationEvent?.let { celebration ->
                    item(key = "card_settlement_celebration_item") {
                        Spacer(modifier = Modifier.height(16.dp))
                        SettlementCelebrationCard(
                            event = celebration,
                            onDismiss = { viewModel.dismissCelebration(celebration.id) }
                        )
                    }
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
                            onSettleClick = { txToSettle = tx },
                            onRepayClick = { txToRepay = tx }
                        )
                    }
                }
            }

            // Contextual Repayment Dialog
            txToRepay?.let { tx ->
                RepaymentDialog(
                    friendName = friend.name,
                    tx = tx,
                    onConfirm = { amount ->
                        val id = tx.id
                        txToRepay = null
                        viewModel.recordRepayment(id, amount)
                    },
                    onDismiss = { txToRepay = null }
                )
            }

            // Contextual Individual Settlement Dialog
            txToSettle?.let { tx ->
                IndividualSettlementDialog(
                    friendName = friend.name,
                    tx = tx,
                    onConfirm = {
                        viewModel.settleTransaction(tx.id)
                        txToSettle = null
                    },
                    onDismiss = { txToSettle = null }
                )
            }

            // Contextual Same-Direction Bulk Settlement Dialog
            if (showBulkSettlementDialog) {
                BulkSettlementDialog(
                    friendName = friend.name,
                    eligibility = uiState.bulkSettlementEligibility,
                    totalRemaining = uiState.bulkSettlementTotalRemaining,
                    openCount = uiState.bulkSettlementOpenCount,
                    onConfirm = {
                        viewModel.settleAllSameDirection()
                        showBulkSettlementDialog = false
                    },
                    onDismiss = { showBulkSettlementDialog = false }
                )
            }
        }
    }
}

@Composable
private fun FriendDetailHeader(
    friendName: String,
    netBalance: Double,
    openTransactionsCount: Int,
    reliabilityInfo: ReliabilityInfo
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
                openTransactionsCount > 0 -> {
                    Text(
                        text = "Net balance ₹0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate500,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "₹0",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate900
                    )
                    Text(
                        text = "($openTransactionsCount unsettled loan${if (openTransactionsCount == 1) "" else "s"})",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate500,
                        modifier = Modifier.padding(top = 4.dp)
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

            // Private Reliability Section
            FriendDetailReliabilitySection(reliability = reliabilityInfo)
        }
    }
}

@Composable
private fun ActionRow(
    eligibility: BulkSettlementEligibility,
    onAddTransaction: () -> Unit,
    onRequestBulkSettle: () -> Unit
) {
    val canBulkSettle = eligibility == BulkSettlementEligibility.SAME_DIRECTION_LENT ||
        eligibility == BulkSettlementEligibility.SAME_DIRECTION_BORROWED
    val isMixed = eligibility == BulkSettlementEligibility.MIXED_DIRECTIONS

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
            Text("Add transaction", fontWeight = FontWeight.Bold)
        }

        // Bulk settlement action or disabled explanation button
        OutlinedButton(
            onClick = onRequestBulkSettle,
            enabled = canBulkSettle,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = if (canBulkSettle) EmeraldGreenDark else Slate400,
                disabledContentColor = Slate400
            ),
            modifier = Modifier
                .weight(1f)
                .height(50.dp)
                .testTag("button_detail_mark_paid")
        ) {
            Icon(
                if (isMixed) Icons.Default.Info else Icons.Default.DoneAll,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isMixed) "Settle individually" else "Settle All",
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TimelineTransactionItem(
    tx: TransactionEntity,
    onSettleClick: () -> Unit,
    onRepayClick: () -> Unit
) {
    val isLent = tx.direction == TransactionDirection.LENT
    val isConfirmed = tx.status == TransactionStatus.CONFIRMED
    val dueInfo = tx.dueInfo
    val isOverdue = dueInfo.isActivelyOverdue
    val effectivePaid = tx.effectivePaidAmount
    val remaining = tx.effectiveRemainingAmount
    val isPartiallyPaid = !isConfirmed && effectivePaid > 0.0

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
                                        text = "Settled",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldGreenDark
                                    )
                                }
                            }
                        }
                        isPartiallyPaid -> {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Amber100,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        Icons.Default.HourglassTop,
                                        contentDescription = null,
                                        tint = Amber700,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Partially Paid",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Amber700
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
                                        text = "Pending",
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

            // Partial payment details breakdown
            if (isPartiallyPaid) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Slate100.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Original: ${Formatters.formatCurrency(tx.amount)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate600
                        )
                        Text(
                            text = if (isLent) "Paid back: ${Formatters.formatCurrency(effectivePaid)}" else "You paid: ${Formatters.formatCurrency(effectivePaid)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isLent) EmeraldGreenDark else CoralOrangeDark,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isLent) "Still pending: ${Formatters.formatCurrency(remaining)}" else "Still to pay: ${Formatters.formatCurrency(remaining)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate900,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Due date & Actions: Record Repayment & Settle this
            if (!isConfirmed) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (dueInfo.state) {
                        DueState.OVERDUE -> {
                            Text(
                                text = dueInfo.formattedStatus,
                                style = MaterialTheme.typography.labelSmall,
                                color = Red600,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        DueState.DUE_TODAY -> {
                            Text(
                                text = "Due today",
                                style = MaterialTheme.typography.labelSmall,
                                color = Amber700,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        DueState.UPCOMING -> {
                            Text(
                                text = dueInfo.formattedStatus,
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate600,
                                fontWeight = FontWeight.Normal
                            )
                        }
                        DueState.NONE -> {
                            Text(
                                text = "No due date",
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate400
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (isLent) "Record repayment" else "Record payment",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isLent) EmeraldGreenDark else CoralOrangeDark,
                            modifier = Modifier
                                .clickable(onClick = onRepayClick)
                                .padding(4.dp)
                                .testTag("button_repay_${tx.id}")
                        )

                        Text(
                            text = "Settle this",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate600,
                            modifier = Modifier
                                .clickable(onClick = onSettleClick)
                                .padding(4.dp)
                                .testTag("button_settle_${tx.id}")
                        )
                    }
                }
            } else if (tx.dueDate != null) {
                // Historical due date on settled transaction (active overdue removed)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Due ${DueDateHelper.formatDueDate(tx.dueDate)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate400
                    )
                }
            }
        }
    }
}

@Composable
internal fun IndividualSettlementDialog(
    friendName: String,
    tx: TransactionEntity,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val isLent = tx.direction == TransactionDirection.LENT
    val remaining = tx.effectiveRemainingAmount
    val originalAmount = tx.amount
    val alreadyPaid = tx.effectivePaidAmount
    val hasPartialPayment = alreadyPaid > 0

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Mark this as fully paid?",
                fontWeight = FontWeight.Bold,
                color = Slate900
            )
        },
        text = {
            Column {
                val bodyText = if (isLent) {
                    "${Formatters.formatCurrency(remaining)} is still pending from $friendName. This will mark this transaction as settled."
                } else {
                    "${Formatters.formatCurrency(remaining)} is still left for you to pay $friendName. This will mark this transaction as settled."
                }
                Text(
                    text = bodyText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate700
                )
                if (hasPartialPayment) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isLent) {
                            "Original: ${Formatters.formatCurrency(originalAmount)} (${Formatters.formatCurrency(alreadyPaid)} paid back)"
                        } else {
                            "Original: ${Formatters.formatCurrency(originalAmount)} (${Formatters.formatCurrency(alreadyPaid)} paid)"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("button_confirm_settle_individual")
            ) {
                Text("Mark as paid", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("button_cancel_settle_individual")
            ) {
                Text("Keep pending", color = Slate700)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
internal fun RepaymentDialog(
    friendName: String,
    tx: TransactionEntity,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isLent = tx.direction == TransactionDirection.LENT
    val remaining = tx.effectiveRemainingAmount
    val originalAmount = tx.amount
    val alreadyPaid = tx.effectivePaidAmount

    var amountInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<UiMessage?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val parsedAmount = amountInput.toDoubleOrNull()
    val ctaText = when {
        parsedAmount != null && parsedAmount > 0 -> "Record ${Formatters.formatCurrency(parsedAmount)}"
        isLent -> "Record repayment"
        else -> "Record payment"
    }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isLent) "Record Repayment" else "Record Payment",
                fontWeight = FontWeight.Bold,
                color = Slate900
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Direction context
                Text(
                    text = if (isLent) {
                        "$friendName owes you ${Formatters.formatCurrency(originalAmount)}"
                    } else {
                        "You owe $friendName ${Formatters.formatCurrency(originalAmount)}"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Breakdown of already repaid and remaining
                if (alreadyPaid > 0) {
                    Text(
                        text = if (isLent) {
                            "Paid back: ${Formatters.formatCurrency(alreadyPaid)}"
                        } else {
                            "You paid: ${Formatters.formatCurrency(alreadyPaid)}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate600
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                Text(
                    text = if (isLent) {
                        "Still pending: ${Formatters.formatCurrency(remaining)}"
                    } else {
                        "Still to pay: ${Formatters.formatCurrency(remaining)}"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isLent) EmeraldGreenDark else CoralOrangeDark
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Amount text field
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d{0,2}$"""))) {
                            amountInput = input
                            errorMessage = null
                        }
                    },
                    label = { Text(if (isLent) "Repayment amount" else "Payment amount") },
                    placeholder = { Text("Enter amount") },
                    prefix = { Text("₹ ", fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = if (errorMessage != null) {
                        {
                            Text(
                                text = errorMessage!!.asString(context),
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.testTag("text_repayment_error")
                            )
                        }
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_repayment_amount")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quick fill button for remaining balance
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            val roundedRem = kotlin.math.round(remaining * 100.0) / 100.0
                            amountInput = if (roundedRem % 1.0 == 0.0) {
                                roundedRem.toLong().toString()
                            } else {
                                roundedRem.toString()
                            }
                            errorMessage = null
                        },
                        modifier = Modifier.testTag("button_fill_remaining")
                    ) {
                        Text(
                            text = "Fill remaining (${Formatters.formatCurrency(remaining)})",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isLent) EmeraldGreenDark else CoralOrangeDark
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isSubmitting) return@Button
                    val trimmed = amountInput.trim()
                    if (trimmed.isEmpty()) {
                        errorMessage = UiMessage(R.string.error_repayment_amount_invalid)
                        return@Button
                    }
                    val amount = trimmed.toDoubleOrNull()
                    if (amount == null || amount.isNaN() || amount.isInfinite() || amount <= 0.0) {
                        errorMessage = UiMessage(R.string.error_repayment_amount_invalid)
                        return@Button
                    }
                    if (amount > remaining + 0.0001) {
                        val formattedRem = Formatters.formatCurrency(remaining)
                        errorMessage = UiMessage(R.string.error_repayment_exceeds_remaining, formattedRem)
                        return@Button
                    }

                    isSubmitting = true
                    onConfirm(amount)
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLent) EmeraldGreen else CoralOrange
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("button_confirm_record_repayment")
            ) {
                Text(ctaText, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("button_cancel_record_repayment")
            ) {
                Text("Cancel", color = Slate700)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
internal fun BulkSettlementDialog(
    friendName: String,
    eligibility: BulkSettlementEligibility,
    totalRemaining: Double,
    openCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val isLent = eligibility == BulkSettlementEligibility.SAME_DIRECTION_LENT

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Settle all pending transactions?",
                fontWeight = FontWeight.Bold,
                color = Slate900
            )
        },
        text = {
            val bodyText = if (isLent) {
                "This will mark all $openCount pending transaction${if (openCount == 1) "" else "s"} (${Formatters.formatCurrency(totalRemaining)}) from $friendName as fully paid."
            } else {
                "This will mark all $openCount pending transaction${if (openCount == 1) "" else "s"} (${Formatters.formatCurrency(totalRemaining)}) to $friendName as fully paid."
            }
            Text(
                text = bodyText,
                style = MaterialTheme.typography.bodyMedium,
                color = Slate700
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("button_confirm_settle_bulk")
            ) {
                Text("Mark all as paid", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("button_cancel_settle_bulk")
            ) {
                Text("Keep pending", color = Slate700)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp)
    )
}
