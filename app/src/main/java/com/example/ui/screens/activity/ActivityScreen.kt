package com.example.ui.screens.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ActivityType
import com.example.data.model.NeedsAttentionItem
import com.example.data.model.TransactionDirection
import com.example.ui.components.AvatarInitial
import com.example.ui.theme.CoralOrange
import com.example.ui.theme.CoralOrangeDark
import com.example.ui.theme.CoralOrangeSurface
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.EmeraldGreenSurface
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.util.Formatters
import com.example.ui.viewmodel.ActivityDisplayItem
import com.example.ui.viewmodel.ActivityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    viewModel: ActivityViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToFriendDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("activity_screen"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Activity",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = "Recent money activity",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500
                        )
                    }
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 48.dp)
        ) {
            // 1. Needs Attention section (live overdue items)
            if (uiState.needsAttentionItems.isNotEmpty()) {
                item {
                    Text(
                        text = "NEEDS ATTENTION (${uiState.needsAttentionItems.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = CoralOrangeDark,
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .testTag("header_needs_attention")
                    )
                }

                items(
                    items = uiState.needsAttentionItems,
                    key = { "overdue_${it.transactionId}" }
                ) { item ->
                    NeedsAttentionCard(
                        item = item,
                        onClick = { onNavigateToFriendDetail(item.friendId) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                item {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // 2. Empty State (when both needs attention and activities are empty)
            if (uiState.isEmpty) {
                item {
                    EmptyActivityState()
                }
            } else if (uiState.groupedActivities.isNotEmpty()) {
                // 3. Recent Activity timeline
                item {
                    Text(
                        text = "RECENT ACTIVITY",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate500,
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .testTag("header_recent_activity")
                    )
                }

                uiState.groupedActivities.forEach { (dateHeader, activities) ->
                    item(key = "header_$dateHeader") {
                        Text(
                            text = dateHeader.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Slate400,
                            modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                        )
                    }

                    items(
                        items = activities,
                        key = { "act_${it.id}" }
                    ) { actItem ->
                        ActivityRowCard(
                            item = actItem,
                            onClick = { onNavigateToFriendDetail(actItem.friendId) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun NeedsAttentionCard(
    item: NeedsAttentionItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("card_needs_attention_${item.transactionId}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CoralOrangeSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(CoralOrange.copy(alpha = 0.3f))
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CoralOrange.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = CoralOrangeDark,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.friendName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${Formatters.formatCurrency(item.remainingAmount)} remaining",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate800
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "· ${item.formattedStatus}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = CoralOrangeDark
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "View Friend",
                tint = CoralOrangeDark,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ActivityRowCard(
    item: ActivityDisplayItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("activity_item_${item.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Event Icon Box
            val (icon, iconColor, bgColor) = getActivityIconConfig(item.type, item.direction, item.note)

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Main Info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.friendName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (item.formattedTime.isNotBlank()) {
                        Text(
                            text = item.formattedTime,
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate400,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = item.eventDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Slate700
                )

                if (!item.note.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Slate100,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = item.note,
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate500,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyActivityState() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp)
            .testTag("empty_activity_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Slate100),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = Slate400,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No activity yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Slate800
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Transactions, repayments and settlements will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = Slate500,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun getActivityIconConfig(
    type: ActivityType,
    direction: TransactionDirection?,
    note: String?
): Triple<ImageVector, Color, Color> {
    return when (type) {
        ActivityType.TRANSACTION_CREATED -> {
            if (direction == TransactionDirection.LENT) {
                Triple(Icons.AutoMirrored.Filled.TrendingUp, EmeraldGreenDark, EmeraldGreenSurface)
            } else {
                Triple(Icons.AutoMirrored.Filled.TrendingDown, CoralOrangeDark, CoralOrangeSurface)
            }
        }
        ActivityType.PARTIAL_REPAYMENT -> {
            if (direction == TransactionDirection.LENT) {
                Triple(Icons.Default.Payments, EmeraldGreenDark, EmeraldGreenSurface)
            } else {
                Triple(Icons.Default.Payments, CoralOrangeDark, CoralOrangeSurface)
            }
        }
        ActivityType.SETTLED -> {
            Triple(Icons.Default.CheckCircle, EmeraldGreenDark, EmeraldGreenSurface)
        }
        ActivityType.BECAME_OVERDUE -> {
            Triple(Icons.Default.WarningAmber, CoralOrangeDark, CoralOrangeSurface)
        }
        ActivityType.REMINDER_SENT -> {
            Triple(Icons.Default.Notifications, CoralOrangeDark, CoralOrangeSurface)
        }
    }
}
