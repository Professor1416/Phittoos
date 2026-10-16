package com.professor1416.phittoos

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.professor1416.phittoos.data.db.AppDatabase
import com.professor1416.phittoos.data.model.ActivityEntity
import com.professor1416.phittoos.data.model.ActivityType
import com.professor1416.phittoos.data.model.Friend
import com.professor1416.phittoos.data.model.TransactionDirection
import com.professor1416.phittoos.data.model.TransactionEntity
import com.professor1416.phittoos.data.model.TransactionStatus
import com.professor1416.phittoos.data.model.effectiveRemainingAmount
import com.professor1416.phittoos.data.repository.PhittoosRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TransactionCreationValidationTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: PhittoosRepository
    private var friendId: Long = 0
    private val occurrence = 1_700_000_000_000L

    @Before
    fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = PhittoosRepository(db.friendDao(), db.transactionDao(), db.activityDao(), db)
        val existingFriend = repository.insertFriend("Existing synthetic friend")
        val lent = repository.addTransaction(existingFriend, 100.0, TransactionDirection.LENT, createdDate = occurrence)
        repository.recordRepayment(lent, 25.0, settledAt = occurrence + 1)
        val borrowed = repository.addTransaction(existingFriend, 50.0, TransactionDirection.BORROWED, createdDate = occurrence)
        repository.markTransactionAsPaid(borrowed, settledAt = occurrence + 2)
        friendId = repository.insertFriend("Target synthetic friend")
    }

    @After
    fun tearDown() {
        db.close()
    }

    private data class LedgerSnapshot(
        val friends: List<Friend>,
        val transactions: List<TransactionEntity>,
        val activities: List<ActivityEntity>
    )

    private suspend fun snapshot() = LedgerSnapshot(
        db.friendDao().getAllFriendsList(),
        db.transactionDao().getAllTransactionsOrdered(),
        db.activityDao().getAllActivitiesList()
    )

    private suspend fun assertRejectedWithoutSideEffects(vararg amounts: Double) {
        val before = snapshot()
        val totalsBefore = repository.dashboardTotals.first()
        for (direction in TransactionDirection.entries) {
            for (amount in amounts) {
                try {
                    repository.addTransaction(friendId, amount, direction, createdDate = occurrence)
                    fail("Expected rejection for $amount / $direction")
                } catch (_: IllegalArgumentException) {
                    // Rejection must precede any financial or activity persistence.
                }
                assertEquals(before, snapshot())
                assertEquals(totalsBefore, repository.dashboardTotals.first())
                assertEquals(0, repository.getFriendWithBalance(friendId).first()!!.openTransactionsCount)
            }
        }
    }

    @Test
    fun zeroAndNegativeZeroAreRejected() = runTest {
        assertRejectedWithoutSideEffects(0.0, -0.0)
    }

    @Test
    fun negativeAmountsAreRejected() = runTest {
        assertRejectedWithoutSideEffects(-0.01, -1.0, -100.25, -Double.MAX_VALUE)
    }

    @Test
    fun nanIsRejected() = runTest {
        assertRejectedWithoutSideEffects(Double.NaN)
    }

    @Test
    fun positiveInfinityAndOverflowAreRejected() = runTest {
        assertRejectedWithoutSideEffects(Double.POSITIVE_INFINITY, Double.MAX_VALUE * 2.0)
    }

    @Test
    fun negativeInfinityIsRejected() = runTest {
        assertRejectedWithoutSideEffects(Double.NEGATIVE_INFINITY)
    }

    @Test
    fun subPaiseAmountsAreRejectedRatherThanRounded() = runTest {
        assertRejectedWithoutSideEffects(0.001, 0.005, 1.001, 1.999, 100.0001, Double.MIN_VALUE)
    }

    @Test
    fun extraPrecisionNearValidPaiseIsNotSilentlyNormalized() = runTest {
        assertRejectedWithoutSideEffects(Math.nextUp(0.01), Math.nextDown(0.01), 0.1 + 0.2)
    }

    @Test
    fun validWholeAndPaiseAmountsPersistUnchangedForBothDirections() = runTest {
        val before = snapshot()
        val amounts = listOf(0.01, 0.1, 0.29, 0.58, 1.01, 10.99, 100.0, 1234.56)
        for (direction in TransactionDirection.entries) {
            for (amount in amounts) {
                val id = repository.addTransaction(friendId, amount, direction, createdDate = occurrence)
                val tx = repository.getTransactionById(id)!!
                assertEquals(amount, tx.amount, 0.0)
                assertEquals(direction, tx.direction)
                assertEquals(TransactionStatus.OPEN, tx.status)
                assertNull(tx.paidAmount)
                assertNull(tx.settledAt)
                assertEquals(amount, tx.effectiveRemainingAmount, 0.0)
                val event = db.activityDao().getAllActivitiesList().single { it.transactionId == id }
                assertEquals(ActivityType.TRANSACTION_CREATED, event.type)
                assertEquals(friendId, event.friendId)
                assertEquals(direction, event.direction)
                assertEquals(amount, event.amount!!, 0.0)
            }
        }
        val balance = repository.getFriendWithBalance(friendId).first()!!
        assertEquals(0.0, balance.netBalance, 0.0000001)
        assertEquals(amounts.size * 2, balance.openTransactionsCount)
        assertEquals(before.transactions, snapshot().transactions.filter { it.friendId != friendId })
        assertEquals(before.activities, snapshot().activities.filter { it.friendId != friendId })
    }

    @Test
    fun noUnapprovedMaximumIsImposedOnFiniteWholeAmounts() = runTest {
        val id = repository.addTransaction(friendId, Double.MAX_VALUE, TransactionDirection.LENT, createdDate = occurrence)
        assertEquals(Double.MAX_VALUE, repository.getTransactionById(id)!!.amount, 0.0)
        val event = db.activityDao().getAllActivitiesList().single { it.transactionId == id }
        assertEquals(Double.MAX_VALUE, event.amount!!, 0.0)
        // Range/aggregate-overflow policy is a separate decision, not invented here.
    }

    @Test
    fun validCreationPreservesDirectionBalancesAndRepaymentBehavior() = runTest {
        val lent = repository.addTransaction(friendId, 500.25, TransactionDirection.LENT, createdDate = occurrence)
        repository.addTransaction(friendId, 300.1, TransactionDirection.BORROWED, createdDate = occurrence)
        assertEquals(200.15, repository.getFriendWithBalance(friendId).first()!!.netBalance, 0.0000001)
        repository.recordRepayment(lent, 100.05, settledAt = occurrence + 10)
        assertEquals(400.2, repository.getTransactionById(lent)!!.effectiveRemainingAmount, 0.0000001)
        assertEquals(100.1, repository.getFriendWithBalance(friendId).first()!!.netBalance, 0.0000001)
        val totals = repository.dashboardTotals.first()
        assertEquals(475.2, totals.youWillGetBack, 0.0000001)
        assertEquals(300.1, totals.youOwe, 0.0000001)
        assertEquals(175.1, totals.netPosition, 0.0000001)
    }

    @Test
    fun validCreationPreservesOccurrenceDueDateAndTrimmedHistory() = runTest {
        val due = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 7) }.timeInMillis
        val id = repository.addTransaction(friendId, 12.34, TransactionDirection.BORROWED, "  Synthetic note  ", due, occurrence)
        val tx = repository.getTransactionById(id)!!
        assertEquals(occurrence, tx.createdDate)
        assertEquals(due, tx.dueDate)
        assertEquals("Synthetic note", tx.note)
        val event = db.activityDao().getAllActivitiesList().single { it.transactionId == id }
        assertEquals(occurrence, event.createdAt)
        assertEquals("Synthetic note", event.note)
    }

    @Test
    fun futureOccurrenceDateStillRejectsWithoutSideEffects() = runTest {
        val before = snapshot()
        val future = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 2) }.timeInMillis
        try {
            repository.addTransaction(friendId, 12.34, TransactionDirection.LENT, createdDate = future)
            fail("Expected future transaction date rejection")
        } catch (_: IllegalArgumentException) {
        }
        assertEquals(before, snapshot())
    }

    @Test
    fun creatingValidEntryDoesNotRewriteLegacyHigherPrecisionHistory() = runTest {
        val legacyId = db.transactionDao().insertTransaction(
            TransactionEntity(friendId = friendId, amount = 12.345, direction = TransactionDirection.LENT,
                createdDate = occurrence, status = TransactionStatus.CONFIRMED, paidAmount = 12.345,
                settledAt = occurrence + 1)
        )
        db.activityDao().insertActivity(
            ActivityEntity(type = ActivityType.SETTLED, friendId = friendId, transactionId = legacyId,
                amount = 12.345, direction = TransactionDirection.LENT, createdAt = occurrence + 1)
        )
        val before = snapshot()
        repository.addTransaction(friendId, 1.23, TransactionDirection.BORROWED, createdDate = occurrence)
        assertEquals(before.transactions, snapshot().transactions.filter { it.id in before.transactions.map { row -> row.id } })
        assertEquals(before.activities, snapshot().activities.filter { it.id in before.activities.map { row -> row.id } })
    }
}
