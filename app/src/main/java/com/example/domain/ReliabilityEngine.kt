package com.example.domain

import com.example.data.model.TransactionDirection
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionStatus
import com.example.data.model.dueInfo

/**
 * Categorical level of private friend reliability.
 * Derived from historical lent repayment behavior and current open status.
 */
enum class ReliabilityLevel {
    NEW,
    GREEN,
    YELLOW,
    RED
}

/**
 * UI and domain model representing private friend reliability.
 * Kept private to device owner, never shared with the friend.
 */
data class ReliabilityInfo(
    val level: ReliabilityLevel,
    val completedCount: Int,
    val overdueOpenCount: Int,
    val averageDelayDays: Double?,
    val summaryText: String,
    val detailText: String = "Only you can see this"
) {
    companion object {
        val NEW = ReliabilityInfo(
            level = ReliabilityLevel.NEW,
            completedCount = 0,
            overdueOpenCount = 0,
            averageDelayDays = null,
            summaryText = "Not enough repayment history yet."
        )
    }
}

/**
 * Pure, deterministic engine for calculating private friend reliability.
 *
 * Rules:
 * 1. Only LENT transactions measure how reliably the friend pays back.
 *    BORROWED transactions represent user's repayment behavior and MUST NEVER penalize the friend.
 * 2. Requires at least 2 completed (CONFIRMED) LENT transactions before assigning GREEN/YELLOW/RED.
 * 3. Delays are calculated only for completed LENT transactions having both a due date and valid settlement timestamp.
 * 4. Open actively overdue LENT transactions influence the score.
 * 5. Missing data is handled conservatively without inventing fake timestamps or fake 0-day proofs.
 * 6. Language is non-judgmental, objective, and privacy-oriented.
 */
object ReliabilityEngine {
    const val MIN_COMPLETED_LENT_FOR_RELIABILITY = 2
    const val GREEN_MAX_AVERAGE_DELAY_DAYS = 3.0
    const val YELLOW_MAX_AVERAGE_DELAY_DAYS = 10.0
    const val OVERDUE_RATIO_HIGH_THRESHOLD = 0.50

    fun calculate(
        transactions: List<TransactionEntity>,
        nowMillis: Long = System.currentTimeMillis()
    ): ReliabilityInfo {
        // Only LENT transactions measure the friend's reliability
        val lentTxs = transactions.filter { it.direction == TransactionDirection.LENT }

        val completedLent = lentTxs.filter { it.status == TransactionStatus.CONFIRMED }
        val completedCount = completedLent.size

        val openLent = lentTxs.filter { it.status == TransactionStatus.OPEN }
        val openCount = openLent.size
        val overdueOpenCount = openLent.count {
            DueDateHelper.calculateDueState(it.dueDate, it.status, nowMillis = nowMillis).isActivelyOverdue
        }
        val overdueOpenRatio = if (openCount > 0) overdueOpenCount.toDouble() / openCount else 0.0

        // 1. MINIMUM HISTORY: At least 2 completed LENT transactions required
        if (completedCount < MIN_COMPLETED_LENT_FOR_RELIABILITY) {
            return ReliabilityInfo(
                level = ReliabilityLevel.NEW,
                completedCount = completedCount,
                overdueOpenCount = overdueOpenCount,
                averageDelayDays = null,
                summaryText = "Not enough repayment history yet."
            )
        }

        // 2. DELAY CALCULATION: Only for completed transactions with BOTH due date and valid settlement timestamp
        val settledWithDelayInfo = completedLent.filter { it.dueDate != null && it.settledAt != null }
        val delays = settledWithDelayInfo.map { tx ->
            DueDateHelper.calculateDelayDays(tx.dueDate!!, tx.settledAt!!)
        }
        val averageDelayDays = if (delays.isNotEmpty()) delays.average() else null
        val hasDelayData = delays.isNotEmpty()

        // 3. CLASSIFICATION:
        // RED criteria:
        // Clear, repeated/history-based problem.
        // Rule: Avoid overreacting to tiny samples (e.g. exactly 2 completed transactions with 1 anomaly is NOT RED).
        val isMateriallyLateHistory = hasDelayData && averageDelayDays != null &&
                averageDelayDays > YELLOW_MAX_AVERAGE_DELAY_DAYS &&
                (if (completedCount == 2) delays.size == 2 && delays.all { it > 7 } else true)

        val isRepeatedOverduePattern = (overdueOpenCount >= 2 && overdueOpenRatio > OVERDUE_RATIO_HIGH_THRESHOLD) ||
                overdueOpenCount >= 3

        val isRed = isMateriallyLateHistory || isRepeatedOverduePattern

        if (isRed) {
            val summary = when {
                overdueOpenCount >= 2 -> "Multiple repayments are currently overdue."
                else -> "Repayments are frequently delayed."
            }
            return ReliabilityInfo(
                level = ReliabilityLevel.RED,
                completedCount = completedCount,
                overdueOpenCount = overdueOpenCount,
                averageDelayDays = averageDelayDays,
                summaryText = summary
            )
        }

        // YELLOW criteria:
        // Some repayments delayed OR currently has overdue balance
        val isYellow = overdueOpenCount > 0 ||
                (hasDelayData && averageDelayDays != null && averageDelayDays > GREEN_MAX_AVERAGE_DELAY_DAYS)

        if (isYellow) {
            val summary = when {
                overdueOpenCount > 0 -> "Currently has an overdue balance."
                else -> "Some repayments have been delayed."
            }
            return ReliabilityInfo(
                level = ReliabilityLevel.YELLOW,
                completedCount = completedCount,
                overdueOpenCount = overdueOpenCount,
                averageDelayDays = averageDelayDays,
                summaryText = summary
            )
        }

        // GREEN criteria:
        // Minimum history met, no overdue balance, and average delay <= 3 days (or no recorded delays)
        val summary = if (hasDelayData) {
            "Usually settles on time."
        } else {
            "Settled on time with no delays recorded."
        }

        return ReliabilityInfo(
            level = ReliabilityLevel.GREEN,
            completedCount = completedCount,
            overdueOpenCount = overdueOpenCount,
            averageDelayDays = averageDelayDays,
            summaryText = summary
        )
    }
}
