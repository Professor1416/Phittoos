package com.example.data.model

import com.example.domain.ReliabilityInfo

data class FriendWithBalance(
    val friend: Friend,
    val netBalance: Double, // > 0: friend owes user, < 0: user owes friend, == 0: settled
    val lastActivityDate: Long?,
    val openTransactionsCount: Int,
    val totalTransactionsCount: Int,
    val overdueTransactionsCount: Int = 0,
    val reliabilityInfo: ReliabilityInfo = ReliabilityInfo.NEW
)
