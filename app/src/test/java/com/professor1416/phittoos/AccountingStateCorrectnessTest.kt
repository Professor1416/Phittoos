package com.professor1416.phittoos

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.professor1416.phittoos.data.db.AppDatabase
import com.professor1416.phittoos.data.model.ActivityType
import com.professor1416.phittoos.data.model.ReminderStage
import com.professor1416.phittoos.data.model.TransactionDirection
import com.professor1416.phittoos.data.model.TransactionEntity
import com.professor1416.phittoos.data.model.TransactionStatus
import com.professor1416.phittoos.data.model.dueInfo
import com.professor1416.phittoos.data.model.effectivePaidAmount
import com.professor1416.phittoos.data.model.effectiveRemainingAmount
import com.professor1416.phittoos.data.preferences.UserPreferences
import com.professor1416.phittoos.data.repository.PhittoosRepository
import com.professor1416.phittoos.data.repository.RepaymentErrorReason
import com.professor1416.phittoos.data.repository.RepaymentResult
import com.professor1416.phittoos.domain.DueDateHelper
import com.professor1416.phittoos.domain.DueState
import com.professor1416.phittoos.export.PhittoosBackupManager
import com.professor1416.phittoos.reminder.SmartReminderEngine
import com.professor1416.phittoos.ui.util.Formatters
import com.professor1416.phittoos.ui.viewmodel.ActivityViewModel
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
import org.junit.Assert.assertNotNull
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
class AccountingStateCorrectnessTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: PhittoosRepository
    private lateinit var preferences: UserPreferences
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = PhittoosRepository(
            friendDao = db.friendDao(),
            transactionDao = db.transactionDao(),
            activityDao = db.activityDao(),
            database = db
        )
        preferences = UserPreferences(context).apply {
            userName = "Test User"
            hasCompletedOnboarding = true
            remindersEnabled = true
        }
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    // ========================================================================
    // BUG-01: SETTLED TRANSACTION OVERDUE/DUE STATE
    // ========================================================================

    @Test
    fun bug01_openTransactionWithPastDueDateIsOverdue() = runTest {
        val now = System.currentTimeMillis()
        val pastDueDate = now - (3 * 86400000L) // 3 days ago

        val info = DueDateHelper.calculateDueState(
            dueDateMillis = pastDueDate,
            status = TransactionStatus.OPEN,
            nowMillis = now
        )

        assertEquals(DueState.OVERDUE, info.state)
        assertTrue(info.isActivelyOverdue)
        assertTrue(info.overdueDays >= 3)
    }

    @Test
    fun bug01_settlingSameTransactionClearsOverdueImmediately() = runTest {
        val friendId = repository.insertFriend("Alice")
        val now = System.currentTimeMillis()
        val pastDueDate = now - (5 * 86400000L)

        val txId = repository.addTransaction(
            friendId = friendId,
            amount = 1000.0,
            direction = TransactionDirection.LENT,
            dueDate = pastDueDate
        )

        // Verify it was overdue
        val openTxs = repository.getTransactionsForFriend(friendId).first()
        assertEquals(1, openTxs.size)
        assertTrue(openTxs[0].dueInfo.isActivelyOverdue)

        val needsAttBefore = repository.needsAttentionItems.first()
        assertEquals(1, needsAttBefore.size)

        // Settle the transaction
        repository.markTransactionAsPaid(txId)

        // Verify that after settlement, it is no longer overdue and disappeared from Needs Attention
        val settledTxs = repository.getTransactionsForFriend(friendId).first()
        assertEquals(1, settledTxs.size)
        assertEquals(TransactionStatus.CONFIRMED, settledTxs[0].status)
        assertFalse(settledTxs[0].dueInfo.isActivelyOverdue)
        assertEquals(DueState.NONE, settledTxs[0].dueInfo.state)
        assertEquals(0, settledTxs[0].dueInfo.overdueDays)

        val needsAttAfter = repository.needsAttentionItems.first()
        assertTrue(needsAttAfter.isEmpty())
    }

    @Test
    fun bug01_openTransactionDueTodayBecomesDueTodayAndClearsOnSettlement() = runTest {
        val tz = TimeZone.getDefault()
        val cal = Calendar.getInstance(tz).apply {
            set(Calendar.HOUR_OF_DAY, 15)
            set(Calendar.MINUTE, 0)
        }
        val todayMillis = cal.timeInMillis

        val openInfo = DueDateHelper.calculateDueState(
            dueDateMillis = todayMillis,
            status = TransactionStatus.OPEN,
            nowMillis = todayMillis,
            timeZone = tz
        )
        assertEquals(DueState.DUE_TODAY, openInfo.state)
        assertFalse(openInfo.isActivelyOverdue)

        val settledInfo = DueDateHelper.calculateDueState(
            dueDateMillis = todayMillis,
            status = TransactionStatus.CONFIRMED,
            nowMillis = todayMillis,
            timeZone = tz
        )
        assertEquals(DueState.NONE, settledInfo.state)
        assertFalse(settledInfo.isActivelyOverdue)
    }

    @Test
    fun bug01_settledTransactionWithPastDueDateExcludedFromOverdueCounts() = runTest {
        val friendId = repository.insertFriend("Bob")
        val now = System.currentTimeMillis()
        val pastDueDate = now - (10 * 86400000L)

        val txId = repository.addTransaction(
            friendId = friendId,
            amount = 500.0,
            direction = TransactionDirection.LENT,
            dueDate = pastDueDate
        )

        var friendWithBal = repository.getFriendWithBalance(friendId).first()
        assertEquals(1, friendWithBal?.overdueTransactionsCount)

        repository.markTransactionAsPaid(txId)

        friendWithBal = repository.getFriendWithBalance(friendId).first()
        assertEquals(0, friendWithBal?.overdueTransactionsCount)
        assertEquals(0, friendWithBal?.openTransactionsCount)
    }

    @Test
    fun bug01_settledTransactionNeverEligibleForSmartReminders() = runTest {
        val friendId = repository.insertFriend("Charlie")
        val now = System.currentTimeMillis()
        val pastDueDate = now - (10 * 86400000L)

        val txId = repository.addTransaction(
            friendId = friendId,
            amount = 1000.0,
            direction = TransactionDirection.LENT,
            dueDate = pastDueDate
        )

        // Settle the transaction
        repository.markTransactionAsPaid(txId)

        var remindersSent = 0
        val summary = SmartReminderEngine.checkAndSendReminders(
            repository = repository,
            context = context,
            nowMillis = now,
            timeZone = TimeZone.getDefault(),
            notificationSender = { _, _, _, _, _ ->
                remindersSent++
                true
            }
        )

        assertEquals(0, summary.remindersSentCount)
        assertEquals(0, remindersSent)
    }

    // ========================================================================
    // BUG-02: OVERPAYMENT CTA / REPAYMENT VALIDATION
    // ========================================================================

    @Test
    fun bug02_repaymentAmountValidationRules() {
        val remaining = 600.0

        // Helper lambda matching UI validation
        val isValid: (Double?) -> Boolean = { pAmount ->
            pAmount != null && !pAmount.isNaN() && !pAmount.isInfinite() && pAmount > 0.0 && pAmount <= remaining + 0.0001
        }

        // Amount = 0 -> invalid
        assertFalse(isValid(0.0))

        // Negative -> invalid
        assertFalse(isValid(-10.0))

        // Exceeds remaining (700 > 600) -> invalid
        assertFalse(isValid(700.0))
        assertFalse(isValid(600.01))

        // Exact remaining -> valid
        assertTrue(isValid(600.0))

        // Less than remaining -> valid
        assertTrue(isValid(400.0))
        assertTrue(isValid(0.01))
    }

    @Test
    fun bug02_repositoryRejectsOverpaymentAndProtectsState() = runTest {
        val friendId = repository.insertFriend("David")
        val txId = repository.addTransaction(
            friendId = friendId,
            amount = 1000.0,
            direction = TransactionDirection.LENT
        )

        // Partial repayment of 400 -> remaining 600
        val partialRes = repository.recordRepayment(txId, 400.0)
        assertTrue(partialRes is RepaymentResult.Success)

        val txAfterPartial = repository.getTransactionById(txId)
        assertEquals(600.0, txAfterPartial!!.effectiveRemainingAmount, 0.001)

        // Attempt overpayment of 700
        val overpayRes = repository.recordRepayment(txId, 700.0)
        assertTrue(overpayRes is RepaymentResult.Error)
        assertEquals(
            RepaymentErrorReason.EXCEEDS_REMAINING,
            (overpayRes as RepaymentResult.Error).reason
        )

        // Verify transaction remaining unchanged at 600
        val txAfterFailed = repository.getTransactionById(txId)
        assertEquals(600.0, txAfterFailed!!.effectiveRemainingAmount, 0.001)
        assertEquals(400.0, txAfterFailed.effectivePaidAmount, 0.001)
        assertEquals(TransactionStatus.OPEN, txAfterFailed.status)

        // Exact remaining of 600 succeeds and completes settlement
        val exactRes = repository.recordRepayment(txId, 600.0)
        assertTrue(exactRes is RepaymentResult.Success)
        assertTrue((exactRes as RepaymentResult.Success).isFullySettled)

        val finalTx = repository.getTransactionById(txId)
        assertEquals(0.0, finalTx!!.effectiveRemainingAmount, 0.001)
        assertEquals(TransactionStatus.CONFIRMED, finalTx.status)
    }

    // ========================================================================
    // BUG-03: HOME / ACTIVITY AMOUNT CONSISTENCY
    // ========================================================================

    @Test
    fun bug03_newlyAddedTransactionShowsIdenticalAmountOnHomeAndActivity() = runTest {
        val friendId = repository.insertFriend("Eve")
        val amount = 155.0
        val note = "Recharge"

        val txId = repository.addTransaction(
            friendId = friendId,
            amount = amount,
            direction = TransactionDirection.BORROWED,
            note = note
        )

        // Sourced for Home screen:
        val recentForHome = repository.recentActivity.first()
        val homeItem = recentForHome.find { it.transaction.id == txId }
        assertNotNull(homeItem)
        val homeFormattedAmount = Formatters.formatCurrency(homeItem!!.transaction.amount)

        // Sourced for Activity screen:
        val activities = repository.activitiesWithFriend.first()
        val activityItem = activities.find { it.activity.transactionId == txId && it.activity.type == ActivityType.TRANSACTION_CREATED }
        assertNotNull(activityItem)
        val activityDisplay = ActivityViewModel.mapToDisplayItem(activityItem!!)

        // Both must show identical amount string: "₹155"
        assertEquals("₹155", homeFormattedAmount)
        assertEquals("₹155", Formatters.formatCurrency(activityItem.activity.amount!!))
        assertTrue(activityDisplay.eventDescription.formatArgs.contains("₹155"))
    }

    @Test
    fun bug03_notesDateFriendDoNotAlterAmountSource() = runTest {
        val friendId = repository.insertFriend("Frank")
        val txId = repository.addTransaction(
            friendId = friendId,
            amount = 1250.50,
            direction = TransactionDirection.LENT,
            note = "Dinner & Drinks on 17 Sep"
        )

        val tx = repository.getTransactionById(txId)
        val activities = repository.activitiesWithFriend.first()
        val act = activities.find { it.activity.transactionId == txId }

        assertEquals(tx!!.amount, act!!.activity.amount!!, 0.001)
        assertEquals(Formatters.formatCurrency(tx.amount), Formatters.formatCurrency(act.activity.amount!!))
    }

    @Test
    fun bug03_repaymentActivityAmountIsRepaymentAmountNotTotal() = runTest {
        val friendId = repository.insertFriend("Grace")
        val txId = repository.addTransaction(
            friendId = friendId,
            amount = 1000.0,
            direction = TransactionDirection.LENT,
            note = "Trip"
        )

        repository.recordRepayment(txId, 350.0)

        val activities = repository.activitiesWithFriend.first()
        val repaymentAct = activities.find { it.activity.type == ActivityType.PARTIAL_REPAYMENT && it.activity.transactionId == txId }
        assertNotNull(repaymentAct)
        assertEquals(350.0, repaymentAct!!.activity.amount!!, 0.001)

        val display = ActivityViewModel.mapToDisplayItem(repaymentAct)
        assertTrue(display.eventDescription.formatArgs.contains("₹350"))
    }

    @Test
    fun bug03_finalRepaymentActivityAmountIsCorrect() = runTest {
        val friendId = repository.insertFriend("Hank")
        val txId = repository.addTransaction(
            friendId = friendId,
            amount = 1000.0,
            direction = TransactionDirection.LENT
        )

        repository.recordRepayment(txId, 400.0)
        repository.recordRepayment(txId, 600.0)

        val activities = repository.activitiesWithFriend.first()
        val finalAct = activities.find { it.activity.type == ActivityType.SETTLED && it.activity.transactionId == txId }
        assertNotNull(finalAct)
        assertEquals(600.0, finalAct!!.activity.amount!!, 0.001)

        val display = ActivityViewModel.mapToDisplayItem(finalAct)
        assertTrue(display.eventDescription.formatArgs.contains("₹600"))
    }

    @Test
    fun bug03_restoreBackupPreservesActivityAndTransactionAmountsConsistently() = runTest {
        val friendId = repository.insertFriend("Ivy")
        val txId = repository.addTransaction(
            friendId = friendId,
            amount = 155.0,
            direction = TransactionDirection.BORROWED,
            note = "Recharge"
        )

        // Create backup JSON
        val backupJson = PhittoosBackupManager.exportBackup(repository, preferences)

        // Clear and restore
        repository.clearAllData()
        assertEquals(0, repository.getAllFriendsForExport().size)

        val restoreRes = PhittoosBackupManager.restoreBackup(backupJson, repository, preferences, db)
        assertTrue(restoreRes.isSuccess)

        // Verify restored transaction and activity have the exact same amount
        val restoredTxs = repository.getAllTransactionsForExport()
        assertEquals(1, restoredTxs.size)
        assertEquals(155.0, restoredTxs[0].amount, 0.001)

        val restoredActs = repository.allActivitiesList()
        val createdAct = restoredActs.find { it.type == ActivityType.TRANSACTION_CREATED }
        assertNotNull(createdAct)
        assertEquals(155.0, createdAct!!.amount!!, 0.001)
    }
}
