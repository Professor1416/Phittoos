package com.example.data.repository

import com.example.data.dao.ActivityDao
import com.example.data.dao.FriendDao
import com.example.data.dao.TransactionDao
import com.example.data.model.ActivityEntity
import com.example.data.model.ActivityType
import com.example.data.model.ActivityWithFriend
import com.example.data.model.Friend
import com.example.data.model.FriendWithBalance
import com.example.data.model.NeedsAttentionItem
import com.example.data.model.ReminderStage
import com.example.data.model.TransactionDirection
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionStatus
import com.example.data.model.TransactionWithFriend
import com.example.data.model.dueInfo
import com.example.data.model.effectiveRemainingAmount
import com.example.domain.ReliabilityEngine
import androidx.room.withTransaction
import com.example.data.db.AppDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

sealed class RepaymentResult {
    data class Success(
        val updatedTransaction: TransactionEntity,
        val isFullySettled: Boolean
    ) : RepaymentResult()

    data class Error(
        val message: String
    ) : RepaymentResult()
}

class PhittoosRepository(
    private val friendDao: FriendDao,
    private val transactionDao: TransactionDao,
    private val activityDao: ActivityDao? = null,
    private val database: AppDatabase? = null
) {
    private suspend fun <T> runInTransaction(block: suspend () -> T): T {
        val db = database
        return if (db != null) {
            db.withTransaction { block() }
        } else {
            block()
        }
    }
    val allFriends: Flow<List<Friend>> = friendDao.getAllFriends()
    val recentFriends: Flow<List<Friend>> = friendDao.getRecentFriendsWithTransactions(8)
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val allActivities: Flow<List<ActivityEntity>> = activityDao?.getAllActivities() ?: flowOf(emptyList())

    val activitiesWithFriend: Flow<List<ActivityWithFriend>> = combine(
        allActivities,
        friendDao.getAllFriends()
    ) { activities, friends ->
        val friendMap = friends.associateBy { it.id }
        activities.map { act ->
            ActivityWithFriend(
                activity = act,
                friendName = friendMap[act.friendId]?.name ?: "Friend"
            )
        }
    }

    val needsAttentionItems: Flow<List<NeedsAttentionItem>> = combine(
        transactionDao.getAllTransactions(),
        friendDao.getAllFriends()
    ) { transactions, friends ->
        val friendMap = friends.associateBy { it.id }
        transactions
            .filter { it.status == TransactionStatus.OPEN && it.dueInfo.isActivelyOverdue && it.effectiveRemainingAmount > 0.0 }
            .sortedByDescending { it.dueInfo.overdueDays }
            .map { tx ->
                NeedsAttentionItem(
                    transactionId = tx.id,
                    friendId = tx.friendId,
                    friendName = friendMap[tx.friendId]?.name ?: "Friend",
                    direction = tx.direction,
                    remainingAmount = tx.effectiveRemainingAmount,
                    overdueDays = tx.dueInfo.overdueDays,
                    dueDate = tx.dueDate ?: 0L,
                    formattedStatus = tx.dueInfo.formattedStatus
                )
            }
    }

    val friendsWithBalance: Flow<List<FriendWithBalance>> = combine(
        friendDao.getAllFriends(),
        transactionDao.getAllTransactions()
    ) { friends, transactions ->
        val txByFriend = transactions.groupBy { it.friendId }

        friends.map { friend ->
            val friendTxs = txByFriend[friend.id] ?: emptyList()
            var net = 0.0
            var openCount = 0
            var overdueCount = 0

            for (tx in friendTxs) {
                if (tx.status == TransactionStatus.OPEN) {
                    openCount++
                    if (tx.dueInfo.isActivelyOverdue) {
                        overdueCount++
                    }
                    val effectiveAmount = tx.effectiveRemainingAmount
                    if (tx.direction == TransactionDirection.LENT) {
                        net += effectiveAmount
                    } else {
                        net -= effectiveAmount
                    }
                }
            }

            val lastActivity = friendTxs.maxOfOrNull { it.createdDate }
            val reliability = ReliabilityEngine.calculate(friendTxs)

            FriendWithBalance(
                friend = friend,
                netBalance = net,
                lastActivityDate = lastActivity,
                openTransactionsCount = openCount,
                totalTransactionsCount = friendTxs.size,
                overdueTransactionsCount = overdueCount,
                reliabilityInfo = reliability
            )
        }
    }

    data class DashboardTotals(
        val youWillGetBack: Double,
        val youOwe: Double,
        val netPosition: Double
    )

    val dashboardTotals: Flow<DashboardTotals> = transactionDao.getAllTransactions().map { transactions ->
        var lentTotal = 0.0
        var borrowedTotal = 0.0

        for (tx in transactions) {
            if (tx.status == TransactionStatus.OPEN) {
                val effectiveAmount = tx.effectiveRemainingAmount
                if (tx.direction == TransactionDirection.LENT) {
                    lentTotal += effectiveAmount
                } else {
                    borrowedTotal += effectiveAmount
                }
            }
        }

        DashboardTotals(
            youWillGetBack = lentTotal,
            youOwe = borrowedTotal,
            netPosition = lentTotal - borrowedTotal
        )
    }

    val recentActivity: Flow<List<TransactionWithFriend>> = combine(
        friendDao.getAllFriends(),
        transactionDao.getRecentTransactions(5)
    ) { friends, recentTxs ->
        val friendMap = friends.associateBy { it.id }
        recentTxs.map { tx ->
            val friendName = friendMap[tx.friendId]?.name ?: "Friend"
            TransactionWithFriend(
                transaction = tx,
                friendName = friendName
            )
        }
    }

    fun getFriendById(friendId: Long): Flow<Friend?> = friendDao.getFriendById(friendId)

    fun getTransactionsForFriend(friendId: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsForFriend(friendId)

    fun getFriendWithBalance(friendId: Long): Flow<FriendWithBalance?> = combine(
        friendDao.getFriendById(friendId),
        transactionDao.getTransactionsForFriend(friendId)
    ) { friend, friendTxs ->
        if (friend == null) return@combine null

        var net = 0.0
        var openCount = 0
        var overdueCount = 0

        for (tx in friendTxs) {
            if (tx.status == TransactionStatus.OPEN) {
                openCount++
                if (tx.dueInfo.isActivelyOverdue) {
                    overdueCount++
                }
                val effectiveAmount = tx.effectiveRemainingAmount
                if (tx.direction == TransactionDirection.LENT) {
                    net += effectiveAmount
                } else {
                    net -= effectiveAmount
                }
            }
        }

        val lastActivity = friendTxs.maxOfOrNull { it.createdDate }
        val reliability = ReliabilityEngine.calculate(friendTxs)

        FriendWithBalance(
            friend = friend,
            netBalance = net,
            lastActivityDate = lastActivity,
            openTransactionsCount = openCount,
            totalTransactionsCount = friendTxs.size,
            overdueTransactionsCount = overdueCount,
            reliabilityInfo = reliability
        )
    }

    val openTransactionsWithDueDate: Flow<List<TransactionEntity>> =
        transactionDao.getOpenTransactionsWithDueDate()

    suspend fun getOpenTransactionsWithDueDate(): List<TransactionEntity> =
        transactionDao.getOpenTransactionsWithDueDateList()

    suspend fun insertFriend(name: String, contactInfo: String? = null): Long {
        val trimmed = name.trim()
        val existing = friendDao.getFriendByName(trimmed)
        if (existing != null) {
            return existing.id
        }
        return friendDao.insertFriend(
            Friend(name = trimmed, contactInfo = contactInfo)
        )
    }

    suspend fun addTransaction(
        friendId: Long,
        amount: Double,
        direction: TransactionDirection,
        note: String? = null,
        dueDate: Long? = null
    ): Long {
        return runInTransaction {
            val trimmedNote = note?.trim()?.ifBlank { null }
            val txId = transactionDao.insertTransaction(
                TransactionEntity(
                    friendId = friendId,
                    amount = amount,
                    direction = direction,
                    note = trimmedNote,
                    dueDate = dueDate,
                    status = TransactionStatus.OPEN
                )
            )
            activityDao?.insertActivity(
                ActivityEntity(
                    type = ActivityType.TRANSACTION_CREATED,
                    friendId = friendId,
                    transactionId = txId,
                    amount = amount,
                    direction = direction,
                    note = trimmedNote,
                    createdAt = System.currentTimeMillis()
                )
            )
            txId
        }
    }

    suspend fun getTransactionById(transactionId: Long): TransactionEntity? {
        return transactionDao.getTransactionById(transactionId)
    }

    suspend fun recordRepayment(
        transactionId: Long,
        repaymentAmount: Double,
        settledAt: Long = System.currentTimeMillis()
    ): RepaymentResult {
        if (repaymentAmount <= 0.0 || repaymentAmount.isNaN() || repaymentAmount.isInfinite()) {
            return RepaymentResult.Error("Repayment amount must be greater than ₹0")
        }

        return runInTransaction {
            val tx = transactionDao.getTransactionById(transactionId)
                ?: return@runInTransaction RepaymentResult.Error("Transaction not found")

            if (tx.status != TransactionStatus.OPEN) {
                return@runInTransaction RepaymentResult.Error("Transaction is already settled")
            }

            val remaining = tx.effectiveRemainingAmount
            if (repaymentAmount > remaining + 0.0001) {
                return@runInTransaction RepaymentResult.Error("Amount exceeds remaining balance of $remaining")
            }

            val currentEffectivePaid = (tx.paidAmount ?: 0.0).coerceIn(0.0, tx.amount)
            val unroundedPaid = currentEffectivePaid + repaymentAmount
            val newPaid = kotlin.math.round(unroundedPaid * 100.0) / 100.0
            val isFullySettled = newPaid >= tx.amount - 0.005

            val finalPaid = if (isFullySettled) tx.amount else newPaid.coerceAtMost(tx.amount)
            val finalStatus = if (isFullySettled) TransactionStatus.CONFIRMED else TransactionStatus.OPEN
            val settledTimestamp = if (isFullySettled) settledAt else null

            val rowsUpdated = transactionDao.updateRepayment(transactionId, finalPaid, finalStatus, settledTimestamp)
            if (rowsUpdated == 0) {
                return@runInTransaction RepaymentResult.Error("Transaction is no longer open")
            }

            // Record activity event
            if (isFullySettled) {
                activityDao?.insertActivity(
                    ActivityEntity(
                        type = ActivityType.SETTLED,
                        friendId = tx.friendId,
                        transactionId = tx.id,
                        amount = repaymentAmount,
                        direction = tx.direction,
                        note = "final_repayment",
                        createdAt = settledAt
                    )
                )
            } else {
                activityDao?.insertActivity(
                    ActivityEntity(
                        type = ActivityType.PARTIAL_REPAYMENT,
                        friendId = tx.friendId,
                        transactionId = tx.id,
                        amount = repaymentAmount,
                        direction = tx.direction,
                        note = null,
                        createdAt = settledAt
                    )
                )
            }

            val updatedTx = tx.copy(paidAmount = finalPaid, status = finalStatus, settledAt = settledTimestamp)
            RepaymentResult.Success(updatedTx, isFullySettled)
        }
    }

    suspend fun markTransactionAsPaid(
        transactionId: Long,
        settledAt: Long = System.currentTimeMillis()
    ) {
        runInTransaction {
            val tx = transactionDao.getTransactionById(transactionId) ?: return@runInTransaction
            if (tx.status != TransactionStatus.OPEN) return@runInTransaction // Idempotent check

            val remaining = tx.effectiveRemainingAmount
            transactionDao.markAsConfirmed(transactionId, settledAt)
            activityDao?.insertActivity(
                ActivityEntity(
                    type = ActivityType.SETTLED,
                    friendId = tx.friendId,
                    transactionId = tx.id,
                    amount = remaining,
                    direction = tx.direction,
                    note = null,
                    createdAt = settledAt
                )
            )
        }
    }

    suspend fun settleAllSameDirectionForFriend(
        friendId: Long,
        settledAt: Long = System.currentTimeMillis()
    ): Boolean {
        return runInTransaction {
            val openTxs = transactionDao.getOpenTransactionsForFriend(friendId)
            if (openTxs.isEmpty()) return@runInTransaction false
            val hasLent = openTxs.any { it.direction == TransactionDirection.LENT }
            val hasBorrowed = openTxs.any { it.direction == TransactionDirection.BORROWED }
            if (hasLent && hasBorrowed) {
                // Mixed direction safety guard: do NOT bulk-settle when transactions are in both directions
                return@runInTransaction false
            }
            transactionDao.markAllOpenForFriendAsConfirmed(friendId, settledAt)
            for (tx in openTxs) {
                val remaining = tx.effectiveRemainingAmount
                activityDao?.insertActivity(
                    ActivityEntity(
                        type = ActivityType.SETTLED,
                        friendId = friendId,
                        transactionId = tx.id,
                        amount = remaining,
                        direction = tx.direction,
                        note = null,
                        createdAt = settledAt
                    )
                )
            }
            true
        }
    }

    suspend fun markAllForFriendAsPaid(
        friendId: Long,
        settledAt: Long = System.currentTimeMillis()
    ): Boolean {
        return settleAllSameDirectionForFriend(friendId, settledAt)
    }

    suspend fun getFriendByIdOnce(friendId: Long): Friend? {
        return friendDao.getFriendByIdOnce(friendId)
    }

    suspend fun getSentReminderStagesForTransaction(transactionId: Long): Set<ReminderStage> {
        return activityDao?.getSentReminderStagesForTransaction(transactionId)?.toSet() ?: emptySet()
    }

    suspend fun recordReminderSent(
        transactionId: Long,
        friendId: Long,
        stage: ReminderStage,
        amount: Double,
        direction: TransactionDirection = TransactionDirection.LENT,
        timestamp: Long = System.currentTimeMillis()
    ): Long? {
        val alreadySent = activityDao?.getSentReminderStagesForTransaction(transactionId) ?: emptyList()
        if (stage in alreadySent) {
            return null // Idempotency guard: never record duplicate for the same stage
        }
        return activityDao?.insertActivity(
            ActivityEntity(
                type = ActivityType.REMINDER_SENT,
                friendId = friendId,
                transactionId = transactionId,
                amount = amount,
                direction = direction,
                note = "${stage.daysOverdueThreshold}-day reminder sent",
                reminderStage = stage,
                createdAt = timestamp
            )
        )
    }

    suspend fun clearAllData() {
        runInTransaction {
            activityDao?.deleteAllActivities()
            transactionDao.deleteAllTransactions()
            friendDao.deleteAllFriends()
        }
    }

    suspend fun getAllTransactionsForExport(): List<TransactionEntity> {
        return transactionDao.getAllTransactionsOrdered()
    }

    suspend fun getAllFriendsForExport(): List<Friend> {
        return friendDao.getAllFriendsList()
    }
}
