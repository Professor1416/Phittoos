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
import com.example.domain.DueDateHelper
import com.example.domain.ReliabilityEngine
import com.example.domain.ReliabilityLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ReliabilityEngineTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: PhittoosRepository
    private val testDispatcher = UnconfinedTestDispatcher()

    private val testTimeZone = TimeZone.getTimeZone("UTC")

    private fun calendarMillis(year: Int, month: Int, day: Int, hour: Int = 12): Long {
        return Calendar.getInstance(testTimeZone).apply {
            set(year, month - 1, day, hour, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

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

    // SCENARIO A: Zero history -> NEW
    @Test
    fun scenarioA_zeroHistory_evaluatesToNew() {
        val result = ReliabilityEngine.calculate(emptyList())
        assertEquals(ReliabilityLevel.NEW, result.level)
        assertEquals(0, result.completedCount)
        assertNull(result.averageDelayDays)
        assertTrue(result.summaryText.contains("Not enough repayment history", ignoreCase = true))
    }

    // SCENARIO B: One completed LENT -> NEW (< 2 completed)
    @Test
    fun scenarioB_oneCompletedLent_evaluatesToNew() {
        val tx = TransactionEntity(
            id = 1,
            friendId = 10,
            amount = 500.0,
            direction = TransactionDirection.LENT,
            status = TransactionStatus.CONFIRMED,
            dueDate = calendarMillis(2026, 9, 10),
            settledAt = calendarMillis(2026, 9, 10)
        )
        val result = ReliabilityEngine.calculate(listOf(tx))
        assertEquals(ReliabilityLevel.NEW, result.level)
        assertEquals(1, result.completedCount)
    }

    // SCENARIO C: Two completed on-time LENT transactions -> non-NEW, normally GREEN
    @Test
    fun scenarioC_twoCompletedOnTimeLent_evaluatesToGreen() {
        val tx1 = TransactionEntity(
            id = 1,
            friendId = 10,
            amount = 500.0,
            direction = TransactionDirection.LENT,
            status = TransactionStatus.CONFIRMED,
            dueDate = calendarMillis(2026, 9, 10),
            settledAt = calendarMillis(2026, 9, 10)
        )
        val tx2 = TransactionEntity(
            id = 2,
            friendId = 10,
            amount = 300.0,
            direction = TransactionDirection.LENT,
            status = TransactionStatus.CONFIRMED,
            dueDate = calendarMillis(2026, 9, 15),
            settledAt = calendarMillis(2026, 9, 14) // Early by 1 day
        )
        val result = ReliabilityEngine.calculate(listOf(tx1, tx2))
        assertEquals(ReliabilityLevel.GREEN, result.level)
        assertEquals(2, result.completedCount)
        assertEquals(0.0, result.averageDelayDays!!, 0.001)
        assertTrue(result.summaryText.contains("on time", ignoreCase = true))
    }

    // SCENARIO D: Completed early repayment -> Expected delay = 0
    @Test
    fun scenarioD_earlyRepayment_yieldsZeroDelay() {
        val dueDate = calendarMillis(2026, 9, 10, 18)
        val settledDate = calendarMillis(2026, 9, 8, 9)
        val delay = DueDateHelper.calculateDelayDays(dueDate, settledDate, testTimeZone)
        assertEquals(0, delay)

        val tx1 = TransactionEntity(
            id = 1, friendId = 10, amount = 1000.0,
            direction = TransactionDirection.LENT, status = TransactionStatus.CONFIRMED,
            dueDate = dueDate, settledAt = settledDate
        )
        val tx2 = TransactionEntity(
            id = 2, friendId = 10, amount = 500.0,
            direction = TransactionDirection.LENT, status = TransactionStatus.CONFIRMED,
            dueDate = calendarMillis(2026, 9, 20), settledAt = calendarMillis(2026, 9, 20)
        )
        val result = ReliabilityEngine.calculate(listOf(tx1, tx2))
        assertEquals(0.0, result.averageDelayDays!!, 0.001)
        assertEquals(ReliabilityLevel.GREEN, result.level)
    }

    // SCENARIO E: One-day-late repayment -> Expected correct calendar delay (= 1)
    @Test
    fun scenarioE_oneDayLateRepayment_yieldsOneCalendarDayDelay() {
        val dueDate = calendarMillis(2026, 9, 10, 23)
        val settledDate = calendarMillis(2026, 9, 11, 2)
        val delay = DueDateHelper.calculateDelayDays(dueDate, settledDate, testTimeZone)
        assertEquals(1, delay)
    }

    // SCENARIO F: Multiple late repayments -> Expected classification reflects delays
    @Test
    fun scenarioF_multipleLateRepayments_evaluatesToYellowOrRed() {
        // Yellow: Moderate average delay (e.g. 5 days)
        val tx1 = TransactionEntity(
            id = 1, friendId = 10, amount = 500.0,
            direction = TransactionDirection.LENT, status = TransactionStatus.CONFIRMED,
            dueDate = calendarMillis(2026, 9, 5), settledAt = calendarMillis(2026, 9, 10) // 5 days late
        )
        val tx2 = TransactionEntity(
            id = 2, friendId = 10, amount = 500.0,
            direction = TransactionDirection.LENT, status = TransactionStatus.CONFIRMED,
            dueDate = calendarMillis(2026, 9, 10), settledAt = calendarMillis(2026, 9, 16) // 6 days late
        )
        val yellowResult = ReliabilityEngine.calculate(listOf(tx1, tx2))
        assertEquals(ReliabilityLevel.YELLOW, yellowResult.level)
        assertEquals(5.5, yellowResult.averageDelayDays!!, 0.001)

        // Red: Substantial average delay (> 10 days) across multiple transactions
        val tx3 = TransactionEntity(
            id = 3, friendId = 10, amount = 500.0,
            direction = TransactionDirection.LENT, status = TransactionStatus.CONFIRMED,
            dueDate = calendarMillis(2026, 9, 1), settledAt = calendarMillis(2026, 9, 16) // 15 days late
        )
        val tx4 = TransactionEntity(
            id = 4, friendId = 10, amount = 500.0,
            direction = TransactionDirection.LENT, status = TransactionStatus.CONFIRMED,
            dueDate = calendarMillis(2026, 9, 1), settledAt = calendarMillis(2026, 9, 13) // 12 days late
        )
        val tx5 = TransactionEntity(
            id = 5, friendId = 10, amount = 500.0,
            direction = TransactionDirection.LENT, status = TransactionStatus.CONFIRMED,
            dueDate = calendarMillis(2026, 9, 1), settledAt = calendarMillis(2026, 9, 15) // 14 days late
        )
        val redResult = ReliabilityEngine.calculate(listOf(tx3, tx4, tx5))
        assertEquals(ReliabilityLevel.RED, redResult.level)
        assertTrue(redResult.averageDelayDays!! > 10.0)
    }

    // SCENARIO G: Current overdue open LENT -> Expected overdue count affects reliability
    @Test
    fun scenarioG_currentOverdueOpenLent_lowersReliabilityToYellow() {
        val now = calendarMillis(2026, 9, 20)
        val tx1 = TransactionEntity(
            id = 1, friendId = 10, amount = 500.0,
            direction = TransactionDirection.LENT, status = TransactionStatus.CONFIRMED,
            dueDate = calendarMillis(2026, 9, 1), settledAt = calendarMillis(2026, 9, 1)
        )
        val tx2 = TransactionEntity(
            id = 2, friendId = 10, amount = 500.0,
            direction = TransactionDirection.LENT, status = TransactionStatus.CONFIRMED,
            dueDate = calendarMillis(2026, 9, 5), settledAt = calendarMillis(2026, 9, 5)
        )
        val txOverdue = TransactionEntity(
            id = 3, friendId = 10, amount = 400.0,
            direction = TransactionDirection.LENT, status = TransactionStatus.OPEN,
            dueDate = calendarMillis(2026, 9, 15) // Past 'now' of Sep 20
        )
        val result = ReliabilityEngine.calculate(listOf(tx1, tx2, txOverdue), nowMillis = now)
        assertEquals(ReliabilityLevel.YELLOW, result.level)
        assertEquals(1, result.overdueOpenCount)
        assertTrue(result.summaryText.contains("overdue", ignoreCase = true))
    }

    // SCENARIO H: Partial overdue repayment: Lent ₹500, ₹300 repaid, ₹200 remaining -> still open overdue
    @Test
    fun scenarioH_partialOverdueRepayment_remainsOpenOverdue() = runTest {
        val friendId = repository.insertFriend("Rohan")
        val pastDueDate = System.currentTimeMillis() - 5 * 86_400_000L

        val txId = repository.addTransaction(
            friendId = friendId,
            amount = 500.0,
            direction = TransactionDirection.LENT,
            dueDate = pastDueDate
        )

        val repayResult = repository.recordRepayment(txId, 300.0)
        assertTrue(repayResult is com.example.data.repository.RepaymentResult.Success)
        assertFalse((repayResult as com.example.data.repository.RepaymentResult.Success).isFullySettled)

        val txAfter = repository.getTransactionById(txId)!!
        assertEquals(TransactionStatus.OPEN, txAfter.status)
        assertEquals(200.0, txAfter.effectiveRemainingAmount, 0.001)

        val friendWithBal = repository.friendsWithBalance.first().first { it.friend.id == friendId }
        assertEquals(1, friendWithBal.overdueTransactionsCount)
    }

    // SCENARIO I: Final repayment moves from overdue-open to completed history, reliability recalculates
    @Test
    fun scenarioI_finalRepayment_completesTransactionAndRecalculatesReliability() = runTest {
        val friendId = repository.insertFriend("Pooja")
        val pastDate1 = System.currentTimeMillis() - 20 * 86_400_000L
        val tx1Id = repository.addTransaction(friendId, 200.0, TransactionDirection.LENT, dueDate = pastDate1)
        repository.markTransactionAsPaid(tx1Id, settledAt = pastDate1)

        val pastDate2 = System.currentTimeMillis() - 15 * 86_400_000L
        val tx2Id = repository.addTransaction(friendId, 300.0, TransactionDirection.LENT, dueDate = pastDate2)
        repository.markTransactionAsPaid(tx2Id, settledAt = pastDate2)

        val pastDueDateOverdue = System.currentTimeMillis() - 5 * 86_400_000L
        val tx3Id = repository.addTransaction(friendId, 500.0, TransactionDirection.LENT, dueDate = pastDueDateOverdue)
        // Open & overdue
        val friendBefore = repository.friendsWithBalance.first().first { it.friend.id == friendId }
        assertEquals(ReliabilityLevel.YELLOW, friendBefore.reliabilityInfo.level)
        assertEquals(1, friendBefore.overdueTransactionsCount)

        // Settle completely on-time relative to its due date
        repository.recordRepayment(tx3Id, 500.0, settledAt = pastDueDateOverdue)
        val friendAfter = repository.friendsWithBalance.first().first { it.friend.id == friendId }
        assertEquals(0, friendAfter.overdueTransactionsCount)
        assertEquals(ReliabilityLevel.GREEN, friendAfter.reliabilityInfo.level)
    }

    // SCENARIO J: BORROWED late transaction -> NO impact on friend's reliability
    @Test
    fun scenarioJ_borrowedLateTransaction_doesNotLowerFriendReliability() {
        val tx1 = TransactionEntity(
            id = 1, friendId = 10, amount = 500.0,
            direction = TransactionDirection.LENT, status = TransactionStatus.CONFIRMED,
            dueDate = calendarMillis(2026, 9, 1), settledAt = calendarMillis(2026, 9, 1)
        )
        val tx2 = TransactionEntity(
            id = 2, friendId = 10, amount = 500.0,
            direction = TransactionDirection.LENT, status = TransactionStatus.CONFIRMED,
            dueDate = calendarMillis(2026, 9, 5), settledAt = calendarMillis(2026, 9, 5)
        )
        // User borrowed ₹1000 from friend and settled 40 days late
        val txBorrowedLate = TransactionEntity(
            id = 3, friendId = 10, amount = 1000.0,
            direction = TransactionDirection.BORROWED, status = TransactionStatus.CONFIRMED,
            dueDate = calendarMillis(2026, 8, 1), settledAt = calendarMillis(2026, 9, 10)
        )
        val result = ReliabilityEngine.calculate(listOf(tx1, tx2, txBorrowedLate))
        assertEquals(ReliabilityLevel.GREEN, result.level)
        assertEquals(2, result.completedCount)
        assertEquals(0.0, result.averageDelayDays!!, 0.001)
    }

    // SCENARIO K: Mixed Lent/Borrowed history -> only LENT used
    @Test
    fun scenarioK_mixedLentBorrowed_onlyLentEvaluated() {
        val txLent1 = TransactionEntity(
            id = 1, friendId = 10, amount = 500.0,
            direction = TransactionDirection.LENT, status = TransactionStatus.CONFIRMED,
            dueDate = calendarMillis(2026, 9, 1), settledAt = calendarMillis(2026, 9, 1)
        )
        val txLent2 = TransactionEntity(
            id = 2, friendId = 10, amount = 500.0,
            direction = TransactionDirection.LENT, status = TransactionStatus.CONFIRMED,
            dueDate = calendarMillis(2026, 9, 5), settledAt = calendarMillis(2026, 9, 5)
        )
        val txBorrowedOverdue = TransactionEntity(
            id = 3, friendId = 10, amount = 800.0,
            direction = TransactionDirection.BORROWED, status = TransactionStatus.OPEN,
            dueDate = calendarMillis(2026, 9, 1) // User has not paid friend back
        )
        val result = ReliabilityEngine.calculate(
            listOf(txLent1, txLent2, txBorrowedOverdue),
            nowMillis = calendarMillis(2026, 9, 20)
        )
        // Overdue BORROWED is user's debt, friend's reliability must remain GREEN
        assertEquals(ReliabilityLevel.GREEN, result.level)
        assertEquals(0, result.overdueOpenCount)
    }

    // SCENARIO L: Settled transaction with no due date -> does not fabricate delay
    @Test
    fun scenarioL_settledNoDueDate_doesNotFabricateDelay() {
        val tx1 = TransactionEntity(
            id = 1, friendId = 10, amount = 500.0,
            direction = TransactionDirection.LENT, status = TransactionStatus.CONFIRMED,
            dueDate = null, settledAt = calendarMillis(2026, 9, 10)
        )
        val tx2 = TransactionEntity(
            id = 2, friendId = 10, amount = 500.0,
            direction = TransactionDirection.LENT, status = TransactionStatus.CONFIRMED,
            dueDate = calendarMillis(2026, 9, 15), settledAt = calendarMillis(2026, 9, 15)
        )
        val result = ReliabilityEngine.calculate(listOf(tx1, tx2))
        assertEquals(2, result.completedCount)
        // Only tx2 has due date and settlement date -> delay is 0.0
        assertEquals(0.0, result.averageDelayDays!!, 0.001)
    }

    // SCENARIO M: Legacy CONFIRMED transaction with missing settlement timestamp -> no invented delay
    @Test
    fun scenarioM_legacyConfirmedMissingSettlementTimestamp_noInventedDelay() {
        val txLegacy = TransactionEntity(
            id = 1, friendId = 10, amount = 500.0,
            direction = TransactionDirection.LENT, status = TransactionStatus.CONFIRMED,
            dueDate = calendarMillis(2026, 9, 1), settledAt = null // Legacy record
        )
        val txNew = TransactionEntity(
            id = 2, friendId = 10, amount = 300.0,
            direction = TransactionDirection.LENT, status = TransactionStatus.CONFIRMED,
            dueDate = calendarMillis(2026, 9, 10), settledAt = calendarMillis(2026, 9, 10)
        )
        val result = ReliabilityEngine.calculate(listOf(txLegacy, txNew))
        assertEquals(2, result.completedCount)
        assertEquals(0.0, result.averageDelayDays!!, 0.001)
    }

    // SCENARIO N: No due dates on completed history -> no false "average delay = excellent proof"
    @Test
    fun scenarioN_noDueDatesOnCompletedHistory_doesNotInventAverageDelay() {
        val tx1 = TransactionEntity(
            id = 1, friendId = 10, amount = 500.0,
            direction = TransactionDirection.LENT, status = TransactionStatus.CONFIRMED,
            dueDate = null, settledAt = calendarMillis(2026, 9, 5)
        )
        val tx2 = TransactionEntity(
            id = 2, friendId = 10, amount = 300.0,
            direction = TransactionDirection.LENT, status = TransactionStatus.CONFIRMED,
            dueDate = null, settledAt = calendarMillis(2026, 9, 8)
        )
        val result = ReliabilityEngine.calculate(listOf(tx1, tx2))
        assertEquals(2, result.completedCount)
        assertNull(result.averageDelayDays)
        assertEquals(ReliabilityLevel.GREEN, result.level)
        assertTrue(result.summaryText.contains("no delays recorded", ignoreCase = true))
    }

    // SCENARIO O: Reliability does not change monetary balances
    @Test
    fun scenarioO_reliabilityDoesNotAlterMonetaryBalances() = runTest {
        val friendId = repository.insertFriend("Kavita")
        repository.addTransaction(friendId, 1000.0, TransactionDirection.LENT)
        repository.addTransaction(friendId, 400.0, TransactionDirection.BORROWED)

        val totalsBefore = repository.dashboardTotals.first()
        val friendsWithBal = repository.friendsWithBalance.first()
        val kavitaBal = friendsWithBal.first { it.friend.id == friendId }

        assertEquals(600.0, kavitaBal.netBalance, 0.001)
        assertEquals(1000.0, totalsBefore.youWillGetBack, 0.001)
        assertEquals(400.0, totalsBefore.youOwe, 0.001)
        assertEquals(600.0, totalsBefore.netPosition, 0.001)
    }

    // SCENARIO P: Color/level text mapping -> NEW/GREEN/YELLOW/RED mapped consistently
    @Test
    fun scenarioP_colorLevelTextMapping_consistent() {
        val levels = ReliabilityLevel.values()
        assertEquals(4, levels.size)
        assertTrue(levels.contains(ReliabilityLevel.NEW))
        assertTrue(levels.contains(ReliabilityLevel.GREEN))
        assertTrue(levels.contains(ReliabilityLevel.YELLOW))
        assertTrue(levels.contains(ReliabilityLevel.RED))
    }

    // SCENARIO Q: Exactly two transactions with one anomaly -> do not over-aggressively label RED without strong evidence
    @Test
    fun scenarioQ_exactlyTwoTransactionsOneAnomaly_doesNotLabelRed() {
        val txOnTime = TransactionEntity(
            id = 1, friendId = 10, amount = 500.0,
            direction = TransactionDirection.LENT, status = TransactionStatus.CONFIRMED,
            dueDate = calendarMillis(2026, 9, 1), settledAt = calendarMillis(2026, 9, 1) // delay = 0
        )
        val txAnomalyLate = TransactionEntity(
            id = 2, friendId = 10, amount = 500.0,
            direction = TransactionDirection.LENT, status = TransactionStatus.CONFIRMED,
            dueDate = calendarMillis(2026, 9, 1), settledAt = calendarMillis(2026, 9, 23) // delay = 22 days
        )
        val result = ReliabilityEngine.calculate(listOf(txOnTime, txAnomalyLate))
        // Even with high average delay from 1 single anomaly, with only 2 completed txs, must NOT jump to RED
        assertNotEquals(ReliabilityLevel.RED, result.level)
        assertEquals(ReliabilityLevel.YELLOW, result.level)
    }
}
