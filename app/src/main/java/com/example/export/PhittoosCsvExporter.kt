package com.example.export

import com.example.data.model.Friend
import com.example.data.model.TransactionEntity
import com.example.data.model.effectivePaidAmount
import com.example.data.model.effectiveRemainingAmount
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object PhittoosCsvExporter {

    const val CSV_HEADER = "transaction_id,friend_name,direction,amount,paid_amount,remaining_amount,status,created_date,due_date,settled_date,note"

    fun generateDefaultFileName(now: Long = System.currentTimeMillis(), timeZone: TimeZone = TimeZone.getDefault()): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            this.timeZone = timeZone
        }
        return "phittoos-export-${sdf.format(Date(now))}.csv"
    }

    fun escapeCsvField(value: String?): String {
        if (value == null) return ""
        val needsQuotes = value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')
        return if (needsQuotes) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    fun formatAmount(amount: Double): String {
        return if (amount % 1.0 == 0.0) {
            amount.toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", amount)
        }
    }

    fun formatTimestamp(
        timestamp: Long?,
        includeTime: Boolean = false,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        if (timestamp == null || timestamp <= 0) return ""
        val pattern = if (includeTime) "yyyy-MM-dd HH:mm" else "yyyy-MM-dd"
        val sdf = SimpleDateFormat(pattern, Locale.US).apply {
            this.timeZone = timeZone
        }
        return sdf.format(Date(timestamp))
    }

    /**
     * Builds the complete CSV string for given transactions and friends list.
     * Orders deterministically by created_date ASC, id ASC.
     */
    fun buildCsv(
        transactions: List<TransactionEntity>,
        friends: List<Friend>,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val friendMap = friends.associateBy { it.id }
        val sb = StringBuilder()
        sb.append(CSV_HEADER).append("\n")

        val sortedTransactions = transactions.sortedWith(
            compareBy<TransactionEntity> { it.createdDate }.thenBy { it.id }
        )

        for (tx in sortedTransactions) {
            val friendName = friendMap[tx.friendId]?.name ?: "Unknown"
            val direction = tx.direction.name
            val amountStr = formatAmount(tx.amount)
            val paidAmountStr = formatAmount(tx.effectivePaidAmount)
            val remainingAmountStr = formatAmount(tx.effectiveRemainingAmount)
            val status = tx.status.name
            val createdDateStr = formatTimestamp(tx.createdDate, includeTime = true, timeZone = timeZone)
            val dueDateStr = formatTimestamp(tx.dueDate, includeTime = false, timeZone = timeZone)
            val settledDateStr = formatTimestamp(tx.settledAt, includeTime = true, timeZone = timeZone)
            val noteStr = tx.note ?: ""

            val row = listOf(
                tx.id.toString(),
                escapeCsvField(friendName),
                direction,
                amountStr,
                paidAmountStr,
                remainingAmountStr,
                status,
                createdDateStr,
                dueDateStr,
                settledDateStr,
                escapeCsvField(noteStr)
            ).joinToString(",")

            sb.append(row).append("\n")
        }

        return sb.toString()
    }
}
