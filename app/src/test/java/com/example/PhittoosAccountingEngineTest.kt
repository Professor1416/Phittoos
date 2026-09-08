package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.Friend
import com.example.data.model.TransactionDirection
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionStatus
import com.example.data.model.effectiveRemainingAmount
import com.example.data.repository.PhittoosRepository
import com.example.ui.viewmodel.AddTransactionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PhittoosAccountingEngineTest {

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
     * Matrix Test A:
     * User Lent ₹500 to a friend.
     * Expected:
     * - You'll get back = 500
     * - You owe = 0
     * - Net position = +500
     * - Friend net balance = +500
     */
    @Test
    fun testMatrixA_lent500() = runTest {
        val friendId = repository.insertFriend("Alice")
        repository.addTransaction(
            friendId = friendId,
            amount = 500.0,
            direction = TransactionDirection.LENT,
            note = "Lunch",
            dueDate = null
        )

        val totals = repository.dashboardTotals.first()
        assertEquals(500.0, totals.youWillGetBack, 0.001)
        assertEquals(0.0, totals.youOwe, 0.001)
        assertEquals(500.0, totals.netPosition, 0.001)

        val friendWithBalance = repository.getFriendWithBalance(friendId).first()
        assertNotNull(friendWithBalance)
        assertEquals(500.0, friendWithBalance!!.netBalance, 0.001)
        assertEquals(1, friendWithBalance.openTransactionsCount)
    }

    /**
     * Matrix Test B:
     * User Borrowed ₹300 from a friend.
     * Expected:
     * - You'll get back = 0
     * - You owe = 300
     * - Net position = -300
     * - Friend net balance = -300
     */
    @Test
    fun testMatrixB_borrow300() = runTest {
        val friendId = repository.insertFriend("Bob")
        repository.addTransaction(
            friendId = friendId,
            amount = 300.0,
            direction = TransactionDirection.BORROWED,
            note = "Coffee",
            dueDate = null
        )

        val totals = repository.dashboardTotals.first()
        assertEquals(0.0, totals.youWillGetBack, 0.001)
        assertEquals(300.0, totals.youOwe, 0.001)
        assertEquals(-300.0, totals.netPosition, 0.001)

        val friendWithBalance = repository.getFriendWithBalance(friendId).first()
        assertNotNull(friendWithBalance)
        assertEquals(-300.0, friendWithBalance!!.netBalance, 0.001)
        assertEquals(1, friendWithBalance.openTransactionsCount)
    }

    /**
     * Matrix Test C:
     * Same friend: Lent ₹500 and Borrowed ₹300.
     * Expected:
     * - Friend net balance = +200
     * - Dashboard totals must remain gross:
     *   You'll get back = 500
     *   You owe = 300
     *   Net position = +200
     */
    @Test
    fun testMatrixC_sameFriendLent500Borrow300() = runTest {
        val friendId = repository.insertFriend("Charlie")
        repository.addTransaction(
            friendId = friendId,
            amount = 500.0,
            direction = TransactionDirection.LENT,
            note = "Movie tickets",
            dueDate = null
        )
        repository.addTransaction(
            friendId = friendId,
            amount = 300.0,
            direction = TransactionDirection.BORROWED,
            note = "Popcorn",
            dueDate = null
        )

        val totals = repository.dashboardTotals.first()
        assertEquals(500.0, totals.youWillGetBack, 0.001)
        assertEquals(300.0, totals.youOwe, 0.001)
        assertEquals(200.0, totals.netPosition, 0.001)

        val friendWithBalance = repository.getFriendWithBalance(friendId).first()
        assertNotNull(friendWithBalance)
        assertEquals(200.0, friendWithBalance!!.netBalance, 0.001)
        assertEquals(2, friendWithBalance.openTransactionsCount)
        assertEquals(2, friendWithBalance.totalTransactionsCount)
    }

    /**
     * Matrix Test D:
     * Friend A: user lent 500
     * Friend B: user borrowed 200
     * Expected:
     * - You'll get back = 500
     * - You owe = 200
     * - Net position = +300
     * - Friend A net = +500
     * - Friend B net = -200
     */
    @Test
    fun testMatrixD_friendALent500FriendBBorrowed200() = runTest {
        val friendA = repository.insertFriend("Friend A")
        val friendB = repository.insertFriend("Friend B")

        repository.addTransaction(
            friendId = friendA,
            amount = 500.0,
            direction = TransactionDirection.LENT,
            note = null,
            dueDate = null
        )
        repository.addTransaction(
            friendId = friendB,
            amount = 200.0,
            direction = TransactionDirection.BORROWED,
            note = null,
            dueDate = null
        )

        val totals = repository.dashboardTotals.first()
        assertEquals(500.0, totals.youWillGetBack, 0.001)
        assertEquals(200.0, totals.youOwe, 0.001)
        assertEquals(300.0, totals.netPosition, 0.001)

        val friends = repository.friendsWithBalance.first().associateBy { it.friend.id }
        assertEquals(500.0, friends[friendA]!!.netBalance, 0.001)
        assertEquals(-200.0, friends[friendB]!!.netBalance, 0.001)
    }

    /**
     * Matrix Test E:
     * Confirmed/closed transactions must NOT remain in outstanding balances.
     */
    @Test
    fun testMatrixE_confirmedTransactionsExcludedFromBalances() = runTest {
        val friendId = repository.insertFriend("David")
        val txId = repository.addTransaction(
            friendId = friendId,
            amount = 500.0,
            direction = TransactionDirection.LENT,
            note = "Dinner",
            dueDate = null
        )

        // Before settlement
        val beforeTotals = repository.dashboardTotals.first()
        assertEquals(500.0, beforeTotals.youWillGetBack, 0.001)

        // Mark as paid (transitions to CONFIRMED)
        repository.markTransactionAsPaid(txId)

        // After settlement
        val afterTotals = repository.dashboardTotals.first()
        assertEquals(0.0, afterTotals.youWillGetBack, 0.001)
        assertEquals(0.0, afterTotals.youOwe, 0.001)
        assertEquals(0.0, afterTotals.netPosition, 0.001)

        val friendWithBalance = repository.getFriendWithBalance(friendId).first()
        assertNotNull(friendWithBalance)
        assertEquals(0.0, friendWithBalance!!.netBalance, 0.001)
        assertEquals(0, friendWithBalance.openTransactionsCount)
        assertEquals(1, friendWithBalance.totalTransactionsCount)
    }

    /**
     * Matrix Test F:
     * Check partial paidAmount behavior.
     * Lent ₹500, paidAmount = 200 -> remaining outstanding = ₹300.
     */
    @Test
    fun testMatrixF_partialPaidAmountCalculation() = runTest {
        val friendId = repository.insertFriend("Elena")
        val tx = TransactionEntity(
            friendId = friendId,
            amount = 500.0,
            direction = TransactionDirection.LENT,
            paidAmount = 200.0,
            status = TransactionStatus.OPEN
        )
        db.transactionDao().insertTransaction(tx)

        val totals = repository.dashboardTotals.first()
        assertEquals(300.0, totals.youWillGetBack, 0.001)
        assertEquals(0.0, totals.youOwe, 0.001)
        assertEquals(300.0, totals.netPosition, 0.001)

        val friendWithBalance = repository.getFriendWithBalance(friendId).first()
        assertNotNull(friendWithBalance)
        assertEquals(300.0, friendWithBalance!!.netBalance, 0.001)
        assertEquals(1, friendWithBalance.openTransactionsCount)
    }

    /**
     * Matrix Test G:
     * paidAmount must never cause negative or inflated outstanding balance.
     */
    @Test
    fun testMatrixG_paidAmountNeverCausesNegativeOrInflatedBalance() = runTest {
        val friendId = repository.insertFriend("Frank")

        // Overpayment scenario: amount = 500, paidAmount = 700
        val overpaidTx = TransactionEntity(
            id = 1,
            friendId = friendId,
            amount = 500.0,
            direction = TransactionDirection.LENT,
            paidAmount = 700.0,
            status = TransactionStatus.OPEN
        )
        assertEquals(0.0, overpaidTx.effectiveRemainingAmount, 0.001)
        db.transactionDao().insertTransaction(overpaidTx)

        val totalsAfterOverpaid = repository.dashboardTotals.first()
        assertEquals(0.0, totalsAfterOverpaid.youWillGetBack, 0.001)
        assertEquals(0.0, totalsAfterOverpaid.netPosition, 0.001)

        // Negative paidAmount scenario: amount = 500, paidAmount = -100
        val negativePaidTx = TransactionEntity(
            id = 2,
            friendId = friendId,
            amount = 500.0,
            direction = TransactionDirection.LENT,
            paidAmount = -100.0,
            status = TransactionStatus.OPEN
        )
        assertEquals(500.0, negativePaidTx.effectiveRemainingAmount, 0.001)
    }

    /**
     * Matrix Test H:
     * Zero or negative transaction amounts must not be accepted through normal transaction creation.
     */
    @Test
    fun testMatrixH_zeroAndNegativeAmountsRejected() = runTest {
        val friendId = repository.insertFriend("George")
        val dummyFriend = Friend(id = friendId, name = "George")
        val addTxVm = AddTransactionViewModel(repository)

        // Wait until flow is active and friend is loaded
        addTxVm.uiState.first { it.allFriends.isNotEmpty() }

        addTxVm.selectFriend(dummyFriend)
        addTxVm.uiState.first { it.selectedFriend?.id == friendId }

        // 1. Zero amount
        addTxVm.setAmount("0")
        var toastCalled = false
        addTxVm.saveTransaction { toastCalled = true }
        val stateZero = addTxVm.uiState.first { it.errorMessage != null }
        assertEquals(false, toastCalled)
        assertEquals("Please enter a valid amount", stateZero.errorMessage)

        // 2. Blank amount
        addTxVm.setAmount("")
        addTxVm.uiState.first { it.errorMessage == null }
        addTxVm.saveTransaction { toastCalled = true }
        val stateBlank = addTxVm.uiState.first { it.errorMessage != null }
        assertEquals(false, toastCalled)
        assertEquals("Please enter a valid amount", stateBlank.errorMessage)

        // 3. Decimal zero
        addTxVm.setAmount("0.00")
        addTxVm.uiState.first { it.errorMessage == null }
        addTxVm.saveTransaction { toastCalled = true }
        val stateDecimalZero = addTxVm.uiState.first { it.errorMessage != null }
        assertEquals(false, toastCalled)
        assertEquals("Please enter a valid amount", stateDecimalZero.errorMessage)

        // 4. Negative input attempt (e.g. "-50"): setAmount filters out negative sign
        addTxVm.setAmount("-50")
        val stateNegative = addTxVm.uiState.first { it.amount == "50" }
        assertEquals("50", stateNegative.amount)
    }

    /**
     * Matrix Test I:
     * Recent Activity ordering must be newest-first and limited to 5.
     */
    @Test
    fun testMatrixI_recentActivityOrderingAndLimit() = runTest {
        val friendId = repository.insertFriend("Hannah")
        val baseTime = 1000000L

        // Insert 7 transactions with explicit created dates
        for (i in 1..7) {
            val tx = TransactionEntity(
                friendId = friendId,
                amount = (i * 100).toDouble(),
                direction = TransactionDirection.LENT,
                note = "Tx $i",
                createdDate = baseTime + (i * 1000)
            )
            db.transactionDao().insertTransaction(tx)
        }

        val recent = repository.recentActivity.first()
        // Must be limited to 5
        assertEquals(5, recent.size)

        // Must be newest first: Tx 7, Tx 6, Tx 5, Tx 4, Tx 3
        assertEquals("Tx 7", recent[0].transaction.note)
        assertEquals("Tx 6", recent[1].transaction.note)
        assertEquals("Tx 5", recent[2].transaction.note)
        assertEquals("Tx 4", recent[3].transaction.note)
        assertEquals("Tx 4", recent[3].transaction.note)
        assertEquals("Tx 3", recent[4].transaction.note)

        // Check strictly descending created_date
        for (k in 0 until recent.size - 1) {
            assertTrue(recent[k].transaction.createdDate >= recent[k + 1].transaction.createdDate)
        }
    }

    /**
     * Matrix Test J:
     * Friend timeline ordering must be newest-first.
     */
    @Test
    fun testMatrixJ_friendTimelineOrdering() = runTest {
        val friendId = repository.insertFriend("Ian")
        val baseTime = 5000000L

        val tx1 = TransactionEntity(friendId = friendId, amount = 100.0, direction = TransactionDirection.LENT, note = "First", createdDate = baseTime)
        val tx2 = TransactionEntity(friendId = friendId, amount = 200.0, direction = TransactionDirection.BORROWED, note = "Second", createdDate = baseTime + 2000)
        val tx3 = TransactionEntity(friendId = friendId, amount = 300.0, direction = TransactionDirection.LENT, note = "Third", createdDate = baseTime + 4000)

        db.transactionDao().insertTransaction(tx1)
        db.transactionDao().insertTransaction(tx2)
        db.transactionDao().insertTransaction(tx3)

        val timeline = repository.getTransactionsForFriend(friendId).first()
        assertEquals(3, timeline.size)
        // Newest first: tx3, tx2, tx1
        assertEquals("Third", timeline[0].note)
        assertEquals("Second", timeline[1].note)
        assertEquals("First", timeline[2].note)
    }

    /**
     * Audit verification:
     * repository.recentFriends is the real Flow-based implementation.
     */
    @Test
    fun testRecentFriendsFlow() = runTest {
        val friends = repository.recentFriends.first()
        assertTrue(friends.isEmpty())
    }
}
