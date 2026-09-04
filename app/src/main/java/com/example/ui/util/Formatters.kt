package com.example.ui.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Formatters {
    fun formatCurrency(amount: Double): String {
        return if (amount % 1.0 == 0.0) {
            "₹%,d".format(Locale.getDefault(), amount.toLong())
        } else {
            "₹%,.2f".format(Locale.getDefault(), amount)
        }
    }

    fun formatDate(timestamp: Long?): String {
        if (timestamp == null || timestamp <= 0) return "No date"

        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = timestamp }

        val isSameDay = now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)

        if (isSameDay) return "Today"

        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val isYesterday = yesterday.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                yesterday.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)

        if (isYesterday) return "Yesterday"

        val sdf = if (now.get(Calendar.YEAR) == target.get(Calendar.YEAR)) {
            SimpleDateFormat("dd MMM", Locale.getDefault())
        } else {
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        }
        return sdf.format(Date(timestamp))
    }

    fun formatFullDate(timestamp: Long?): String {
        if (timestamp == null || timestamp <= 0) return "No date"
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
