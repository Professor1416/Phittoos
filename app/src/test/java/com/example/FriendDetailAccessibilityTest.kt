package com.example

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import com.example.data.model.TransactionDirection
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionStatus
import com.example.ui.screens.frienddetail.ActionRow
import com.example.ui.screens.frienddetail.BulkSettlementDialog
import com.example.ui.screens.frienddetail.IndividualSettlementDialog
import com.example.ui.screens.frienddetail.RepaymentDialog
import com.example.ui.screens.frienddetail.TimelineTransactionItem
import com.example.ui.theme.PhittoosTheme
import com.example.ui.util.Formatters
import com.example.ui.viewmodel.BulkSettlementEligibility
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w400dp-h1000dp")
class FriendDetailAccessibilityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createSampleTransaction(
        id: Long = 42L,
        friendId: Long = 1L,
        amount: Double = 500.0,
        paidAmount: Double = 0.0,
        direction: TransactionDirection = TransactionDirection.LENT,
        status: TransactionStatus = TransactionStatus.OPEN,
        note: String? = "Dinner split",
        createdDate: Long = 1773000000000L
    ): TransactionEntity {
        return TransactionEntity(
            id = id,
            friendId = friendId,
            amount = amount,
            paidAmount = paidAmount,
            direction = direction,
            status = status,
            note = note,
            createdDate = createdDate
        )
    }

    @Test
    fun testTimelineActionTargetSizesAndNonOverlappingBounds() {
        val tx = createSampleTransaction()
        var repayClicks = 0
        var settleClicks = 0

        composeTestRule.setContent {
            PhittoosTheme {
                TimelineTransactionItem(
                    tx = tx,
                    onSettleClick = { settleClicks++ },
                    onRepayClick = { repayClicks++ }
                )
            }
        }

        val repayNode = composeTestRule.onNodeWithTag("button_repay_42")
        val settleNode = composeTestRule.onNodeWithTag("button_settle_42")

        // 1. Ensure effective touch targets are at least 48dp x 48dp
        repayNode.assertWidthIsAtLeast(48.dp)
        repayNode.assertHeightIsAtLeast(48.dp)

        settleNode.assertWidthIsAtLeast(48.dp)
        settleNode.assertHeightIsAtLeast(48.dp)

        // 2. Ensure non-overlapping bounds
        val repayBounds = repayNode.getUnclippedBoundsInRoot()
        val settleBounds = settleNode.getUnclippedBoundsInRoot()

        val isHorizontallySeparated = repayBounds.right <= settleBounds.left || settleBounds.right <= repayBounds.left
        val isVerticallySeparated = repayBounds.bottom <= settleBounds.top || settleBounds.bottom <= repayBounds.top
        assertTrue("Buttons must not overlap: repay=$repayBounds, settle=$settleBounds",
            isHorizontallySeparated || isVerticallySeparated
        )

        // 3. Each activation invokes its existing action exactly once
        repayNode.performClick()
        assertEquals(1, repayClicks)
        assertEquals(0, settleClicks)

        settleNode.performClick()
        assertEquals(1, repayClicks)
        assertEquals(1, settleClicks)
    }

    @Test
    fun testAccessibleActionContextDoesNotExposeDatabaseId() {
        val tx = createSampleTransaction(id = 999L, amount = 750.0, note = "Movie tickets")

        composeTestRule.setContent {
            PhittoosTheme {
                TimelineTransactionItem(
                    tx = tx,
                    onSettleClick = {},
                    onRepayClick = {}
                )
            }
        }

        val repayNode = composeTestRule.onNodeWithTag("button_repay_999")
        val settleNode = composeTestRule.onNodeWithTag("button_settle_999")

        // Assert visible plain language labels are preserved
        composeTestRule.onNodeWithText("Record repayment").assertExists()
        composeTestRule.onNodeWithText("Settle this").assertExists()

        // Assert accessible contentDescription contains amount, direction and note, but not database id
        val formattedAmt = Formatters.formatCurrency(750.0)
        repayNode.assert(SemanticsMatcher("Has repay context description") { node ->
            val cdList = node.config.getOrElse(SemanticsProperties.ContentDescription) { emptyList() }
            val cd = cdList.joinToString(" ")
            cd.contains(formattedAmt) && cd.contains("lent") && !cd.contains("999")
        })

        settleNode.assert(SemanticsMatcher("Has settle context description") { node ->
            val cdList = node.config.getOrElse(SemanticsProperties.ContentDescription) { emptyList() }
            val cd = cdList.joinToString(" ")
            cd.contains(formattedAmt) && cd.contains("lent") && !cd.contains("999")
        })
    }

    @Test
    fun testRepaymentInputLabelEditableSemanticsAndErrorSemantics() {
        val tx = createSampleTransaction(amount = 1000.0, paidAmount = 200.0)
        var confirmedAmount: Double? = null
        var dismissed = false

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            PhittoosTheme {
                RepaymentDialog(
                    friendName = "Rohan",
                    tx = tx,
                    onConfirm = { confirmedAmount = it },
                    onDismiss = { dismissed = true }
                )
            }
        }
        composeTestRule.mainClock.advanceTimeByFrame()

        // 1. Label identifying repayment amount in rupees
        composeTestRule.onNodeWithText("Repayment amount in rupees").assertExists()

        // 2. Amount field allows text input (editable text semantics)
        val amountInput = composeTestRule.onNodeWithTag("input_repayment_amount")
        amountInput.performTextInput("150")
        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.onNodeWithText("Record ₹150").assertExists()

        // 3. Fill remaining button touch target & functionality
        val fillBtn = composeTestRule.onNodeWithTag("button_fill_remaining")
        fillBtn.assertHeightIsAtLeast(48.dp)
        fillBtn.assertWidthIsAtLeast(48.dp)
        fillBtn.performClick()
        composeTestRule.mainClock.advanceTimeByFrame()

        // Remaining balance is 800 (1000 - 200)
        composeTestRule.onNodeWithText("Record ₹800").assertExists()

        // 4. Exceeding remaining amount triggers error semantics on the input
        amountInput.performTextInput("9999")
        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.onNodeWithTag("button_confirm_record_repayment").performClick()
        composeTestRule.mainClock.advanceTimeByFrame()

        // Native error semantics on the input field
        amountInput.assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
        composeTestRule.onNodeWithTag("text_repayment_error").assertExists()
        assertEquals("Should not confirm when error present", null, confirmedAmount)
    }

    @Test
    fun testMixedDirectionBulkSettlementRemainsDisabledWithAccessibleGuidance() {
        composeTestRule.setContent {
            PhittoosTheme {
                ActionRow(
                    eligibility = BulkSettlementEligibility.MIXED_DIRECTIONS,
                    onAddTransaction = {},
                    onRequestBulkSettle = {}
                )
            }
        }

        val bulkButton = composeTestRule.onNodeWithTag("button_detail_mark_paid")
        bulkButton.assertIsNotEnabled()

        // Accessible stateDescription explains mixed direction guidance
        bulkButton.assert(SemanticsMatcher("Has stateDescription") { node ->
            val sd = node.config.getOrElse(SemanticsProperties.StateDescription) { "" }
            sd.contains("individually") && sd.contains("both directions")
        })
    }

    @Test
    fun testDialogDismissActionsDoNotTriggerConfirm() {
        var repayConfirmed = false
        var repayDismissed = false
        val tx = createSampleTransaction(amount = 500.0)

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            PhittoosTheme {
                RepaymentDialog(
                    friendName = "Akash",
                    tx = tx,
                    onConfirm = { repayConfirmed = true },
                    onDismiss = { repayDismissed = true }
                )
            }
        }
        composeTestRule.mainClock.advanceTimeByFrame()

        // Cancel button has touch target and does not confirm
        val cancelBtn = composeTestRule.onNodeWithTag("button_cancel_record_repayment")
        cancelBtn.assertHeightIsAtLeast(48.dp)
        cancelBtn.assertWidthIsAtLeast(48.dp)
        cancelBtn.performClick()
        composeTestRule.mainClock.advanceTimeByFrame()

        assertTrue(repayDismissed)
        assertFalse(repayConfirmed)
    }

    @Test
    fun testIndividualSettlementDialogDismissDoesNotTriggerConfirm() {
        var settleConfirmed = false
        var settleDismissed = false
        val tx = createSampleTransaction(amount = 300.0)

        composeTestRule.setContent {
            PhittoosTheme {
                IndividualSettlementDialog(
                    friendName = "Pooja",
                    tx = tx,
                    onConfirm = { settleConfirmed = true },
                    onDismiss = { settleDismissed = true }
                )
            }
        }

        val keepPendingBtn = composeTestRule.onNodeWithTag("button_cancel_settle_individual")
        keepPendingBtn.assertHeightIsAtLeast(48.dp)
        keepPendingBtn.assertWidthIsAtLeast(48.dp)
        keepPendingBtn.performClick()

        assertTrue(settleDismissed)
        assertFalse(settleConfirmed)
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp")
    fun testDialogActionsRemainReachableAtNarrowViewportAndLargeFontScale() {
        composeTestRule.setContent {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(
                    androidx.compose.ui.platform.LocalDensity.current.density,
                    fontScale = 2.0f
                )
            ) {
                PhittoosTheme {
                    BulkSettlementDialog(
                        friendName = "Aman",
                        eligibility = BulkSettlementEligibility.SAME_DIRECTION_LENT,
                        totalRemaining = 500.0,
                        openCount = 1,
                        onConfirm = {},
                        onDismiss = {}
                    )
                }
            }
        }

        // Actions remain displayed and reachable even at 320dp width and 2.0x font scale
        composeTestRule.onNodeWithTag("button_confirm_settle_bulk").assertIsDisplayed()
        composeTestRule.onNodeWithTag("button_cancel_settle_bulk").assertIsDisplayed()
    }
}
