package com.professor1416.phittoos.ui.screens.home

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.professor1416.phittoos.R
import com.professor1416.phittoos.data.model.FriendWithBalance
import com.professor1416.phittoos.data.model.TransactionDirection
import com.professor1416.phittoos.data.model.TransactionStatus
import com.professor1416.phittoos.data.model.TransactionWithFriend
import com.professor1416.phittoos.domain.ReliabilityLevel
import com.professor1416.phittoos.ui.components.AvatarInitial
import com.professor1416.phittoos.ui.components.ReliabilityPill
import com.professor1416.phittoos.ui.theme.EmeraldGreen
import com.professor1416.phittoos.ui.theme.EmeraldGreenDark
import com.professor1416.phittoos.ui.theme.EmeraldGreenSurface
import com.professor1416.phittoos.ui.theme.PhittoosColors
import com.professor1416.phittoos.ui.util.Formatters
import com.professor1416.phittoos.ui.viewmodel.HomeViewModel

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
                    netPosition = uiState.netPosition,
                    openTransactionsCount = uiState.openTransactionsCount
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
                        searchQuery = uiState.searchQuery,
                        onAddFriendClick = { showQuickAddDialog = true },
                        onAddTransactionClick = { onNavigateToAddTransaction(null) },
                        onClearSearch = { viewModel.onSearchQueryChange("") },
                        onAddSearchedFriend = { friendName ->
                            viewModel.quickAddFriend(friendName) { newId ->
                                onNavigateToFriendDetail(newId)
                            }
                        }
                    )
                }
            } else {
                item {
                    Text(
                        text = "FRIENDS (${uiState.friends.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = stringResource(id = R.string.tagline),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onActivityClick,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .testTag("btn_activity")
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "Activity History",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .testTag("btn_settings")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryBox(
    title: String,
    amount: Double,
    icon: ImageVector,
    tintColor: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .semantics(mergeDescendants = true) {}
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tintColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = tintColor
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = Formatters.formatCurrency(amount),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = tintColor
            )
        }
    }
}

@Composable
private fun TopSummaryCard(
    youWillGetBack: Double,
    youOwe: Double,
    netPosition: Double,
    openTransactionsCount: Int = 0
) {
    val financialColors = PhittoosColors.financial

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("top_summary_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        androidx.compose.foundation.layout.BoxWithConstraints(
            modifier = Modifier.fillMaxWidth()
        ) {
            val isNarrow = maxWidth < 340.dp
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                if (isNarrow) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SummaryBox(
                            title = "You'll get back",
                            amount = youWillGetBack,
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            tintColor = financialColors.onLentContainer,
                            backgroundColor = financialColors.lentContainer,
                            modifier = Modifier.fillMaxWidth()
                        )
                        SummaryBox(
                            title = "You owe",
                            amount = youOwe,
                            icon = Icons.AutoMirrored.Filled.TrendingDown,
                            tintColor = financialColors.onBorrowedContainer,
                            backgroundColor = financialColors.borrowedContainer,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SummaryBox(
                            title = "You'll get back",
                            amount = youWillGetBack,
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            tintColor = financialColors.onLentContainer,
                            backgroundColor = financialColors.lentContainer,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryBox(
                            title = "You owe",
                            amount = youOwe,
                            icon = Icons.AutoMirrored.Filled.TrendingDown,
                            tintColor = financialColors.onBorrowedContainer,
                            backgroundColor = financialColors.borrowedContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(14.dp))

                val netText = when {
                    netPosition > 0 -> "You'll receive ${Formatters.formatCurrency(netPosition)}"
                    netPosition < 0 -> "You need to pay ${Formatters.formatCurrency(kotlin.math.abs(netPosition))}"
                    openTransactionsCount > 0 -> "Overall balance ₹0"
                    else -> "All settled up (₹0)"
                }

                val netColor = when {
                    netPosition > 0 -> financialColors.lentAccent
                    netPosition < 0 -> financialColors.borrowedAccent
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                if (isNarrow) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Overall",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = netText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = netColor,
                            modifier = Modifier.testTag("top_summary_net_text")
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Overall",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = netText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = netColor,
                            modifier = Modifier.testTag("top_summary_net_text")
                        )
                    }
                }
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
            placeholder = {
                Text(
                    "Search friends...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        )

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = onQuickAddClick,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .testTag("button_quick_add_friend")
        ) {
            Icon(
                imageVector = Icons.Default.PersonAdd,
                contentDescription = "Add Friend",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FriendBalanceColumn(
    friendWithBalance: FriendWithBalance,
    horizontalAlignment: Alignment.Horizontal
) {
    val financialColors = PhittoosColors.financial
    val net = friendWithBalance.netBalance

    Column(
        horizontalAlignment = horizontalAlignment
    ) {
        when {
            net > 0 -> {
                Text(
                    text = "owes you",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = Formatters.formatCurrency(net),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = financialColors.lentAccent
                )
            }
            net < 0 -> {
                Text(
                    text = "you owe",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = Formatters.formatCurrency(kotlin.math.abs(net)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = financialColors.borrowedAccent
                )
            }
            friendWithBalance.openTransactionsCount > 0 -> {
                Text(
                    text = "Net ₹0",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> {
                Text(
                    text = "All settled",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (friendWithBalance.overdueTransactionsCount > 0) {
            Text(
                text = "${friendWithBalance.overdueTransactionsCount} overdue",
                style = MaterialTheme.typography.labelSmall,
                color = financialColors.overdueAccent,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun FriendRowItem(
    friendWithBalance: FriendWithBalance,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .testTag("friend_row_${friendWithBalance.friend.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        androidx.compose.foundation.layout.BoxWithConstraints(
            modifier = Modifier.fillMaxWidth()
        ) {
            val isNarrow = maxWidth < 340.dp

            if (isNarrow) {
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
                                color = MaterialTheme.colorScheme.onSurface,
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        FriendBalanceColumn(
                            friendWithBalance = friendWithBalance,
                            horizontalAlignment = Alignment.Start
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
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
                                color = MaterialTheme.colorScheme.onSurface,
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    FriendBalanceColumn(
                        friendWithBalance = friendWithBalance,
                        horizontalAlignment = Alignment.End
                    )

                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun EmptyFriendsState(
    isSearching: Boolean,
    searchQuery: String = "",
    onAddFriendClick: () -> Unit,
    onAddTransactionClick: () -> Unit,
    onClearSearch: () -> Unit = {},
    onAddSearchedFriend: (String) -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("empty_friends_state"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        if (isSearching) {
            // SEARCH EMPTY STATE
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                EmptySearchIllustration()

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.empty_search_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.empty_search_subtitle, searchQuery),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { onAddSearchedFriend(searchQuery) },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("button_empty_add_friend")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.empty_search_add_friend, searchQuery),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onClearSearch,
                    modifier = Modifier.testTag("button_empty_clear_search")
                ) {
                    Text(
                        text = stringResource(R.string.empty_search_clear),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            // MAIN ZERO-STATE / FIRST ENTRY ONBOARDING
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                EmptyHomeIllustration()

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.empty_home_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    letterSpacing = (-0.3).sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.empty_home_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Guided Step Cards
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    EmptyGuideStepItem(
                        stepNumber = "1",
                        title = stringResource(R.string.empty_home_step_1_title),
                        description = stringResource(R.string.empty_home_step_1_desc),
                        icon = Icons.Default.PersonAdd,
                        accentColor = PhittoosColors.financial.onLentContainer,
                        badgeBg = PhittoosColors.financial.lentContainer
                    )

                    EmptyGuideStepItem(
                        stepNumber = "2",
                        title = stringResource(R.string.empty_home_step_2_title),
                        description = stringResource(R.string.empty_home_step_2_desc),
                        icon = Icons.Default.Payments,
                        accentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        badgeBg = MaterialTheme.colorScheme.primaryContainer
                    )

                    EmptyGuideStepItem(
                        stepNumber = "3",
                        title = stringResource(R.string.empty_home_step_3_title),
                        description = stringResource(R.string.empty_home_step_3_desc),
                        icon = Icons.Default.Celebration,
                        accentColor = PhittoosColors.financial.onBorrowedContainer,
                        badgeBg = PhittoosColors.financial.borrowedContainer
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action CTA Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onAddTransactionClick,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("button_empty_add_transaction")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.empty_home_cta_primary),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onAddFriendClick,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("button_empty_add_friend")
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.empty_home_cta_secondary),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyGuideStepItem(
    stepNumber: String,
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    badgeBg: Color
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(badgeBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyHomeIllustration(modifier: Modifier = Modifier) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val outlineColor = MaterialTheme.colorScheme.outline
    val onSurfaceVar = MaterialTheme.colorScheme.onSurfaceVariant
    val mintSurface = EmeraldGreenSurface
    val emerald = EmeraldGreen
    val emeraldDark = EmeraldGreenDark
    val goldCoin = Color(0xFFFBBF24)
    val goldCoinDark = Color(0xFFD97706)

    Box(
        modifier = modifier
            .size(width = 190.dp, height = 135.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f

            // 1. Soft Ambient Glow Background
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        mintSurface.copy(alpha = 0.9f),
                        mintSurface.copy(alpha = 0.4f),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = size.width * 0.48f
                ),
                radius = size.width * 0.48f,
                center = Offset(cx, cy)
            )

            // 2. Main Ledger / Record Card (Centered with subtle tilt)
            val cardW = 100.dp.toPx()
            val cardH = 75.dp.toPx()
            val cardLeft = cx - cardW / 2f - 4.dp.toPx()
            val cardTop = cy - cardH / 2f + 4.dp.toPx()

            // Card shadow/background
            drawRoundRect(
                color = surfaceColor,
                topLeft = Offset(cardLeft, cardTop),
                size = Size(cardW, cardH),
                cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
            )
            drawRoundRect(
                color = outlineColor.copy(alpha = 0.4f),
                topLeft = Offset(cardLeft, cardTop),
                size = Size(cardW, cardH),
                cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Card Header Ribbon
            drawRoundRect(
                color = emerald.copy(alpha = 0.85f),
                topLeft = Offset(cardLeft, cardTop),
                size = Size(cardW, 16.dp.toPx()),
                cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
            )
            // Flatten bottom of ribbon
            drawRect(
                color = emerald.copy(alpha = 0.85f),
                topLeft = Offset(cardLeft, cardTop + 8.dp.toPx()),
                size = Size(cardW, 8.dp.toPx())
            )

            // Card Line Records
            val lineLeft = cardLeft + 14.dp.toPx()
            val lineY1 = cardTop + 28.dp.toPx()
            val lineY2 = cardTop + 42.dp.toPx()
            val lineY3 = cardTop + 56.dp.toPx()

            val lineCol = outlineColor.copy(alpha = 0.4f)

            // Line 1: Green bullet + line
            drawCircle(color = emeraldDark, radius = 2.5.dp.toPx(), center = Offset(lineLeft - 4.dp.toPx(), lineY1))
            drawLine(
                color = lineCol,
                start = Offset(lineLeft + 4.dp.toPx(), lineY1),
                end = Offset(cardLeft + cardW - 14.dp.toPx(), lineY1),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Line 2: Blue bullet + line
            drawCircle(color = onSurfaceVar, radius = 2.5.dp.toPx(), center = Offset(lineLeft - 4.dp.toPx(), lineY2))
            drawLine(
                color = lineCol,
                start = Offset(lineLeft + 4.dp.toPx(), lineY2),
                end = Offset(cardLeft + cardW - 24.dp.toPx(), lineY2),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Line 3: Soft bullet + line
            drawCircle(color = emerald, radius = 2.5.dp.toPx(), center = Offset(lineLeft - 4.dp.toPx(), lineY3))
            drawLine(
                color = lineCol.copy(alpha = 0.25f),
                start = Offset(lineLeft + 4.dp.toPx(), lineY3),
                end = Offset(cardLeft + cardW - 36.dp.toPx(), lineY3),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )

            // 3. Floating Gold Rupee Coin (Top Right)
            val coinRadius = 16.dp.toPx()
            val coinCenter = Offset(cx + 42.dp.toPx(), cy - 22.dp.toPx())

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(goldCoin, goldCoinDark),
                    center = coinCenter,
                    radius = coinRadius
                ),
                radius = coinRadius,
                center = coinCenter
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.5f),
                radius = coinRadius - 3.dp.toPx(),
                center = coinCenter,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Floating Small Coin (Left)
            val smallCoinRadius = 9.dp.toPx()
            val smallCoinCenter = Offset(cx - 52.dp.toPx(), cy + 18.dp.toPx())
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(emerald.copy(alpha = 0.9f), emeraldDark),
                    center = smallCoinCenter,
                    radius = smallCoinRadius
                ),
                radius = smallCoinRadius,
                center = smallCoinCenter
            )

            // 4. Floating Sparkle Stars
            val sparkle1 = Offset(cx + 62.dp.toPx(), cy + 16.dp.toPx())
            drawCircle(color = goldCoin, radius = 2.5.dp.toPx(), center = sparkle1)

            val sparkle2 = Offset(cx - 44.dp.toPx(), cy - 28.dp.toPx())
            drawCircle(color = emeraldDark, radius = 3.dp.toPx(), center = sparkle2)

            val sparkle3 = Offset(cx + 20.dp.toPx(), cy - 42.dp.toPx())
            drawCircle(color = goldCoin, radius = 2.dp.toPx(), center = sparkle3)
        }

        // Overlay Centered Emblem Badge (Handshake / Checkmark)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 4.dp)
                .size(34.dp)
                .clip(CircleShape)
                .background(EmeraldGreen)
                .border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun EmptySearchIllustration(modifier: Modifier = Modifier) {
    val financialColors = PhittoosColors.financial
    Box(
        modifier = modifier
            .size(90.dp)
            .clip(CircleShape)
            .background(financialColors.lentContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            tint = financialColors.onLentContainer,
            modifier = Modifier.size(44.dp)
        )
    }
}

@Composable
private fun RecentActivityRow(
    item: TransactionWithFriend,
    onClick: () -> Unit
) {
    val financialColors = PhittoosColors.financial
    val tx = item.transaction
    val isLent = tx.direction == TransactionDirection.LENT

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .testTag("recent_activity_row_${tx.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                    .background(if (isLent) financialColors.lentContainer else financialColors.borrowedContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isLent) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                    contentDescription = null,
                    tint = if (isLent) financialColors.onLentContainer else financialColors.onBorrowedContainer,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isLent) "Lent to ${item.friendName}" else "Borrowed from ${item.friendName}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!tx.note.isNullOrBlank()) {
                    Text(
                        text = tx.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = Formatters.formatDate(tx.createdDate),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Formatters.formatCurrency(tx.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isLent) financialColors.lentAccent else financialColors.borrowedAccent
                )

                if (tx.status == TransactionStatus.CONFIRMED) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldGreen,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Settled",
                            style = MaterialTheme.typography.labelSmall,
                            color = financialColors.onSettledContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.HourglassTop,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Open",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                "Add Friend",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column {
                Text(
                    "Enter friend's name to track money lent and borrowed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = EmeraldGreen,
                        focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = EmeraldGreen,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}
