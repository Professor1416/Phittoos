package com.example.data.model

/**
 * Meaningful activity event types for Phittoos.
 * Only TRANSACTION_CREATED, PARTIAL_REPAYMENT, and SETTLED are active in Task #8.
 * BECAME_OVERDUE and REMINDER_SENT are future-compatible definitions.
 */
enum class ActivityType {
    TRANSACTION_CREATED,
    PARTIAL_REPAYMENT,
    SETTLED,
    BECAME_OVERDUE,
    REMINDER_SENT
}
