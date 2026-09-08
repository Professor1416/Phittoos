package com.example.data.model

/**
 * Product-defined escalation schedule for repayment reminders.
 * Values indicate calendar days overdue relative to the transaction due date.
 */
enum class ReminderStage(val daysOverdueThreshold: Int) {
    DAY_7(7),
    DAY_15(15),
    DAY_30(30);

    fun formatTitle(friendName: String): String {
        return when (this) {
            DAY_7 -> "Payment reminder · $friendName"
            DAY_15 -> "Payment follow-up · $friendName"
            DAY_30 -> "Payment still pending · $friendName"
        }
    }

    fun formatBody(amountFormatted: String): String {
        return when (this) {
            DAY_7 -> "$amountFormatted is still pending. It’s been 7 days since the due date."
            DAY_15 -> "$amountFormatted is still pending after 15 days."
            DAY_30 -> "$amountFormatted remains unsettled after 30 days."
        }
    }

    companion object {
        val ALL_STAGES = listOf(DAY_7, DAY_15, DAY_30)

        /**
         * Determines the highest currently-due unsent stage.
         * If the transaction is overdue, we consider all stages whose threshold is <= overdueDays.
         * To prevent duplicate notifications and spam after app downtime:
         * 1. Already sent stages cannot be re-sent.
         * 2. Any stage strictly lower than a stage that was already sent is skipped.
         * 3. Among the remaining eligible stages, we select the highest one.
         */
        fun determineStageToSend(
            overdueDays: Int,
            alreadySentStages: Set<ReminderStage>
        ): ReminderStage? {
            if (overdueDays < DAY_7.daysOverdueThreshold) {
                return null
            }

            val maxSentThreshold = alreadySentStages.maxOfOrNull { it.daysOverdueThreshold } ?: 0

            // Candidates must:
            // 1. Have their threshold reached: threshold <= overdueDays
            // 2. Not have been sent already
            // 3. Be strictly higher than any stage previously sent
            val candidates = ALL_STAGES.filter { stage ->
                stage.daysOverdueThreshold <= overdueDays &&
                        stage !in alreadySentStages &&
                        stage.daysOverdueThreshold > maxSentThreshold
            }

            // Return the highest currently-due candidate
            return candidates.maxByOrNull { it.daysOverdueThreshold }
        }
    }
}
