package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.TransactionDirection
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionStatus
import com.example.data.model.effectiveRemainingAmount
import com.example.data.repository.PhittoosRepository
import com.example.data.repository.RepaymentErrorReason
import com.example.data.repository.RepaymentResult
import com.example.ui.viewmodel.BulkSettlementEligibility
import com.example.ui.viewmodel.FriendDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OfflineSettlementFlowTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: PhittoosRepository
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = PhittoosRepository(db.friendDao(), db.transactionDao())
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    /**
     * TEST A:
     * User LENT ₹500 to friend.
     * Settle individually:
     * - Status transitions OPEN -> CONFIRMED
     * - Outstanding ₹500 -> ₹0
     * - Transaction remains in timeline history
     */
    @Test
    fun testA_lentIndividualSettlement() = runTest {
        val friendId = repository.insertFriend("Rohan")
        val txId = repository.addTransaction(
            friendId = friendId,
            amount = 500.0,
            direction = TransactionDirection.LENT,
            note = "Lunch",
            dueDate = null
        )

        // Before settlement
        val beforeFriend = repository.getFriendWithBalance(friendId).first()
        assertNotNull(beforeFriend)
        assertEquals(500.0, beforeFriend!!.netBalance, 0.001)
        assertEquals(1, beforeFriend.openTransactionsCount)

        val beforeTotals = repository.dashboardTotals.first()
        assertEquals(500.0, beforeTotals.youWillGetBack, 0.001)

        // Settle individually
        repository.markTransactionAsPaid(txId)

        // After settlement
        val settledTx = repository.getTransactionById(txId)
        assertNotNull(settledTx)
        assertEquals(TransactionStatus.CONFIRMED, settledTx!!.status)

        val afterFriend = repository.getFriendWithBalance(friendId).first()
        assertNotNull(afterFriend)
        assertEquals(0.0, afterFriend!!.netBalance, 0.001)
        assertEquals(0, afterFriend.openTransactionsCount)
        assertEquals(1, afterFriend.totalTransactionsCount)

        val afterTotals = repository.dashboardTotals.first()
        assertEquals(0.0, afterTotals.youWillGetBack, 0.001)
        assertEquals(0.0, afterTotals.youOwe, 0.001)
        assertEquals(0.0, afterTotals.netPosition, 0.001)

        // Verifies transaction is still in timeline history
        val timeline = repository.getTransactionsForFriend(friendId).first()
        assertEquals(1, timeline.size)
        assertEquals(txId, timeline[0].id)
        assertEquals(TransactionStatus.CONFIRMED, timeline[0].status)
    }

    /**
     * TEST B:
     * User BORROWED ₹300 from friend.
     * Settle individually:
     * - Status transitions OPEN -> CONFIRMED
     * - You Owe ₹300 -> ₹0
     * - Friend net balance -₹300 -> ₹0
     */
    @Test
    fun testB_borrowedIndividualSettlement() = runTest {
        val friendId = repository.insertFriend("Priya")
        val txId = repository.addTransaction(
            friendId = friendId,
            amount = 300.0,
            direction = TransactionDirection.BORROWED,
            note = "Metro card recharge",
            dueDate = null
        )

        // Before settlement
        val beforeFriend = repository.getFriendWithBalance(friendId).first()
        assertNotNull(beforeFriend)
        assertEquals(-300.0, beforeFriend!!.netBalance, 0.001)
        assertEquals(1, beforeFriend.openTransactionsCount)

        val beforeTotals = repository.dashboardTotals.first()
        assertEquals(300.0, beforeTotals.youOwe, 0.001)
        assertEquals(-300.0, beforeTotals.netPosition, 0.001)

        // Settle individually
        repository.markTransactionAsPaid(txId)

        // After settlement
        val settledTx = repository.getTransactionById(txId)
        assertNotNull(settledTx)
        assertEquals(TransactionStatus.CONFIRMED, settledTx!!.status)

        val afterFriend = repository.getFriendWithBalance(friendId).first()
        assertNotNull(afterFriend)
        assertEquals(0.0, afterFriend!!.netBalance, 0.001)
        assertEquals(0, afterFriend.openTransactionsCount)

        val afterTotals = repository.dashboardTotals.first()
        assertEquals(0.0, afterTotals.youOwe, 0.001)
        assertEquals(0.0, afterTotals.netPosition, 0.001)
    }

    /**
     * TEST C:
     * Mixed-direction safety:
     * Friend has LENT ₹500 and BORROWED ₹300.
     * Net balance = +₹200, openCount = 2.
     * Bulk settlement MUST NOT blindly confirm both.
     * settleAllSameDirectionForFriend returns false and modifies nothing.
     * markAllForFriendAsPaid protects against mixed bulk settlement.
     */
    @Test
    fun testC_mixedDirectionBulkSettlementIsPrevented() = runTest {
        val friendId = repository.insertFriend("Amit")
        val tx1 = repository.addTransaction(
            friendId = friendId,
            amount = 500.0,
            direction = TransactionDirection.LENT,
            note = "Lent for movie",
            dueDate = null
        )
        val tx2 = repository.addTransaction(
            friendId = friendId,
            amount = 300.0,
            direction = TransactionDirection.BORROWED,
            note = "Borrowed for snacks",
            dueDate = null
        )

        // Pre-condition
        val friendState = repository.getFriendWithBalance(friendId).first()
        assertNotNull(friendState)
        assertEquals(200.0, friendState!!.netBalance, 0.001)
        assertEquals(2, friendState.openTransactionsCount)

        // Attempting bulk settlement on mixed directions
        val success = repository.settleAllSameDirectionForFriend(friendId)
        assertFalse("Bulk settlement must be rejected for mixed directions", success)

        // Verify both transactions are still strictly OPEN
        val checkTx1 = repository.getTransactionById(tx1)
        val checkTx2 = repository.getTransactionById(tx2)
        assertEquals(TransactionStatus.OPEN, checkTx1!!.status)
        assertEquals(TransactionStatus.OPEN, checkTx2!!.status)

        // Legacy/wrapper method markAllForFriendAsPaid must also safely reject
        val legacyResult = repository.markAllForFriendAsPaid(friendId)
        assertFalse("markAllForFriendAsPaid must be safe against mixed directions", legacyResult)
        assertEquals(TransactionStatus.OPEN, repository.getTransactionById(tx1)!!.status)
        assertEquals(TransactionStatus.OPEN, repository.getTransactionById(tx2)!!.status)
    }

    /**
     * TEST D:
     * Zero-net edge case:
     * LENT ₹500, BORROWED ₹500 -> Net balance = ₹0, openCount = 2.
     * The domain and ViewModel state must NOT treat this as fully settled.
     */
    @Test
    fun testD_zeroNetWithOpenTransactionsIsNotSettled() = runTest {
        val friendId = repository.insertFriend("Karan")
        repository.addTransaction(
            friendId = friendId,
            amount = 500.0,
            direction = TransactionDirection.LENT,
            note = "Dinner share",
            dueDate = null
        )
        repository.addTransaction(
            friendId = friendId,
            amount = 500.0,
            direction = TransactionDirection.BORROWED,
            note = "Cab share",
            dueDate = null
        )

        val friendWithBalance = repository.getFriendWithBalance(friendId).first()
        assertNotNull(friendWithBalance)
        assertEquals(0.0, friendWithBalance!!.netBalance, 0.001)
        // Critical: openTransactionsCount is 2, not 0!
        assertEquals(2, friendWithBalance.openTransactionsCount)

        val vm = FriendDetailViewModel(repository, friendId)
        val uiState = vm.uiState.first { it.friend != null }
        assertEquals(0.0, uiState.netBalance, 0.001)
        assertEquals(2, uiState.openTransactionsCount)
        // Must be identified as mixed directions, not eligible for bulk settle
        assertEquals(BulkSettlementEligibility.MIXED_DIRECTIONS, uiState.bulkSettlementEligibility)
    }

    /**
     * TEST E:
     * Same-direction LENT bulk settlement:
     * LENT ₹500, LENT ₹300, LENT ₹200.
     * Total remaining = ₹1,000.
     * Bulk settlement confirms all 3, outstanding = ₹0.
     */
    @Test
    fun testE_sameDirectionLentBulkSettlement() = runTest {
        val friendId = repository.insertFriend("Sameer")
        val tx1 = repository.addTransaction(friendId, 500.0, TransactionDirection.LENT, "Tx1", null)
        val tx2 = repository.addTransaction(friendId, 300.0, TransactionDirection.LENT, "Tx2", null)
        val tx3 = repository.addTransaction(friendId, 200.0, TransactionDirection.LENT, "Tx3", null)

        val vm = FriendDetailViewModel(repository, friendId)
        val stateBefore = vm.uiState.first { it.friend != null && it.timeline.size == 3 }
        assertEquals(BulkSettlementEligibility.SAME_DIRECTION_LENT, stateBefore.bulkSettlementEligibility)
        assertEquals(1000.0, stateBefore.bulkSettlementTotalRemaining, 0.001)
        assertEquals(3, stateBefore.bulkSettlementOpenCount)

        // Perform bulk settlement
        val success = repository.settleAllSameDirectionForFriend(friendId)
        assertTrue(success)

        assertEquals(TransactionStatus.CONFIRMED, repository.getTransactionById(tx1)!!.status)
        assertEquals(TransactionStatus.CONFIRMED, repository.getTransactionById(tx2)!!.status)
        assertEquals(TransactionStatus.CONFIRMED, repository.getTransactionById(tx3)!!.status)

        val stateAfter = vm.uiState.first { it.openTransactionsCount == 0 }
        assertEquals(0.0, stateAfter.netBalance, 0.001)
        assertEquals(BulkSettlementEligibility.NONE, stateAfter.bulkSettlementEligibility)
        assertEquals(0.0, stateAfter.bulkSettlementTotalRemaining, 0.001)
    }

    /**
     * TEST F:
     * Same-direction BORROWED bulk settlement:
     * BORROWED ₹500, BORROWED ₹300 -> Bulk remaining = ₹800.
     * After bulk settlement: You Owe = ₹0, both CONFIRMED.
     */
    @Test
    fun testF_sameDirectionBorrowedBulkSettlement() = runTest {
        val friendId = repository.insertFriend("Neha")
        val tx1 = repository.addTransaction(friendId, 500.0, TransactionDirection.BORROWED, "Rent part", null)
        val tx2 = repository.addTransaction(friendId, 300.0, TransactionDirection.BORROWED, "Groceries", null)

        val vm = FriendDetailViewModel(repository, friendId)
        val stateBefore = vm.uiState.first { it.friend != null && it.timeline.size == 2 }
        assertEquals(BulkSettlementEligibility.SAME_DIRECTION_BORROWED, stateBefore.bulkSettlementEligibility)
        assertEquals(800.0, stateBefore.bulkSettlementTotalRemaining, 0.001)

        val success = repository.settleAllSameDirectionForFriend(friendId)
        assertTrue(success)

        assertEquals(TransactionStatus.CONFIRMED, repository.getTransactionById(tx1)!!.status)
        assertEquals(TransactionStatus.CONFIRMED, repository.getTransactionById(tx2)!!.status)

        val totals = repository.dashboardTotals.first()
        assertEquals(0.0, totals.youOwe, 0.001)
        assertEquals(0.0, totals.netPosition, 0.001)
    }

    /**
     * TEST G:
     * Partial payment handling:
     * Amount = ₹500, paidAmount = ₹200, status = OPEN.
     * Effective remaining = ₹300.
     * Bulk remaining and settlement amount is ₹300, not ₹500.
     */
    @Test
    fun testG_partialPaymentRemainingAmount() = runTest {
        val friendId = repository.insertFriend("Vikram")
        val tx = TransactionEntity(
            friendId = friendId,
            amount = 500.0,
            direction = TransactionDirection.LENT,
            paidAmount = 200.0,
            status = TransactionStatus.OPEN
        )
        val txId = db.transactionDao().insertTransaction(tx)

        val retrievedTx = repository.getTransactionById(txId)
        assertNotNull(retrievedTx)
        assertEquals(300.0, retrievedTx!!.effectiveRemainingAmount, 0.001)

        val vm = FriendDetailViewModel(repository, friendId)
        val uiState = vm.uiState.first { it.friend != null && it.timeline.isNotEmpty() }
        assertEquals(300.0, uiState.netBalance, 0.001)
        assertEquals(300.0, uiState.bulkSettlementTotalRemaining, 0.001)

        // Settle the remaining transaction
        repository.markTransactionAsPaid(txId)
        assertEquals(TransactionStatus.CONFIRMED, repository.getTransactionById(txId)!!.status)
        val stateAfter = vm.uiState.first { it.openTransactionsCount == 0 }
        assertEquals(0.0, stateAfter.netBalance, 0.001)
    }

    /**
     * TEST H:
     * Overpaid defensive handling:
     * Amount = ₹500, paidAmount = ₹700, status = OPEN.
     * Effective remaining must be clamped at ₹0, never negative.
     */
    @Test
    fun testH_overpaidDefensiveState() = runTest {
        val tx = TransactionEntity(
            friendId = 1L,
            amount = 500.0,
            direction = TransactionDirection.LENT,
            paidAmount = 700.0,
            status = TransactionStatus.OPEN
        )
        assertEquals(0.0, tx.effectiveRemainingAmount, 0.001)
    }

    /**
     * TEST I:
     * Idempotency & Repeat Settlement:
     * Already CONFIRMED transaction settled again:
     * - No corruption of balances
     * - Status remains CONFIRMED
     * - No duplicates created
     */
    @Test
    fun testI_idempotentSettlement() = runTest {
        val friendId = repository.insertFriend("Deepak")
        val txId = repository.addTransaction(friendId, 400.0, TransactionDirection.LENT, "Test", null)

        // Settle once
        repository.markTransactionAsPaid(txId)
        assertEquals(TransactionStatus.CONFIRMED, repository.getTransactionById(txId)!!.status)

        // Settle second and third time
        repository.markTransactionAsPaid(txId)
        repository.markTransactionAsPaid(txId)

        val timeline = repository.getTransactionsForFriend(friendId).first()
        assertEquals(1, timeline.size)
        assertEquals(TransactionStatus.CONFIRMED, timeline[0].status)

        val totals = repository.dashboardTotals.first()
        assertEquals(0.0, totals.youWillGetBack, 0.001)
        assertEquals(0.0, totals.netPosition, 0.001)
    }

    /**
     * TEST J:
     * Mixed transactions remain traceable when settling only one direction:
     * Friend with LENT ₹500 (tx1) and BORROWED ₹300 (tx2).
     * Settle LENT individually:
     * - tx1 becomes CONFIRMED
     * - tx2 remains OPEN
     * - Friend net balance becomes -₹300
     * - Dashboard: Get Back = ₹0, You Owe = ₹300
     * - Now only BORROWED is open, friend becomes eligible for BORROWED bulk settle
     */
    @Test
    fun testJ_settlingSingleTransactionInMixedAccount() = runTest {
        val friendId = repository.insertFriend("Suresh")
        val tx1 = repository.addTransaction(friendId, 500.0, TransactionDirection.LENT, "Lent", null)
        val tx2 = repository.addTransaction(friendId, 300.0, TransactionDirection.BORROWED, "Borrowed", null)

        val vm = FriendDetailViewModel(repository, friendId)
        val stateInitial = vm.uiState.first { it.friend != null && it.timeline.size == 2 }
        assertEquals(BulkSettlementEligibility.MIXED_DIRECTIONS, stateInitial.bulkSettlementEligibility)

        // Settle tx1 (LENT) only
        repository.markTransactionAsPaid(tx1)

        val tx1After = repository.getTransactionById(tx1)
        val tx2After = repository.getTransactionById(tx2)
        assertEquals(TransactionStatus.CONFIRMED, tx1After!!.status)
        assertEquals(TransactionStatus.OPEN, tx2After!!.status)

        val friendAfterFirstSettle = repository.getFriendWithBalance(friendId).first()
        assertNotNull(friendAfterFirstSettle)
        assertEquals(-300.0, friendAfterFirstSettle!!.netBalance, 0.001)
        assertEquals(1, friendAfterFirstSettle.openTransactionsCount)

        val vm2 = FriendDetailViewModel(repository, friendId)
        val stateAfterFirstSettle = vm2.uiState.first { it.openTransactionsCount == 1 }
        assertEquals(-300.0, stateAfterFirstSettle.netBalance, 0.001)
        // With only BORROWED remaining, eligibility transitions to SAME_DIRECTION_BORROWED!
        assertEquals(BulkSettlementEligibility.SAME_DIRECTION_BORROWED, stateAfterFirstSettle.bulkSettlementEligibility)
        assertEquals(300.0, stateAfterFirstSettle.bulkSettlementTotalRemaining, 0.001)

        val totals = repository.dashboardTotals.first()
        assertEquals(0.0, totals.youWillGetBack, 0.001)
        assertEquals(300.0, totals.youOwe, 0.001)
        assertEquals(-300.0, totals.netPosition, 0.001)

        // Now settle tx2 using bulk settlement
        val bulkResult = repository.settleAllSameDirectionForFriend(friendId)
        assertTrue(bulkResult)

        val tx2Final = repository.getTransactionById(tx2)
        assertEquals(TransactionStatus.CONFIRMED, tx2Final!!.status)

        val finalFriend = repository.getFriendWithBalance(friendId).first()
        assertNotNull(finalFriend)
        assertEquals(0.0, finalFriend!!.netBalance, 0.001)
        assertEquals(0, finalFriend.openTransactionsCount)
    }

    /**
     * TEST K:
     * Verify plain-language user feedback messages in ViewModel:
     * - "Marked as fully paid."
     * - "Selected transactions marked as fully paid."
     * - "Repayment of ₹X recorded."
     * - "Final repayment of ₹X recorded."
     */
    @Test
    fun testK_plainLanguageSettlementMessages() = runTest {
        val friendId = repository.insertFriend("Manoj")
        val txId = repository.addTransaction(friendId, 500.0, TransactionDirection.LENT, "Loan")

        val vm = FriendDetailViewModel(repository, friendId)

        // 1. Partial repayment
        vm.recordRepayment(txId, 200.0)
        testScheduler.advanceUntilIdle()
        val stateAfterPartial = vm.uiState.first { it.toastMessage != null }
        assertEquals("Repayment of ₹200 recorded.", stateAfterPartial.toastMessage)

        vm.clearToast()

        // 2. Final repayment completing the transaction
        vm.recordRepayment(txId, 300.0)
        testScheduler.advanceUntilIdle()
        val stateAfterFinal = vm.uiState.first { it.toastMessage != null }
        assertEquals("Final repayment of ₹300 recorded.", stateAfterFinal.toastMessage)

        vm.clearToast()

        // 3. Single transaction settle
        val txId2 = repository.addTransaction(friendId, 400.0, TransactionDirection.BORROWED, "Dinner")
        vm.settleTransaction(txId2)
        testScheduler.advanceUntilIdle()
        val stateAfterSingle = vm.uiState.first { it.toastMessage != null }
        assertEquals("Marked as fully paid.", stateAfterSingle.toastMessage)

        vm.clearToast()

        // 4. Bulk same-direction settle
        repository.addTransaction(friendId, 100.0, TransactionDirection.LENT, "Snacks 1")
        repository.addTransaction(friendId, 150.0, TransactionDirection.LENT, "Snacks 2")
        vm.settleAllSameDirection()
        testScheduler.advanceUntilIdle()
        val stateAfterBulk = vm.uiState.first { it.toastMessage != null }
        assertEquals("Selected transactions marked as fully paid.", stateAfterBulk.toastMessage)
    }

    /**
     * TEST L:
     * Verify plain-language error messages and typed error reasons:
     * - Invalid repayment amount -> "Enter an amount greater than ₹0."
     * - Non-existent transaction -> "This transaction could not be found."
     * - Already settled transaction -> "This transaction is already fully paid."
     * - Exceeds remaining amount -> "Enter ₹X or less. That’s the amount left to pay."
     */
    @Test
    fun testL_plainLanguageRepaymentErrorMessages() = runTest {
        val friendId = repository.insertFriend("Sunita")
        val txId = repository.addTransaction(friendId, 500.0, TransactionDirection.LENT, "Loan")

        // 1. Invalid repayment amount <= 0
        val resZero = repository.recordRepayment(txId, 0.0)
        assertTrue(resZero is RepaymentResult.Error)
        val errZero = resZero as RepaymentResult.Error
        assertEquals("Enter an amount greater than ₹0.", errZero.message)
        assertEquals(RepaymentErrorReason.INVALID_AMOUNT, errZero.reason)

        val resNeg = repository.recordRepayment(txId, -50.0)
        assertTrue(resNeg is RepaymentResult.Error)
        assertEquals(RepaymentErrorReason.INVALID_AMOUNT, (resNeg as RepaymentResult.Error).reason)

        // 2. Repayment exceeds remaining amount
        val resExceed = repository.recordRepayment(txId, 600.0)
        assertTrue(resExceed is RepaymentResult.Error)
        val errExceed = resExceed as RepaymentResult.Error
        assertEquals("Enter ₹500 or less. That’s the amount left to pay.", errExceed.message)
        assertEquals(RepaymentErrorReason.EXCEEDS_REMAINING, errExceed.reason)

        // 3. Non-existent transaction
        val resNotFound = repository.recordRepayment(99999L, 100.0)
        assertTrue(resNotFound is RepaymentResult.Error)
        val errNotFound = resNotFound as RepaymentResult.Error
        assertEquals("This transaction could not be found.", errNotFound.message)
        assertEquals(RepaymentErrorReason.TRANSACTION_NOT_FOUND, errNotFound.reason)

        // 4. Already settled transaction
        repository.markTransactionAsPaid(txId)
        val resAlreadySettled = repository.recordRepayment(txId, 100.0)
        assertTrue(resAlreadySettled is RepaymentResult.Error)
        val errAlreadySettled = resAlreadySettled as RepaymentResult.Error
        assertEquals("This transaction is already fully paid.", errAlreadySettled.message)
        assertEquals(RepaymentErrorReason.ALREADY_SETTLED, errAlreadySettled.reason)
    }
}
