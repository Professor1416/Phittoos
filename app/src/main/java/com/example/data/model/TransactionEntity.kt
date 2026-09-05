package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Friend::class,
            parentColumns = ["id"],
            childColumns = ["friend_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["friend_id"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "friend_id")
    val friendId: Long,
    val amount: Double,
    val direction: TransactionDirection,
    val note: String? = null,
    @ColumnInfo(name = "created_date")
    val createdDate: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "due_date")
    val dueDate: Long? = null,
    val status: TransactionStatus = TransactionStatus.OPEN,
    @ColumnInfo(name = "paid_amount")
    val paidAmount: Double? = null
)

val TransactionEntity.effectiveRemainingAmount: Double
    get() {
        val validPaid = (paidAmount ?: 0.0).coerceIn(0.0, amount)
        return (amount - validPaid).coerceAtLeast(0.0)
    }
