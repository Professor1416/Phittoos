package com.example.reminder

import android.content.Context
import com.example.data.model.ReminderStage
import com.example.data.model.TransactionDirection
import com.example.data.model.TransactionStatus
import com.example.data.model.effectiveRemainingAmount
import com.example.data.repository.PhittoosRepository
import com.example.domain.DueDateHelper
import java.util.TimeZone

data class ReminderProcessResult(
    val transactionId: Long,
    val friendId: Long,
    val friendName: String,
    val stage: ReminderStage,
    val amount: Double,
    val posted: Boolean,
    val recordedInActivity: Boolean
)

data class ReminderExecutionSummary(
    val checkedTransactionsCount: Int,
    val remindersSentCount: Int,
    val details: List<ReminderProcessResult>
)

object SmartReminderEngine {

    suspend fun checkAndSendReminders(
        repository: PhittoosRepository,
        context: Context,
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault(),
        notificationSender: (
            friendName: String,
            remainingAmount: Double,
            stage: ReminderStage,
            transactionId: Long,
            friendId: Long
        ) -> Boolean = { friendName, remainingAmount, stage, transactionId, friendId ->
            ReminderNotificationHelper.sendReminderNotification(
                context = context,
                friendName = friendName,
                amount = remainingAmount,
                stage = stage,
                transactionId = transactionId,
                friendId = friendId
            )
        },
        remindersEnabled: Boolean = true
    ): ReminderExecutionSummary {
        if (!remindersEnabled) {
            return ReminderExecutionSummary(
                checkedTransactionsCount = 0,
                remindersSentCount = 0,
                details = emptyList()
            )
        }

        // 1. Fetch open transactions with due dates
        val openTxs = repository.getOpenTransactionsWithDueDate()

        // 2. Strict qualification criteria:
        // - direction == LENT (BORROWED transactions NEVER trigger reminders)
        // - status == OPEN (CONFIRMED/Settled transactions NEVER trigger reminders)
        // - dueDate != null && dueDate > 0 (Transactions without due dates NEVER trigger reminders)
        // - effectiveRemainingAmount > 0.0 (Zero-balance transactions NEVER trigger reminders)
        val eligibleTxs = openTxs.filter { tx ->
            tx.direction == TransactionDirection.LENT &&
                    tx.status == TransactionStatus.OPEN &&
                    tx.dueDate != null &&
                    tx.dueDate > 0 &&
                    tx.effectiveRemainingAmount > 0.0
        }

        val details = mutableListOf<ReminderProcessResult>()
        var remindersSent = 0

        for (tx in eligibleTxs) {
            val dueDate = tx.dueDate ?: continue
            val dueInfo = DueDateHelper.calculateDueState(
                dueDateMillis = dueDate,
                status = tx.status,
                nowMillis = nowMillis,
                timeZone = timeZone
            )

            // Must be actively overdue and reached at least 7 days threshold
            if (!dueInfo.isActivelyOverdue || dueInfo.overdueDays < ReminderStage.DAY_7.daysOverdueThreshold) {
                continue
            }

            // Fetch already sent stages for this transaction
            val alreadySentStages = repository.getSentReminderStagesForTransaction(tx.id)

            // Determine highest unsent stage due
            val stageToSend = ReminderStage.determineStageToSend(
                overdueDays = dueInfo.overdueDays,
                alreadySentStages = alreadySentStages
            ) ?: continue

            // Look up friend
            val friend = repository.getFriendByIdOnce(tx.friendId)
            val friendName = friend?.name ?: "Friend"

            val remainingAmount = tx.effectiveRemainingAmount

            // Post notification
            val posted = notificationSender(
                friendName,
                remainingAmount,
                stageToSend,
                tx.id,
                tx.friendId
            )

            var recorded = false
            if (posted) {
                // Persist successful reminder history
                val actId = repository.recordReminderSent(
                    transactionId = tx.id,
                    friendId = tx.friendId,
                    stage = stageToSend,
                    amount = remainingAmount,
                    direction = tx.direction,
                    timestamp = nowMillis
                )
                recorded = actId != null
                remindersSent++
            }

            details.add(
                ReminderProcessResult(
                    transactionId = tx.id,
                    friendId = tx.friendId,
                    friendName = friendName,
                    stage = stageToSend,
                    amount = remainingAmount,
                    posted = posted,
                    recordedInActivity = recorded
                )
            )
        }

        return ReminderExecutionSummary(
            checkedTransactionsCount = eligibleTxs.size,
            remindersSentCount = remindersSent,
            details = details
        )
    }
}
