package com.professor1416.phittoos

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.professor1416.phittoos.domain.DashboardInsights
import com.professor1416.phittoos.domain.SpendingCategory
import com.professor1416.phittoos.domain.SpendingCategoryBreakdown
import com.professor1416.phittoos.domain.WeeklySpendingTrend
import com.professor1416.phittoos.ui.components.DashboardInsightsCard
import com.professor1416.phittoos.ui.theme.PhittoosTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w400dp-h1000dp-xhdpi")
class DashboardInsightsVisualTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun dashboardInsightsCard_rendersMetricsAndCategories() {
        val testInsights = DashboardInsights(
            totalLentLast30Days = 3500.0,
            totalBorrowedLast30Days = 1500.0,
            netFlowLast30Days = 2000.0,
            totalTransactionsCount30Days = 6,
            weeklyTrends = listOf(
                WeeklySpendingTrend(1, "Week 1", "3w ago", 500.0, 200.0, 300.0, 1),
                WeeklySpendingTrend(2, "Week 2", "2w ago", 1000.0, 400.0, 600.0, 2),
                WeeklySpendingTrend(3, "Week 3", "1w ago", 800.0, 600.0, 200.0, 2),
                WeeklySpendingTrend(4, "Week 4", "This week", 1200.0, 300.0, 900.0, 1)
            ),
            categoryBreakdowns = listOf(
                SpendingCategoryBreakdown(
                    category = SpendingCategory.FOOD,
                    title = "Food & Dining",
                    totalAmount = 2000.0,
                    lentAmount = 1500.0,
                    borrowedAmount = 500.0,
                    percentage = 60f,
                    transactionCount = 4
                ),
                SpendingCategoryBreakdown(
                    category = SpendingCategory.TRAVEL,
                    title = "Travel & Commute",
                    totalAmount = 1000.0,
                    lentAmount = 800.0,
                    borrowedAmount = 200.0,
                    percentage = 30f,
                    transactionCount = 2
                )
            ),
            has30DayActivity = true
        )

        composeTestRule.setContent {
            PhittoosTheme {
                DashboardInsightsCard(insights = testInsights)
            }
        }

        // 1. Verify Card presence
        composeTestRule.onNodeWithTag("dashboard_insights_card").assertIsDisplayed()

        // 2. Verify Title and Header
        composeTestRule.onNodeWithText("Dashboard Insights").assertIsDisplayed()
        composeTestRule.onNodeWithText("30 Days").assertIsDisplayed()

        // 3. Verify Lent vs Borrowed Proportions
        composeTestRule.onNodeWithText("Lent").assertIsDisplayed()
        composeTestRule.onNodeWithText("Borrowed").assertIsDisplayed()

        // 4. Verify Trend Chart header & Categories
        composeTestRule.onNodeWithText("30-DAY VOLUME TRENDS").assertIsDisplayed()
        composeTestRule.onNodeWithText("SPENDING TRENDS BY CATEGORY").assertIsDisplayed()
        composeTestRule.onNodeWithText("Food & Dining").assertIsDisplayed()
        composeTestRule.onNodeWithText("Travel & Commute").assertIsDisplayed()

        // 5. Test Week Selection Interaction
        composeTestRule.onNodeWithText("Week 4").performClick()
        composeTestRule.onNodeWithText("Week 4 (This week)").assertIsDisplayed()
    }
}
