package com.example.data.repository

import com.example.data.dao.FriendDao
import com.example.data.dao.TransactionDao
import com.example.data.model.Friend
import com.example.data.model.FriendWithBalance
import com.example.data.model.TransactionDirection
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionStatus
import com.example.data.model.TransactionWithFriend
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

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
                    val effectiveAmount = tx.amount - (tx.paidAmount ?: 0.0)
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
                val effectiveAmount = tx.amount - (tx.paidAmount ?: 0.0)
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
                val effectiveAmount = tx.amount - (tx.paidAmount ?: 0.0)
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
        note: String?,
        dueDate: Long?
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

    suspend fun markTransactionAsPaid(transactionId: Long) {
        transactionDao.markAsConfirmed(transactionId)
    }

    suspend fun markAllForFriendAsPaid(friendId: Long) {
        transactionDao.markAllOpenForFriendAsConfirmed(friendId)
    }

    suspend fun getRecentFriends(limit: Int = 6): List<Friend> {
        // Find distinct friend IDs from recent transactions
        // Fallback to all friends
        val all = friendDao.getFriendByName("") // just a check, or query
        return emptyList()
    }
}
