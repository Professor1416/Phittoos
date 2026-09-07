package com.example.domain

import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionStatus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Logical due date states for transactions.
 * Informational only; does not affect financial accounting.
 */
enum class DueState {
    NONE,
    UPCOMING,
    DUE_TODAY,
    OVERDUE
}

data class DueDateInfo(
    val state: DueState,
    val overdueDays: Int = 0,
    val formattedStatus: String = "",
    val isActivelyOverdue: Boolean = false
)

object DueDateHelper {

    /**
     * Calculates the due date status for a transaction in calendar days.
     * Settled (CONFIRMED) transactions are never actively overdue.
     */
    fun calculateDueState(
        dueDateMillis: Long?,
        status: TransactionStatus,
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): DueDateInfo {
        if (dueDateMillis == null || dueDateMillis <= 0) {
            return DueDateInfo(
                state = DueState.NONE,
                overdueDays = 0,
                formattedStatus = "",
                isActivelyOverdue = false
            )
        }

        // Settled/CONFIRMED transactions are never actively overdue
        if (status == TransactionStatus.CONFIRMED) {
            val formattedDate = formatDueDate(dueDateMillis, nowMillis, timeZone)
            return DueDateInfo(
                state = DueState.NONE,
                overdueDays = 0,
                formattedStatus = "Due $formattedDate",
                isActivelyOverdue = false
            )
        }

        // Normalize both timestamps to noon (12:00:00) in the given timezone to ensure
        // pure calendar-day comparison immune to clock-time and daylight-saving shifts.
        val dueCal = Calendar.getInstance(timeZone).apply {
            timeInMillis = dueDateMillis
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val nowCal = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val isSameDay = dueCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                dueCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)

        if (isSameDay) {
            return DueDateInfo(
                state = DueState.DUE_TODAY,
                overdueDays = 0,
                formattedStatus = "Due today",
                isActivelyOverdue = false
            )
        }

        if (nowCal.after(dueCal)) {
            val diffMillis = nowCal.timeInMillis - dueCal.timeInMillis
            val days = Math.round(diffMillis.toDouble() / 86_400_000.0).toInt().coerceAtLeast(1)
            val dayText = if (days == 1) "1 day" else "$days days"
            return DueDateInfo(
                state = DueState.OVERDUE,
                overdueDays = days,
                formattedStatus = "Overdue by $dayText",
                isActivelyOverdue = true
            )
        } else {
            val formattedDate = formatDueDate(dueDateMillis, nowMillis, timeZone)
            return DueDateInfo(
                state = DueState.UPCOMING,
                overdueDays = 0,
                formattedStatus = "Due $formattedDate",
                isActivelyOverdue = false
            )
        }
    }

    /**
     * Checks if a calendar date is strictly in the past relative to nowMillis in timeZone.
     * Today is NOT in the past (it is allowed for a new transaction).
     */
    fun isPastDate(
        dateMillis: Long,
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): Boolean {
        val dateCal = Calendar.getInstance(timeZone).apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nowCal = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val isSameDay = dateCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                dateCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)

        return dateCal.before(nowCal) && !isSameDay
    }

    /**
     * Calculates delay in calendar days between due date and settlement date.
     * Early repayment (settled before or on due date) returns 0.
     * Negative delay is never returned.
     */
    fun calculateDelayDays(
        dueDateMillis: Long,
        settledAtMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Int {
        val dueCal = Calendar.getInstance(timeZone).apply {
            timeInMillis = dueDateMillis
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val settledCal = Calendar.getInstance(timeZone).apply {
            timeInMillis = settledAtMillis
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (!settledCal.after(dueCal)) {
            return 0
        }

        val diffMillis = settledCal.timeInMillis - dueCal.timeInMillis
        return Math.round(diffMillis.toDouble() / 86_400_000.0).toInt().coerceAtLeast(1)
    }

    /**
     * Formats a due date into a concise string, e.g. "15 Sep" or "15 Sep 2027".
     */
    fun formatDueDate(
        timestamp: Long,
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val nowCal = Calendar.getInstance(timeZone).apply { timeInMillis = nowMillis }
        val targetCal = Calendar.getInstance(timeZone).apply { timeInMillis = timestamp }
        val pattern = if (nowCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR)) {
            "d MMM"
        } else {
            "d MMM yyyy"
        }
        val sdf = SimpleDateFormat(pattern, Locale.getDefault()).apply {
            this.timeZone = timeZone
        }
        return sdf.format(Date(timestamp))
    }
}
