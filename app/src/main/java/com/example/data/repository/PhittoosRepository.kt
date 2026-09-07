package com.example.data.repository

import com.example.data.dao.FriendDao
import com.example.data.dao.TransactionDao
import com.example.data.model.Friend
import com.example.data.model.FriendWithBalance
import com.example.data.model.TransactionDirection
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionStatus
import com.example.data.model.TransactionWithFriend
import com.example.data.model.effectiveRemainingAmount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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
    private val transactionDao: TransactionDao
) {
    val allFriends: Flow<List<Friend>> = friendDao.getAllFriends()
    val recentFriends: Flow<List<Friend>> = friendDao.getRecentFriendsWithTransactions(8)
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    val friendsWithBalance: Flow<List<FriendWithBalance>> = combine(
        friendDao.getAllFriends(),
        transactionDao.getAllTransactions()
    ) { friends, transactions ->
        val txByFriend = transactions.groupBy { it.friendId }

        friends.map { friend ->
            val friendTxs = txByFriend[friend.id] ?: emptyList()
            var net = 0.0
            var openCount = 0

            for (tx in friendTxs) {
                if (tx.status == TransactionStatus.OPEN) {
                    openCount++
                    val effectiveAmount = tx.effectiveRemainingAmount
                    if (tx.direction == TransactionDirection.LENT) {
                        net += effectiveAmount
                    } else {
                        net -= effectiveAmount
                    }
                }
            }

            val lastActivity = friendTxs.maxOfOrNull { it.createdDate }

            FriendWithBalance(
                friend = friend,
                netBalance = net,
                lastActivityDate = lastActivity,
                openTransactionsCount = openCount,
                totalTransactionsCount = friendTxs.size
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

        for (tx in friendTxs) {
            if (tx.status == TransactionStatus.OPEN) {
                openCount++
                val effectiveAmount = tx.effectiveRemainingAmount
                if (tx.direction == TransactionDirection.LENT) {
                    net += effectiveAmount
                } else {
                    net -= effectiveAmount
                }
            }
        }

        val lastActivity = friendTxs.maxOfOrNull { it.createdDate }

        FriendWithBalance(
            friend = friend,
            netBalance = net,
            lastActivityDate = lastActivity,
            openTransactionsCount = openCount,
            totalTransactionsCount = friendTxs.size
        )
    }

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
        return transactionDao.insertTransaction(
            TransactionEntity(
                friendId = friendId,
                amount = amount,
                direction = direction,
                note = note?.trim()?.ifBlank { null },
                dueDate = dueDate,
                status = TransactionStatus.OPEN
            )
        )
    }

    suspend fun getTransactionById(transactionId: Long): TransactionEntity? {
        return transactionDao.getTransactionById(transactionId)
    }

    suspend fun recordRepayment(transactionId: Long, repaymentAmount: Double): RepaymentResult {
        if (repaymentAmount <= 0.0 || repaymentAmount.isNaN() || repaymentAmount.isInfinite()) {
            return RepaymentResult.Error("Repayment amount must be greater than ₹0")
        }

        val tx = transactionDao.getTransactionById(transactionId)
            ?: return RepaymentResult.Error("Transaction not found")

        if (tx.status != TransactionStatus.OPEN) {
            return RepaymentResult.Error("Transaction is already settled")
        }

        val remaining = tx.effectiveRemainingAmount
        if (repaymentAmount > remaining + 0.0001) {
            return RepaymentResult.Error("Amount exceeds remaining balance of $remaining")
        }

        val currentEffectivePaid = (tx.paidAmount ?: 0.0).coerceIn(0.0, tx.amount)
        val unroundedPaid = currentEffectivePaid + repaymentAmount
        val newPaid = kotlin.math.round(unroundedPaid * 100.0) / 100.0
        val isFullySettled = newPaid >= tx.amount - 0.005

        val finalPaid = if (isFullySettled) tx.amount else newPaid.coerceAtMost(tx.amount)
        val finalStatus = if (isFullySettled) TransactionStatus.CONFIRMED else TransactionStatus.OPEN

        val rowsUpdated = transactionDao.updateRepayment(transactionId, finalPaid, finalStatus)
        if (rowsUpdated == 0) {
            return RepaymentResult.Error("Transaction is no longer open")
        }

        val updatedTx = tx.copy(paidAmount = finalPaid, status = finalStatus)
        return RepaymentResult.Success(updatedTx, isFullySettled)
    }

    suspend fun markTransactionAsPaid(transactionId: Long) {
        transactionDao.markAsConfirmed(transactionId)
    }

    suspend fun settleAllSameDirectionForFriend(friendId: Long): Boolean {
        val openTxs = transactionDao.getOpenTransactionsForFriend(friendId)
        if (openTxs.isEmpty()) return false
        val hasLent = openTxs.any { it.direction == TransactionDirection.LENT }
        val hasBorrowed = openTxs.any { it.direction == TransactionDirection.BORROWED }
        if (hasLent && hasBorrowed) {
            // Mixed direction safety guard: do NOT bulk-settle when transactions are in both directions
            return false
        }
        transactionDao.markAllOpenForFriendAsConfirmed(friendId)
        return true
    }

    suspend fun markAllForFriendAsPaid(friendId: Long): Boolean {
        return settleAllSameDirectionForFriend(friendId)
    }

    suspend fun getRecentFriends(limit: Int = 6): List<Friend> {
        // Find distinct friend IDs from recent transactions
        // Fallback to all friends
        val all = friendDao.getFriendByName("") // just a check, or query
        return emptyList()
    }
}
