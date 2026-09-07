package com.example

import android.content.Context
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.TransactionDirection
import com.example.data.model.TransactionStatus
import com.example.data.model.effectivePaidAmount
import com.example.data.model.effectiveRemainingAmount
import com.example.data.repository.PhittoosRepository
import com.example.data.repository.RepaymentResult
import com.example.ui.viewmodel.FriendDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PartialRepaymentEngineTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: PhittoosRepository
    private val testDispatcher = UnconfinedTestDispatcher()

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
     * TEST A: Partial repayment on LENT transaction
     * User lent ₹500 to Akash.
     * Akash repays ₹300.
     * Verify:
     * - paidAmount = 300.0
     * - effectiveRemainingAmount = 200.0
     * - status = OPEN
     * - dashboard totals: youWillGetBack = 200.0
     */
    @Test
    fun testA_partialRepaymentOnLentTransaction() = runTest {
        val friendId = repository.insertFriend("Akash")
        val txId = repository.addTransaction(
            friendId = friendId,
            amount = 500.0,
            direction = TransactionDirection.LENT
        )

        val result = repository.recordRepayment(txId, 300.0)
        assertTrue(result is RepaymentResult.Success)
        assertFalse((result as RepaymentResult.Success).isFullySettled)

        val updatedTx = repository.getTransactionById(txId)
        assertNotNull(updatedTx)
        assertEquals(300.0, updatedTx!!.paidAmount!!, 0.001)
        assertEquals(300.0, updatedTx.effectivePaidAmount, 0.001)
        assertEquals(200.0, updatedTx.effectiveRemainingAmount, 0.001)
        assertEquals(TransactionStatus.OPEN, updatedTx.status)

        val totals = repository.dashboardTotals.first()
        assertEquals(200.0, totals.youWillGetBack, 0.001)
        assertEquals(0.0, totals.youOwe, 0.001)
        assertEquals(200.0, totals.netPosition, 0.001)
    }

    /**
     * TEST B: Partial repayment on BORROWED transaction
     * User borrowed ₹500 from Akash.
     * User repays ₹300.
     * Verify:
     * - paidAmount = 300.0
     * - effectiveRemainingAmount = 200.0
     * - status = OPEN
     * - dashboard totals: youOwe = 200.0
     */
    @Test
    fun testB_partialRepaymentOnBorrowedTransaction() = runTest {
        val friendId = repository.insertFriend("Akash")
        val txId = repository.addTransaction(
            friendId = friendId,
            amount = 500.0,
            direction = TransactionDirection.BORROWED
        )

        val result = repository.recordRepayment(txId, 300.0)
        assertTrue(result is RepaymentResult.Success)
        assertFalse((result as RepaymentResult.Success).isFullySettled)

        val updatedTx = repository.getTransactionById(txId)
        assertNotNull(updatedTx)
        assertEquals(300.0, updatedTx!!.paidAmount!!, 0.001)
        assertEquals(200.0, updatedTx.effectiveRemainingAmount, 0.001)
        assertEquals(TransactionStatus.OPEN, updatedTx.status)

        val totals = repository.dashboardTotals.first()
        assertEquals(0.0, totals.youWillGetBack, 0.001)
        assertEquals(200.0, totals.youOwe, 0.001)
        assertEquals(-200.0, totals.netPosition, 0.001)
    }

    /**
     * TEST C: Multiple consecutive partial repayments
     * Lent ₹500.
     * Repayment 1: ₹100 -> paidAmount = 100, remaining = 400, OPEN.
     * Repayment 2: ₹150 -> paidAmount = 250, remaining = 250, OPEN.
     */
    @Test
    fun testC_multipleConsecutivePartialRepayments() = runTest {
        val friendId = repository.insertFriend("Priya")
        val txId = repository.addTransaction(
            friendId = friendId,
            amount = 500.0,
            direction = TransactionDirection.LENT
        )

        val result1 = repository.recordRepayment(txId, 100.0)
        assertTrue(result1 is RepaymentResult.Success)
        var tx = repository.getTransactionById(txId)!!
        assertEquals(100.0, tx.effectivePaidAmount, 0.001)
        assertEquals(400.0, tx.effectiveRemainingAmount, 0.001)
        assertEquals(TransactionStatus.OPEN, tx.status)

        val result2 = repository.recordRepayment(txId, 150.0)
        assertTrue(result2 is RepaymentResult.Success)
        tx = repository.getTransactionById(txId)!!
        assertEquals(250.0, tx.effectivePaidAmount, 0.001)
        assertEquals(250.0, tx.effectiveRemainingAmount, 0.001)
        assertEquals(TransactionStatus.OPEN, tx.status)
    }

    /**
     * TEST D: Exact final repayment (clearing balance)
     * Lent ₹500.
     * Repayment 1: ₹300 (remaining ₹200).
     * Repayment 2: ₹200 (clearing balance).
     * Verify:
     * - paidAmount = 500.0
     * - remaining = 0.0
     * - status = CONFIRMED
     * - isFullySettled = true
     * - dashboard totals: youWillGetBack = 0.0
     */
    @Test
    fun testD_exactFinalRepaymentTransitionsToConfirmed() = runTest {
        val friendId = repository.insertFriend("Priya")
        val txId = repository.addTransaction(
            friendId = friendId,
            amount = 500.0,
            direction = TransactionDirection.LENT
        )

        repository.recordRepayment(txId, 300.0)
        val finalResult = repository.recordRepayment(txId, 200.0)
        assertTrue(finalResult is RepaymentResult.Success)
        assertTrue((finalResult as RepaymentResult.Success).isFullySettled)

        val tx = repository.getTransactionById(txId)!!
        assertEquals(500.0, tx.effectivePaidAmount, 0.001)
        assertEquals(0.0, tx.effectiveRemainingAmount, 0.001)
        assertEquals(TransactionStatus.CONFIRMED, tx.status)

        val totals = repository.dashboardTotals.first()
        assertEquals(0.0, totals.youWillGetBack, 0.001)
    }

    /**
     * TEST E: Overpayment rejected
     * Lent ₹500, already paid ₹300, remaining ₹200.
     * Attempt repayment of ₹201.
     * Verify:
     * - Returns RepaymentResult.Error
     * - DB remains unchanged (paidAmount = 300, remaining = 200, OPEN)
     */
    @Test
    fun testE_overpaymentRejected() = runTest {
        val friendId = repository.insertFriend("Vikram")
        val txId = repository.addTransaction(
            friendId = friendId,
            amount = 500.0,
            direction = TransactionDirection.LENT
        )
        repository.recordRepayment(txId, 300.0)

        val overpayResult = repository.recordRepayment(txId, 201.0)
        assertTrue(overpayResult is RepaymentResult.Error)

        val tx = repository.getTransactionById(txId)!!
        assertEquals(300.0, tx.effectivePaidAmount, 0.001)
        assertEquals(200.0, tx.effectiveRemainingAmount, 0.001)
        assertEquals(TransactionStatus.OPEN, tx.status)
    }

    /**
     * TEST F: Zero amount rejected
     */
    @Test
    fun testF_zeroAmountRejected() = runTest {
        val friendId = repository.insertFriend("Vikram")
        val txId = repository.addTransaction(
            friendId = friendId,
            amount = 500.0,
            direction = TransactionDirection.LENT
        )

        val result = repository.recordRepayment(txId, 0.0)
        assertTrue(result is RepaymentResult.Error)

        val tx = repository.getTransactionById(txId)!!
        assertEquals(500.0, tx.effectiveRemainingAmount, 0.001)
        assertEquals(TransactionStatus.OPEN, tx.status)
    }

    /**
     * TEST G: Negative amount rejected
     */
    @Test
    fun testG_negativeAmountRejected() = runTest {
        val friendId = repository.insertFriend("Vikram")
        val txId = repository.addTransaction(
            friendId = friendId,
            amount = 500.0,
            direction = TransactionDirection.LENT
        )

        val result = repository.recordRepayment(txId, -50.0)
        assertTrue(result is RepaymentResult.Error)

        val tx = repository.getTransactionById(txId)!!
        assertEquals(500.0, tx.effectiveRemainingAmount, 0.001)
        assertEquals(TransactionStatus.OPEN, tx.status)
    }

    /**
     * TEST H: Repayment on already CONFIRMED transaction rejected
     */
    @Test
    fun testH_alreadyConfirmedTransactionRejected() = runTest {
        val friendId = repository.insertFriend("Rahul")
        val txId = repository.addTransaction(
            friendId = friendId,
            amount = 500.0,
            direction = TransactionDirection.LENT
        )
        repository.markTransactionAsPaid(txId)

        val result = repository.recordRepayment(txId, 100.0)
        assertTrue(result is RepaymentResult.Error)

        val tx = repository.getTransactionById(txId)!!
        assertEquals(TransactionStatus.CONFIRMED, tx.status)
    }

    /**
     * TEST I: Mixed directions with partial repayment
     * Lent ₹500, Borrowed ₹300.
     * Repay ₹200 towards Lent transaction.
     * Lent remaining = ₹300, Borrowed remaining = ₹300.
     * Net balance = 0.0.
     * Both transactions remain OPEN. Open count = 2.
     */
    @Test
    fun testI_mixedDirectionsWithPartialRepayment() = runTest {
        val friendId = repository.insertFriend("Sameer")
        val lentTxId = repository.addTransaction(
            friendId = friendId,
            amount = 500.0,
            direction = TransactionDirection.LENT
        )
        val borrowedTxId = repository.addTransaction(
            friendId = friendId,
            amount = 300.0,
            direction = TransactionDirection.BORROWED
        )

        val result = repository.recordRepayment(lentTxId, 200.0)
        assertTrue(result is RepaymentResult.Success)

        val lentTx = repository.getTransactionById(lentTxId)!!
        val borrowedTx = repository.getTransactionById(borrowedTxId)!!

        assertEquals(300.0, lentTx.effectiveRemainingAmount, 0.001)
        assertEquals(300.0, borrowedTx.effectiveRemainingAmount, 0.001)
        assertEquals(TransactionStatus.OPEN, lentTx.status)
        assertEquals(TransactionStatus.OPEN, borrowedTx.status)

        val friends = repository.friendsWithBalance.first()
        val friend = friends.first { it.friend.id == friendId }
        assertEquals(0.0, friend.netBalance, 0.001)
        assertEquals(2, friend.openTransactionsCount)
    }

    /**
     * TEST J: Partial repayment followed by manual full settlement
     * Lent ₹500, repaid ₹300 (remaining ₹200).
     * Mark as settled (manual full settlement).
     * Verify:
     * - status = CONFIRMED
     * - paidAmount = 500.0
     * - remaining = 0.0
     */
    @Test
    fun testJ_partialRepaymentFollowedByManualFullSettlement() = runTest {
        val friendId = repository.insertFriend("Anita")
        val txId = repository.addTransaction(
            friendId = friendId,
            amount = 500.0,
            direction = TransactionDirection.LENT
        )

        repository.recordRepayment(txId, 300.0)
        var tx = repository.getTransactionById(txId)!!
        assertEquals(300.0, tx.effectivePaidAmount, 0.001)
        assertEquals(200.0, tx.effectiveRemainingAmount, 0.001)

        repository.markTransactionAsPaid(txId)
        tx = repository.getTransactionById(txId)!!
        assertEquals(TransactionStatus.CONFIRMED, tx.status)
        assertEquals(500.0, tx.effectivePaidAmount, 0.001)
        assertEquals(0.0, tx.effectiveRemainingAmount, 0.001)
    }

    /**
     * TEST K: Concurrent repayments safety / idempotency guard
     * Multiple repayments cannot exceed remaining balance.
     */
    @Test
    fun testK_concurrentRepaymentsCannotOverpay() = runTest {
        val friendId = repository.insertFriend("Karan")
        val txId = repository.addTransaction(
            friendId = friendId,
            amount = 500.0,
            direction = TransactionDirection.LENT
        )

        // Record initial partial repayment of 300 (remaining 200)
        val res1 = repository.recordRepayment(txId, 300.0)
        assertTrue(res1 is RepaymentResult.Success)

        // Attempt two repayments of 150 each (total 300, which exceeds remaining 200)
        val res2 = repository.recordRepayment(txId, 150.0)
        assertTrue(res2 is RepaymentResult.Success)

        // Second repayment is rejected because 150 > remaining (50)
        val res3 = repository.recordRepayment(txId, 150.0)
        assertTrue(res3 is RepaymentResult.Error)

        val tx = repository.getTransactionById(txId)!!
        assertEquals(450.0, tx.effectivePaidAmount, 0.001)
        assertEquals(50.0, tx.effectiveRemainingAmount, 0.001)
        assertEquals(TransactionStatus.OPEN, tx.status)
    }

    /**
     * TEST L: ViewModel UI state correctly reflects partial repayments
     * Verifies netBalance, openTransactionsCount, and bulk settlement eligibility
     * update in real time with partial repayments.
     */
    @Test
    fun testL_viewModelDerivesOpenCountAndRemainingAccurately() = runTest {
        val friendId = repository.insertFriend("Ravi")
        val txId = repository.addTransaction(
            friendId = friendId,
            amount = 500.0,
            direction = TransactionDirection.LENT
        )
        repository.recordRepayment(txId, 200.0)

        val vm = FriendDetailViewModel(repository, friendId)
        val state = vm.uiState.first { it.timeline.isNotEmpty() }
        assertEquals(300.0, state.netBalance, 0.001)
        assertEquals(1, state.openTransactionsCount)
        assertEquals(com.example.ui.viewmodel.BulkSettlementEligibility.SAME_DIRECTION_LENT, state.bulkSettlementEligibility)
        assertEquals(300.0, state.bulkSettlementTotalRemaining, 0.001)
    }
}
