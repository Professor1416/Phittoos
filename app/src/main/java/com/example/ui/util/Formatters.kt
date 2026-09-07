package com.example.ui.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object Formatters {
    fun formatCurrency(amount: Double): String {
        return if (amount % 1.0 == 0.0) {
            "₹%,d".format(Locale.getDefault(), amount.toLong())
        } else {
            "₹%,.2f".format(Locale.getDefault(), amount)
        }
    }

    fun formatDate(
        timestamp: Long?,
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        if (timestamp == null || timestamp <= 0) return "No date"

        val now = Calendar.getInstance(timeZone).apply { timeInMillis = nowMillis }
        val target = Calendar.getInstance(timeZone).apply { timeInMillis = timestamp }

        val isSameDay = now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)

        if (isSameDay) return "Today"

        val yesterday = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowMillis
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val isYesterday = yesterday.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                yesterday.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)

        if (isYesterday) return "Yesterday"

        val sdf = if (now.get(Calendar.YEAR) == target.get(Calendar.YEAR)) {
            SimpleDateFormat("d MMM", Locale.getDefault()).apply { this.timeZone = timeZone }
        } else {
            SimpleDateFormat("d MMM yyyy", Locale.getDefault()).apply { this.timeZone = timeZone }
        }
        return sdf.format(Date(timestamp))
    }

    fun formatTime(
        timestamp: Long?,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        if (timestamp == null || timestamp <= 0) return ""
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault()).apply {
            this.timeZone = timeZone
        }
        return sdf.format(Date(timestamp))
    }

    fun formatFullDate(timestamp: Long?): String {
        if (timestamp == null || timestamp <= 0) return "No date"
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
