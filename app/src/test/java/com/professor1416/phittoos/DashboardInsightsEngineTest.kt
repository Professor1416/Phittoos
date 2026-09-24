package com.professor1416.phittoos

import com.professor1416.phittoos.data.model.TransactionDirection
import com.professor1416.phittoos.data.model.TransactionEntity
import com.professor1416.phittoos.data.model.TransactionStatus
import com.professor1416.phittoos.domain.DashboardInsightsEngine
import com.professor1416.phittoos.domain.SpendingCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class DashboardInsightsEngineTest {

    @Test
    fun compute_emptyTransactions_returnsDefaultEmptyInsights() {
        val insights = DashboardInsightsEngine.compute(emptyList())
        assertEquals(0.0, insights.totalLentLast30Days, 0.001)
        assertEquals(0.0, insights.totalBorrowedLast30Days, 0.001)
        assertEquals(0.0, insights.netFlowLast30Days, 0.001)
        assertEquals(0, insights.totalTransactionsCount30Days)
        assertFalse(insights.has30DayActivity)
    }

    @Test
    fun compute_filtersTransactionsOlderThan30Days() {
        val now = System.currentTimeMillis()
        val oldTimestamp = now - TimeUnit.DAYS.toMillis(40)
        val recentTimestamp = now - TimeUnit.DAYS.toMillis(5)

        val txs = listOf(
            TransactionEntity(
                id = 1,
                friendId = 10,
                amount = 5000.0,
                direction = TransactionDirection.LENT,
                createdDate = oldTimestamp,
                status = TransactionStatus.OPEN
            ),
            TransactionEntity(
                id = 2,
                friendId = 10,
                amount = 1200.0,
                direction = TransactionDirection.LENT,
                createdDate = recentTimestamp,
                status = TransactionStatus.OPEN
            ),
            TransactionEntity(
                id = 3,
                friendId = 20,
                amount = 400.0,
                direction = TransactionDirection.BORROWED,
                createdDate = recentTimestamp,
                status = TransactionStatus.OPEN
            )
        )

        val insights = DashboardInsightsEngine.compute(txs, referenceTimestamp = now)

        assertEquals(1200.0, insights.totalLentLast30Days, 0.001)
        assertEquals(400.0, insights.totalBorrowedLast30Days, 0.001)
        assertEquals(800.0, insights.netFlowLast30Days, 0.001)
        assertEquals(2, insights.totalTransactionsCount30Days)
        assertTrue(insights.has30DayActivity)
    }

    @Test
    fun compute_categorizesTransactionsAccurately() {
        val now = System.currentTimeMillis()
        val txs = listOf(
            TransactionEntity(
                id = 1,
                friendId = 1,
                amount = 350.0,
                direction = TransactionDirection.LENT,
                note = "Chai and samosa snacks",
                createdDate = now - TimeUnit.DAYS.toMillis(2)
            ),
            TransactionEntity(
                id = 2,
                friendId = 2,
                amount = 1500.0,
                direction = TransactionDirection.BORROWED,
                note = "Petrol and uber cab",
                createdDate = now - TimeUnit.DAYS.toMillis(3)
            ),
            TransactionEntity(
                id = 3,
                friendId = 3,
                amount = 2500.0,
                direction = TransactionDirection.LENT,
                note = "Wifi broadband bill and rent share",
                createdDate = now - TimeUnit.DAYS.toMillis(4)
            ),
            TransactionEntity(
                id = 4,
                friendId = 4,
                amount = 800.0,
                direction = TransactionDirection.LENT,
                note = "Groceries from blinkit mart",
                createdDate = now - TimeUnit.DAYS.toMillis(5)
            )
        )

        val insights = DashboardInsightsEngine.compute(txs, referenceTimestamp = now)

        assertEquals(4, insights.categoryBreakdowns.size)

        val foodCategory = insights.categoryBreakdowns.find { it.category == SpendingCategory.FOOD }
        assertNotNull(foodCategory)
        assertEquals(350.0, foodCategory!!.totalAmount, 0.001)

        val travelCategory = insights.categoryBreakdowns.find { it.category == SpendingCategory.TRAVEL }
        assertNotNull(travelCategory)
        assertEquals(1500.0, travelCategory!!.totalAmount, 0.001)

        val billsCategory = insights.categoryBreakdowns.find { it.category == SpendingCategory.BILLS }
        assertNotNull(billsCategory)
        assertEquals(2500.0, billsCategory!!.totalAmount, 0.001)

        val shoppingCategory = insights.categoryBreakdowns.find { it.category == SpendingCategory.SHOPPING }
        assertNotNull(shoppingCategory)
        assertEquals(800.0, shoppingCategory!!.totalAmount, 0.001)

        // Top category should be bills (2500)
        assertEquals(SpendingCategory.BILLS, insights.topSpendingCategory?.category)
    }

    @Test
    fun compute_weeklyTrendsAreDividedInto4Buckets() {
        val now = System.currentTimeMillis()
        val txs = listOf(
            TransactionEntity(
                id = 1,
                friendId = 1,
                amount = 500.0,
                direction = TransactionDirection.LENT,
                createdDate = now - TimeUnit.DAYS.toMillis(2) // Week 4 (recent)
            ),
            TransactionEntity(
                id = 2,
                friendId = 2,
                amount = 300.0,
                direction = TransactionDirection.BORROWED,
                createdDate = now - TimeUnit.DAYS.toMillis(28) // Week 1 (oldest in 30d)
            )
        )

        val insights = DashboardInsightsEngine.compute(txs, referenceTimestamp = now)
        assertEquals(4, insights.weeklyTrends.size)
    }
}
