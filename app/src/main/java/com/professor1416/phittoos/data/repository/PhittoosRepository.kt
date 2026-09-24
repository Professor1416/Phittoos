package com.professor1416.phittoos.data.repository

import com.professor1416.phittoos.data.dao.ActivityDao
import com.professor1416.phittoos.data.dao.FriendDao
import com.professor1416.phittoos.data.dao.TransactionDao
import com.professor1416.phittoos.data.model.ActivityEntity
import com.professor1416.phittoos.data.model.ActivityType
import com.professor1416.phittoos.data.model.ActivityWithFriend
import com.professor1416.phittoos.data.model.Friend
import com.professor1416.phittoos.data.model.FriendWithBalance
import com.professor1416.phittoos.data.model.NeedsAttentionItem
import com.professor1416.phittoos.data.model.ReminderStage
import com.professor1416.phittoos.data.model.TransactionDirection
import com.professor1416.phittoos.data.model.TransactionEntity
import com.professor1416.phittoos.data.model.TransactionStatus
import com.professor1416.phittoos.data.model.TransactionWithFriend
import com.professor1416.phittoos.data.model.dueInfo
import com.professor1416.phittoos.data.model.effectivePaidAmount
import com.professor1416.phittoos.data.model.effectiveRemainingAmount
import com.professor1416.phittoos.domain.ReliabilityEngine
import androidx.room.withTransaction
import com.professor1416.phittoos.data.db.AppDatabase
import com.professor1416.phittoos.data.preferences.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

import com.professor1416.phittoos.ui.util.Formatters

enum class RepaymentErrorReason {
    INVALID_AMOUNT,
    TRANSACTION_NOT_FOUND,
    ALREADY_SETTLED,
    EXCEEDS_REMAINING,
    NO_LONGER_OPEN
}

enum class EditErrorReason {
    TRANSACTION_NOT_FOUND,
    NOT_OPEN,
    HAS_REPAYMENTS,
    INVALID_AMOUNT,
    INVALID_DUE_DATE
}

sealed class EditTransactionResult {
    data class Success(val updatedTransaction: TransactionEntity) : EditTransactionResult()
    data class Error(val message: String, val reason: EditErrorReason) : EditTransactionResult()
}

enum class DeleteErrorReason {
    TRANSACTION_NOT_FOUND,
    NOT_OPEN,
    HAS_REPAYMENTS
}

sealed class DeleteTransactionResult {
    object Success : DeleteTransactionResult()
    data class Error(val message: String, val reason: DeleteErrorReason) : DeleteTransactionResult()
}

data class SettlementResult(
    val success: Boolean,
    val isAllSettledForFriend: Boolean
)

sealed class RepaymentResult {
    data class Success(
        val updatedTransaction: TransactionEntity,
        val isFullySettled: Boolean,
        val isAllSettledForFriend: Boolean = false
    ) : RepaymentResult()

    data class Error(
        val message: String,
        val reason: RepaymentErrorReason = RepaymentErrorReason.INVALID_AMOUNT,
        val remainingAmount: Double? = null
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
        val netPosition: Double,
        val openTransactionsCount: Int = 0
    )

    val dashboardTotals: Flow<DashboardTotals> = transactionDao.getAllTransactions().map { transactions ->
        var lentTotal = 0.0
        var borrowedTotal = 0.0
        var openCount = 0

        for (tx in transactions) {
            if (tx.status == TransactionStatus.OPEN) {
                openCount++
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
            netPosition = lentTotal - borrowedTotal,
            openTransactionsCount = openCount
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
        require(trimmed.isNotBlank()) { "Friend name cannot be blank" }
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

    suspend fun updateOpenUnpaidTransaction(
        transactionId: Long,
        amount: Double,
        direction: TransactionDirection,
        note: String? = null,
        dueDate: Long? = null
    ): EditTransactionResult {
        if (amount <= 0.0 || amount.isNaN() || amount.isInfinite()) {
            return EditTransactionResult.Error(
                message = "Enter an amount greater than ₹0.",
                reason = EditErrorReason.INVALID_AMOUNT
            )
        }
        if (dueDate != null && com.professor1416.phittoos.domain.DueDateHelper.isPastDate(dueDate)) {
            return EditTransactionResult.Error(
                message = "Due date cannot be in the past.",
                reason = EditErrorReason.INVALID_DUE_DATE
            )
        }

        return runInTransaction {
            val tx = transactionDao.getTransactionById(transactionId)
                ?: return@runInTransaction EditTransactionResult.Error(
                    message = "This transaction could not be found.",
                    reason = EditErrorReason.TRANSACTION_NOT_FOUND
                )

            if (tx.status != TransactionStatus.OPEN) {
                return@runInTransaction EditTransactionResult.Error(
                    message = "This transaction is no longer pending.",
                    reason = EditErrorReason.NOT_OPEN
                )
            }

            if (tx.effectivePaidAmount > 0.0 || (tx.paidAmount ?: 0.0) > 0.0 || tx.settledAt != null) {
                return@runInTransaction EditTransactionResult.Error(
                    message = "This transaction already has repayment history and cannot be edited.",
                    reason = EditErrorReason.HAS_REPAYMENTS
                )
            }

            val trimmedNote = note?.trim()?.ifBlank { null }
            val updatedTx = tx.copy(
                amount = amount,
                direction = direction,
                note = trimmedNote,
                dueDate = dueDate
            )

            transactionDao.updateTransaction(updatedTx)

            // Update corresponding TRANSACTION_CREATED activity if present to maintain internal consistency
            activityDao?.updateTransactionCreatedActivity(
                transactionId = transactionId,
                amount = amount,
                direction = direction,
                note = trimmedNote
            )

            EditTransactionResult.Success(updatedTx)
        }
    }

    suspend fun deleteOpenUnpaidTransaction(
        transactionId: Long
    ): DeleteTransactionResult {
        return runInTransaction {
            val tx = transactionDao.getTransactionById(transactionId)
                ?: return@runInTransaction DeleteTransactionResult.Error(
                    message = "This transaction could not be found.",
                    reason = DeleteErrorReason.TRANSACTION_NOT_FOUND
                )

            if (tx.status != TransactionStatus.OPEN) {
                return@runInTransaction DeleteTransactionResult.Error(
                    message = "This transaction is no longer pending.",
                    reason = DeleteErrorReason.NOT_OPEN
                )
            }

            if (tx.effectivePaidAmount > 0.0 || (tx.paidAmount ?: 0.0) > 0.0 || tx.settledAt != null) {
                return@runInTransaction DeleteTransactionResult.Error(
                    message = "This transaction already has repayment history and cannot be deleted.",
                    reason = DeleteErrorReason.HAS_REPAYMENTS
                )
            }

            // Remove activities for this transaction to avoid orphaned records
            activityDao?.deleteActivitiesForTransaction(transactionId)

            // Delete transaction from database
            transactionDao.deleteTransaction(tx)

            DeleteTransactionResult.Success
        }
    }

    suspend fun recordRepayment(
        transactionId: Long,
        repaymentAmount: Double,
        settledAt: Long = System.currentTimeMillis()
    ): RepaymentResult {
        if (repaymentAmount <= 0.0 || repaymentAmount.isNaN() || repaymentAmount.isInfinite()) {
            return RepaymentResult.Error(
                message = "Enter an amount greater than ₹0.",
                reason = RepaymentErrorReason.INVALID_AMOUNT
            )
        }

        return runInTransaction {
            val tx = transactionDao.getTransactionById(transactionId)
                ?: return@runInTransaction RepaymentResult.Error(
                    message = "This transaction could not be found.",
                    reason = RepaymentErrorReason.TRANSACTION_NOT_FOUND
                )

            if (tx.status != TransactionStatus.OPEN) {
                return@runInTransaction RepaymentResult.Error(
                    message = "This transaction is already fully paid.",
                    reason = RepaymentErrorReason.ALREADY_SETTLED
                )
            }

            val remaining = tx.effectiveRemainingAmount
            if (repaymentAmount > remaining + 0.0001) {
                val formatted = Formatters.formatCurrency(remaining)
                return@runInTransaction RepaymentResult.Error(
                    message = "Enter $formatted or less. That’s the amount left to pay.",
                    reason = RepaymentErrorReason.EXCEEDS_REMAINING,
                    remainingAmount = remaining
                )
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
                return@runInTransaction RepaymentResult.Error(
                    message = "This transaction is no longer pending.",
                    reason = RepaymentErrorReason.NO_LONGER_OPEN
                )
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
            val isAllSettled = if (isFullySettled) {
                val remainingOpenCount = transactionDao.getOpenCountForFriend(tx.friendId)
                remainingOpenCount == 0
            } else {
                false
            }
            RepaymentResult.Success(
                updatedTransaction = updatedTx,
                isFullySettled = isFullySettled,
                isAllSettledForFriend = isAllSettled
            )
        }
    }

    suspend fun markTransactionAsPaid(
        transactionId: Long,
        settledAt: Long = System.currentTimeMillis()
    ): SettlementResult {
        return runInTransaction {
            val tx = transactionDao.getTransactionById(transactionId)
                ?: return@runInTransaction SettlementResult(success = false, isAllSettledForFriend = false)
            if (tx.status != TransactionStatus.OPEN) {
                return@runInTransaction SettlementResult(success = false, isAllSettledForFriend = false)
            }

            val rowsUpdated = transactionDao.markAsConfirmed(transactionId, settledAt)
            if (rowsUpdated == 0) {
                return@runInTransaction SettlementResult(success = false, isAllSettledForFriend = false)
            }

            val remaining = tx.effectiveRemainingAmount
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

            val remainingOpenCount = transactionDao.getOpenCountForFriend(tx.friendId)
            SettlementResult(
                success = true,
                isAllSettledForFriend = (remainingOpenCount == 0)
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
            val rowsUpdated = transactionDao.markAllOpenForFriendAsConfirmed(friendId, settledAt)
            if (rowsUpdated == 0) return@runInTransaction false

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
            val remainingOpenCount = transactionDao.getOpenCountForFriend(friendId)
            remainingOpenCount == 0
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

    suspend fun allActivitiesList(): List<ActivityEntity> {
        return activityDao?.getAllActivitiesList() ?: emptyList()
    }

    suspend fun restoreValidatedData(
        friends: List<Friend>,
        transactions: List<TransactionEntity>,
        activities: List<ActivityEntity>,
        userName: String,
        hasCompletedOnboarding: Boolean,
        remindersEnabled: Boolean,
        userPreferences: UserPreferences
    ) {
        runInTransaction {
            activityDao?.deleteAllActivities()
            transactionDao.deleteAllTransactions()
            friendDao.deleteAllFriends()

            for (friend in friends) {
                friendDao.insertFriend(friend)
            }
            for (tx in transactions) {
                transactionDao.insertTransaction(tx)
            }
            if (activityDao != null) {
                for (act in activities) {
                    activityDao.insertActivity(act)
                }
            }

            userPreferences.userName = userName
            userPreferences.hasCompletedOnboarding = hasCompletedOnboarding
            userPreferences.remindersEnabled = remindersEnabled
        }
    }
}
