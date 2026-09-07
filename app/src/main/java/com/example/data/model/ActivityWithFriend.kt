package com.example.data.model

data class ActivityWithFriend(
    val activity: ActivityEntity,
    val friendName: String
)

data class NeedsAttentionItem(
    val transactionId: Long,
    val friendId: Long,
    val friendName: String,
    val direction: TransactionDirection,
    val remainingAmount: Double,
    val overdueDays: Int,
    val dueDate: Long,
    val formattedStatus: String
)
