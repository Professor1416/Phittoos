package com.professor1416.phittoos.domain

import com.professor1416.phittoos.data.model.TransactionDirection
import com.professor1416.phittoos.data.model.TransactionEntity
import com.professor1416.phittoos.data.model.TransactionStatus
import java.util.concurrent.TimeUnit

data class WeeklySpendingTrend(
    val weekIndex: Int,
    val weekLabel: String,
    val dateRangeLabel: String,
    val lentAmount: Double,
    val borrowedAmount: Double,
    val netDifference: Double,
    val transactionCount: Int
)

enum class SpendingCategory(
    val title: String,
    val defaultKeywords: List<String>
) {
    FOOD(
        "Food & Dining",
        listOf("chai", "tea", "coffee", "dinner", "lunch", "breakfast", "snacks", "biryani", "pizza", "zomato", "swiggy", "cafe", "food", "drinks", "burger", "party", "restaurant", "hotel", "ice cream", "dosa", "momo")
    ),
    TRAVEL(
        "Travel & Commute",
        listOf("petrol", "fuel", "diesel", "uber", "ola", "auto", "cab", "train", "flight", "bus", "toll", "trip", "metro", "rapido", "ticket", "travel")
    ),
    BILLS(
        "Bills & Utilities",
        listOf("rent", "bill", "wifi", "electricity", "water", "recharge", "subscription", "broadband", "maid", "maintenance", "gas", "dth", "emi")
    ),
    SHOPPING(
        "Shopping & Groceries",
        listOf("groceries", "mart", "supermarket", "veggies", "shopping", "amazon", "flipkart", "clothes", "shoes", "gift", "blinkit", "zepto", "instamart")
    ),
    HEALTH(
        "Medical & Health",
        listOf("medicine", "doctor", "hospital", "clinic", "pharmacy", "emergency", "med", "tablet", "health", "test", "dentist")
    ),
    GENERAL(
        "General & Others",
        emptyList()
    )
}

data class SpendingCategoryBreakdown(
    val category: SpendingCategory,
    val title: String,
    val totalAmount: Double,
    val lentAmount: Double,
    val borrowedAmount: Double,
    val percentage: Float, // 0.0f - 100.0f
    val transactionCount: Int
)

data class DashboardInsights(
    val totalLentLast30Days: Double = 0.0,
    val totalBorrowedLast30Days: Double = 0.0,
    val netFlowLast30Days: Double = 0.0,
    val totalTransactionsCount30Days: Int = 0,
    val weeklyTrends: List<WeeklySpendingTrend> = emptyList(),
    val categoryBreakdowns: List<SpendingCategoryBreakdown> = emptyList(),
    val topSpendingCategory: SpendingCategoryBreakdown? = null,
    val lentRatio: Float = 0.5f, // 0.0 to 1.0
    val settlementRatePercentage: Int = 0,
    val has30DayActivity: Boolean = false
)

object DashboardInsightsEngine {

    fun compute(
        transactions: List<TransactionEntity>,
        referenceTimestamp: Long = System.currentTimeMillis()
    ): DashboardInsights {
        if (transactions.isEmpty()) {
            return DashboardInsights()
        }

        val thirtyDaysAgo = referenceTimestamp - TimeUnit.DAYS.toMillis(30)
        val recentTxs = transactions.filter { it.createdDate >= thirtyDaysAgo }

        var lentSum30d = 0.0
        var borrowedSum30d = 0.0
        var settledAmount30d = 0.0
        var totalVolume30d = 0.0

        for (tx in recentTxs) {
            totalVolume30d += tx.amount
            if (tx.direction == TransactionDirection.LENT) {
                lentSum30d += tx.amount
            } else {
                borrowedSum30d += tx.amount
            }
            if (tx.status == TransactionStatus.CONFIRMED || (tx.paidAmount ?: 0.0) >= tx.amount) {
                settledAmount30d += tx.amount
            } else if ((tx.paidAmount ?: 0.0) > 0.0) {
                settledAmount30d += (tx.paidAmount ?: 0.0).coerceAtMost(tx.amount)
            }
        }

        val netFlow = lentSum30d - borrowedSum30d
        val totalLentPlusBorrowed = lentSum30d + borrowedSum30d
        val lentRatio = if (totalLentPlusBorrowed > 0.0) {
            (lentSum30d / totalLentPlusBorrowed).toFloat()
        } else {
            0.5f
        }

        val settlementRate = if (totalVolume30d > 0.0) {
            ((settledAmount30d / totalVolume30d) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }

        // Compute 4 Weekly Buckets:
        val weeklyTrends = mutableListOf<WeeklySpendingTrend>()
        for (w in 0..3) {
            val weekStartDays = 30 - ((w + 1) * 7).coerceAtMost(30)
            val weekEndDays = 30 - (w * 7)

            val weekStartMs = referenceTimestamp - TimeUnit.DAYS.toMillis(weekEndDays.toLong())
            val weekEndMs = referenceTimestamp - TimeUnit.DAYS.toMillis(weekStartDays.toLong())

            val weekTxs = recentTxs.filter { it.createdDate in weekStartMs..weekEndMs }
            val wLent = weekTxs.filter { it.direction == TransactionDirection.LENT }.sumOf { it.amount }
            val wBorrowed = weekTxs.filter { it.direction == TransactionDirection.BORROWED }.sumOf { it.amount }

            val label = when (w) {
                0 -> "Week 1"
                1 -> "Week 2"
                2 -> "Week 3"
                else -> "Week 4"
            }

            weeklyTrends.add(
                WeeklySpendingTrend(
                    weekIndex = w + 1,
                    weekLabel = label,
                    dateRangeLabel = if (w == 3) "This week" else "${4 - w}w ago",
                    lentAmount = wLent,
                    borrowedAmount = wBorrowed,
                    netDifference = wLent - wBorrowed,
                    transactionCount = weekTxs.size
                )
            )
        }

        // Categorize Spending Breakdown
        val categoryAmounts = mutableMapOf<SpendingCategory, Double>()
        val categoryLent = mutableMapOf<SpendingCategory, Double>()
        val categoryBorrowed = mutableMapOf<SpendingCategory, Double>()
        val categoryCounts = mutableMapOf<SpendingCategory, Int>()

        for (tx in recentTxs) {
            val cat = categorize(tx.note)
            categoryAmounts[cat] = (categoryAmounts[cat] ?: 0.0) + tx.amount
            if (tx.direction == TransactionDirection.LENT) {
                categoryLent[cat] = (categoryLent[cat] ?: 0.0) + tx.amount
            } else {
                categoryBorrowed[cat] = (categoryBorrowed[cat] ?: 0.0) + tx.amount
            }
            categoryCounts[cat] = (categoryCounts[cat] ?: 0) + 1
        }

        val totalCategorizedVolume = categoryAmounts.values.sum()
        val breakdowns = SpendingCategory.entries.mapNotNull { cat ->
            val amt = categoryAmounts[cat] ?: 0.0
            if (amt > 0.0) {
                val percentage = if (totalCategorizedVolume > 0.0) {
                    ((amt / totalCategorizedVolume) * 100).toFloat()
                } else 0f
                SpendingCategoryBreakdown(
                    category = cat,
                    title = cat.title,
                    totalAmount = amt,
                    lentAmount = categoryLent[cat] ?: 0.0,
                    borrowedAmount = categoryBorrowed[cat] ?: 0.0,
                    percentage = percentage,
                    transactionCount = categoryCounts[cat] ?: 0
                )
            } else null
        }.sortedByDescending { it.totalAmount }

        return DashboardInsights(
            totalLentLast30Days = lentSum30d,
            totalBorrowedLast30Days = borrowedSum30d,
            netFlowLast30Days = netFlow,
            totalTransactionsCount30Days = recentTxs.size,
            weeklyTrends = weeklyTrends,
            categoryBreakdowns = breakdowns,
            topSpendingCategory = breakdowns.firstOrNull(),
            lentRatio = lentRatio,
            settlementRatePercentage = settlementRate,
            has30DayActivity = recentTxs.isNotEmpty()
        )
    }

    private fun categorize(note: String?): SpendingCategory {
        if (note.isNullOrBlank()) return SpendingCategory.GENERAL
        val lower = note.lowercase()
        for (cat in SpendingCategory.entries) {
            if (cat == SpendingCategory.GENERAL) continue
            for (keyword in cat.defaultKeywords) {
                if (lower.contains(keyword)) {
                    return cat
                }
            }
        }
        return SpendingCategory.GENERAL
    }
}
