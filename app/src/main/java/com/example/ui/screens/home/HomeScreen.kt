package com.example.ui.screens.home

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import com.example.domain.ReliabilityLevel
import com.example.ui.components.ReliabilityPill
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.FriendWithBalance
import com.example.data.model.TransactionDirection
import com.example.data.model.TransactionStatus
import com.example.data.model.TransactionWithFriend
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
import com.example.ui.viewmodel.HomeUiState
import com.example.ui.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToAddTransaction: (Long?) -> Unit,
    onNavigateToFriendDetail: (Long) -> Unit,
    onNavigateToActivity: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showQuickAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigateToAddTransaction(null) },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add Transaction") },
                text = { Text("Add", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                containerColor = EmeraldGreen,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                modifier = Modifier.testTag("fab_add_transaction")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // App Header
            item {
                HomeHeader(
                    userName = uiState.userName,
                    onActivityClick = onNavigateToActivity,
                    onSettingsClick = onNavigateToSettings
                )
            }

            // Top Summary Card (Side-by-side numbers + Net line)
            item {
                TopSummaryCard(
                    youWillGetBack = uiState.youWillGetBack,
                    youOwe = uiState.youOwe,
                    netPosition = uiState.netPosition
                )
            }

            // Search / Filter Bar for friends
            item {
                SearchAndFilterBar(
                    query = uiState.searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChange(it) },
                    onQuickAddClick = { showQuickAddDialog = true }
                )
            }

            // Friend List Section
            if (uiState.friends.isEmpty()) {
                item {
                    EmptyFriendsState(
                        isSearching = uiState.searchQuery.isNotBlank(),
                        onAddFriendClick = { showQuickAddDialog = true },
                        onAddTransactionClick = { onNavigateToAddTransaction(null) }
                    )
                }
            } else {
                item {
                    Text(
                        text = "FRIENDS (${uiState.friends.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate500,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }

                items(
                    items = uiState.friends,
                    key = { "friend_${it.friend.id}" }
                ) { friendWithBalance ->
                    FriendRowItem(
                        friendWithBalance = friendWithBalance,
                        onClick = { onNavigateToFriendDetail(friendWithBalance.friend.id) }
                    )
                }
            }

            // Bottom Section: Recent Activity Feed (last 5 transactions across all friends)
            if (uiState.recentActivity.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "RECENT ACTIVITY",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate500,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }

                items(
                    items = uiState.recentActivity,
                    key = { "activity_${it.transaction.id}" }
                ) { activityItem ->
                    RecentActivityRow(
                        item = activityItem,
                        onClick = { onNavigateToFriendDetail(activityItem.transaction.friendId) }
                    )
                }
            }
        }
    }

    if (showQuickAddDialog) {
        QuickAddFriendDialog(
            onDismiss = { showQuickAddDialog = false },
            onConfirm = { friendName ->
                viewModel.quickAddFriend(friendName) { newId ->
                    showQuickAddDialog = false
                    onNavigateToFriendDetail(newId)
                }
            }
        )
    }
}

@Composable
private fun HomeHeader(
    userName: String,
    onActivityClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Phittoos",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = Slate900,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "Tera Mera Hisaab Phittoos",
                style = MaterialTheme.typography.bodySmall,
                color = Slate500,
                fontWeight = FontWeight.Medium
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onActivityClick,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Slate100)
                    .testTag("btn_activity")
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "Activity History",
                    tint = Slate800,
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Slate100)
                    .testTag("btn_settings")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Slate800,
                    modifier = Modifier.size(24.dp)
                )
            }

            if (userName.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(EmeraldGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = userName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate800
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopSummaryCard(
    youWillGetBack: Double,
    youOwe: Double,
    netPosition: Double
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("top_summary_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Two numbers side by side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // You'll get back (Green)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(EmeraldGreenSurface)
                        .padding(16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = EmeraldGreenDark,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "You'll get back",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreenDark
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = Formatters.formatCurrency(youWillGetBack),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldGreenDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // You owe (Orange/Red)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CoralOrangeSurface)
                        .padding(16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = CoralOrangeDark,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "You owe",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = CoralOrangeDark
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = Formatters.formatCurrency(youOwe),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = CoralOrangeDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Slate200.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(14.dp))

            // Net position line underneath
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Overall",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate500,
                    fontWeight = FontWeight.Medium
                )

                val netText = when {
                    netPosition > 0 -> "You'll receive ${Formatters.formatCurrency(netPosition)}"
                    netPosition < 0 -> "You need to pay ${Formatters.formatCurrency(kotlin.math.abs(netPosition))}"
                    else -> "All settled up (₹0)"
                }

                val netColor = when {
                    netPosition > 0 -> EmeraldGreenDark
                    netPosition < 0 -> CoralOrangeDark
                    else -> Slate700
                }

                Text(
                    text = netText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = netColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SearchAndFilterBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onQuickAddClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .testTag("search_friends_input"),
            placeholder = { Text("Search friends...", color = Slate400) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Slate400
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = Slate500
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
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

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = onQuickAddClick,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .testTag("button_quick_add_friend")
        ) {
            Icon(
                imageVector = Icons.Default.PersonAdd,
                contentDescription = "Add Friend",
                tint = Slate800
            )
        }
    }
}

@Composable
private fun FriendRowItem(
    friendWithBalance: FriendWithBalance,
    onClick: () -> Unit
) {
    val net = friendWithBalance.netBalance

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable(onClick = onClick)
            .testTag("friend_row_${friendWithBalance.friend.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarInitial(
                name = friendWithBalance.friend.name,
                size = 48.dp
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = friendWithBalance.friend.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (friendWithBalance.reliabilityInfo.level != ReliabilityLevel.NEW) {
                        Spacer(modifier = Modifier.width(8.dp))
                        ReliabilityPill(level = friendWithBalance.reliabilityInfo.level)
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = if (friendWithBalance.lastActivityDate != null) {
                        "Activity: ${Formatters.formatDate(friendWithBalance.lastActivityDate)}"
                    } else {
                        "No transactions yet"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Balance text
            Column(
                horizontalAlignment = Alignment.End
            ) {
                when {
                    net > 0 -> {
                        Text(
                            text = "owes you",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate500,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = Formatters.formatCurrency(net),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldGreenDark
                        )
                    }
                    net < 0 -> {
                        Text(
                            text = "you owe",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate500,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = Formatters.formatCurrency(kotlin.math.abs(net)),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = CoralOrangeDark
                        )
                    }
                    friendWithBalance.openTransactionsCount > 0 -> {
                        Text(
                            text = "Net ₹0",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Slate700
                        )
                    }
                    else -> {
                        Text(
                            text = "All settled",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Slate500
                        )
                    }
                }

                if (friendWithBalance.overdueTransactionsCount > 0) {
                    Text(
                        text = "${friendWithBalance.overdueTransactionsCount} overdue",
                        style = MaterialTheme.typography.labelSmall,
                        color = CoralOrangeDark,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Slate200,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun EmptyFriendsState(
    isSearching: Boolean,
    onAddFriendClick: () -> Unit,
    onAddTransactionClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("empty_friends_state"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(EmeraldGreenSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = EmeraldGreenDark,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isSearching) "No friends matching your search" else "Add your first friend to get started",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Slate900,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Track informal loans and borrowings cleanly with one tap.",
                style = MaterialTheme.typography.bodyMedium,
                color = Slate500,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onAddFriendClick,
                colors = ButtonDefaults.buttonColors(containerColor = Slate900),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.testTag("button_empty_add_friend")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Friend", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RecentActivityRow(
    item: TransactionWithFriend,
    onClick: () -> Unit
) {
    val tx = item.transaction
    val isLent = tx.direction == TransactionDirection.LENT

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick)
            .testTag("recent_activity_row_${tx.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
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
                    text = if (isLent) "Lent to ${item.friendName}" else "Borrowed from ${item.friendName}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!tx.note.isNullOrBlank()) {
                    Text(
                        text = tx.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = Formatters.formatDate(tx.createdDate),
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate400
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Formatters.formatCurrency(tx.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isLent) EmeraldGreenDark else CoralOrangeDark
                )

                if (tx.status == TransactionStatus.CONFIRMED) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Settled",
                            tint = EmeraldGreen,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Settled",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldGreenDark,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.HourglassTop,
                            contentDescription = "Open",
                            tint = Slate400,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Open",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate500
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAddFriendDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Text(
                "Add Friend",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Slate900
            )
        },
        text = {
            Column {
                Text(
                    "Enter friend's name to track loans and borrowings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate500
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Friend's Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_friend_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Slate900,
                        unfocusedTextColor = Slate900,
                        cursorColor = EmeraldGreen,
                        focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = Slate200,
                        focusedLabelColor = EmeraldGreen,
                        unfocusedLabelColor = Slate500,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedPlaceholderColor = Slate400,
                        unfocusedPlaceholderColor = Slate400
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) onConfirm(name.trim())
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                modifier = Modifier.testTag("dialog_confirm_add_friend")
            ) {
                Text("Add", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Slate500)
            }
        }
    )
}
