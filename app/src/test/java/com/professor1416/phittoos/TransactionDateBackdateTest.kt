package com.professor1416.phittoos

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.professor1416.phittoos.data.dao.ActivityDao
import com.professor1416.phittoos.data.dao.FriendDao
import com.professor1416.phittoos.data.dao.TransactionDao
import com.professor1416.phittoos.data.db.AppDatabase
import com.professor1416.phittoos.data.model.ActivityType
import com.professor1416.phittoos.data.model.TransactionDirection
import com.professor1416.phittoos.data.model.TransactionStatus
import com.professor1416.phittoos.data.model.dueInfo
import com.professor1416.phittoos.data.model.effectivePaidAmount
import com.professor1416.phittoos.data.model.effectiveRemainingAmount
import com.professor1416.phittoos.data.preferences.UserPreferences
import com.professor1416.phittoos.data.repository.DeleteErrorReason
import com.professor1416.phittoos.data.repository.EditErrorReason
import com.professor1416.phittoos.data.repository.EditTransactionResult
import com.professor1416.phittoos.data.repository.PhittoosRepository
import com.professor1416.phittoos.domain.DueDateHelper
import com.professor1416.phittoos.domain.DueState
import com.professor1416.phittoos.export.PhittoosBackupManager
import com.professor1416.phittoos.export.PhittoosCsvExporter
import com.professor1416.phittoos.ui.util.Formatters
import com.professor1416.phittoos.ui.viewmodel.AddTransactionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class TransactionDateBackdateTest {

    private lateinit var db: AppDatabase
    private lateinit var friendDao: FriendDao
    private lateinit var transactionDao: TransactionDao
    private lateinit var activityDao: ActivityDao
    private lateinit var repository: PhittoosRepository
    private lateinit var userPreferences: UserPreferences
    private lateinit var context: Context
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        friendDao = db.friendDao()
        transactionDao = db.transactionDao()
        activityDao = db.activityDao()
        repository = PhittoosRepository(friendDao, transactionDao, activityDao, db)
        userPreferences = UserPreferences(context)
        userPreferences.clearAll()
        userPreferences.userName = "Test User"
        userPreferences.hasCompletedOnboarding = true
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun newTransaction_defaultsToToday() = runTest {
        val friendId = repository.insertFriend("Aarav")
        val vm = AddTransactionViewModel(repository, initialFriendId = friendId)
        val state = vm.uiState.first { it.selectedFriend != null }

        assertTrue(DueDateHelper.isToday(state.transactionDate))
        assertFalse(DueDateHelper.isFutureDate(state.transactionDate))
    }

    @Test
    fun newTransaction_pastDateSavesCorrectly_andMatchesCreatedDate() = runTest {
        val friendId = repository.insertFriend("Bhavin")

        // 5 days ago normalized to midday
        val fiveDaysAgo = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -5)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val txId = repository.addTransaction(
            friendId = friendId,
            amount = 2000.0,
            direction = TransactionDirection.LENT,
            note = "Dinner 5 days ago",
            dueDate = null,
            createdDate = fiveDaysAgo
        )

        val tx = repository.getTransactionById(txId)
        assertNotNull(tx)
        assertEquals(fiveDaysAgo, tx!!.createdDate)
        assertEquals(2000.0, tx.amount, 0.001)
        assertEquals("Dinner 5 days ago", tx.note)

        // Activity record must have matching createdAt
        val activities = activityDao.getAllActivitiesList()
        val createdAct = activities.find { it.transactionId == txId && it.type == ActivityType.TRANSACTION_CREATED }
        assertNotNull(createdAct)
        assertEquals(fiveDaysAgo, createdAct!!.createdAt)
    }

    @Test
    fun newTransaction_futureDateRejectedByViewModel() = runTest {
        val friendId = repository.insertFriend("Chetan")
        val vm = AddTransactionViewModel(repository, initialFriendId = friendId)
        vm.uiState.first { it.selectedFriend != null }

        // Tomorrow
        val tomorrow = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        vm.setTransactionDate(tomorrow)
        val stateAfterFuture = vm.uiState.first { it.errorMessage != null }
        assertEquals("Transaction date cannot be in the future.", stateAfterFuture.errorMessage)

        var saved = false
        vm.saveTransaction { saved = true }
        assertFalse(saved)
        val stateAfterSave = vm.uiState.first { it.errorMessage != null }
        assertEquals("Transaction date cannot be in the future.", stateAfterSave.errorMessage)
    }

    @Test
    fun newTransaction_futureDateRejectedByRepository() = runTest {
        val friendId = repository.insertFriend("Deepak")
        val tomorrow = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 2)
        }.timeInMillis

        var exceptionThrown = false
        try {
            repository.addTransaction(
                friendId = friendId,
                amount = 500.0,
                direction = TransactionDirection.LENT,
                createdDate = tomorrow
            )
        } catch (e: IllegalArgumentException) {
            exceptionThrown = true
            assertEquals("Transaction date cannot be in the future.", e.message)
        }
        assertTrue(exceptionThrown)
    }

    @Test
    fun newTransaction_amountAndDirectionUnaffectedByDate() = runTest {
        val friendId = repository.insertFriend("Eshaan")
        val pastDate = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -10)
            set(Calendar.HOUR_OF_DAY, 12)
        }.timeInMillis

        val txId = repository.addTransaction(
            friendId = friendId,
            amount = 1500.0,
            direction = TransactionDirection.BORROWED,
            createdDate = pastDate
        )

        val tx = repository.getTransactionById(txId)
        assertNotNull(tx)
        assertEquals(1500.0, tx!!.amount, 0.001)
        assertEquals(TransactionDirection.BORROWED, tx.direction)
        assertEquals(1500.0, tx.effectiveRemainingAmount, 0.001)
        assertEquals(0.0, tx.effectivePaidAmount, 0.001)
        assertEquals(TransactionStatus.OPEN, tx.status)

        // Friend balance
        val friends = repository.friendsWithBalance.first()
        val friendBalance = friends.find { it.friend.id == friendId }
        assertNotNull(friendBalance)
        assertEquals(-1500.0, friendBalance!!.netBalance, 0.001)
    }

    @Test
    fun editTransaction_eligibleOpenUntouched_dateCanChange() = runTest {
        val friendId = repository.insertFriend("Farhan")
        val origDate = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 12)
        }.timeInMillis

        val txId = repository.addTransaction(
            friendId = friendId,
            amount = 800.0,
            direction = TransactionDirection.LENT,
            createdDate = origDate
        )

        val newPastDate = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -7)
            set(Calendar.HOUR_OF_DAY, 12)
        }.timeInMillis

        val editResult = repository.updateOpenUnpaidTransaction(
            transactionId = txId,
            amount = 800.0,
            direction = TransactionDirection.LENT,
            createdDate = newPastDate
        )

        assertTrue(editResult is EditTransactionResult.Success)
        val updatedTx = (editResult as EditTransactionResult.Success).updatedTransaction
        assertEquals(txId, updatedTx.id)
        assertEquals(newPastDate, updatedTx.createdDate)

        // Activity record also updated
        val activities = activityDao.getAllActivitiesList()
        val createdAct = activities.find { it.transactionId == txId && it.type == ActivityType.TRANSACTION_CREATED }
        assertNotNull(createdAct)
        assertEquals(newPastDate, createdAct!!.createdAt)
    }

    @Test
    fun editTransaction_idRemainsSame_andBalanceMathematicallyIdentical() = runTest {
        val friendId = repository.insertFriend("Girish")
        val origDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -2) }.timeInMillis
        val txId = repository.addTransaction(
            friendId = friendId,
            amount = 1200.0,
            direction = TransactionDirection.LENT,
            createdDate = origDate
        )

        val newDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -4) }.timeInMillis
        val res = repository.updateOpenUnpaidTransaction(
            transactionId = txId,
            amount = 1200.0,
            direction = TransactionDirection.LENT,
            createdDate = newDate
        )
        assertTrue(res is EditTransactionResult.Success)

        val txAfter = repository.getTransactionById(txId)
        assertNotNull(txAfter)
        assertEquals(txId, txAfter!!.id)
        assertEquals(1200.0, txAfter.amount, 0.001)

        val friends = repository.friendsWithBalance.first()
        val friend = friends.find { it.friend.id == friendId }
        assertEquals(1200.0, friend?.netBalance ?: 0.0, 0.001)
    }

    @Test
    fun editTransaction_futureDateRejectedByRepositoryAndViewModel() = runTest {
        val friendId = repository.insertFriend("Harish")
        val txId = repository.addTransaction(
            friendId = friendId,
            amount = 300.0,
            direction = TransactionDirection.LENT
        )

        val futureDate = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 3)
            set(Calendar.HOUR_OF_DAY, 12)
        }.timeInMillis

        // Direct repository attempt
        val res = repository.updateOpenUnpaidTransaction(
            transactionId = txId,
            amount = 300.0,
            direction = TransactionDirection.LENT,
            createdDate = futureDate
        )
        assertTrue(res is EditTransactionResult.Error)
        assertEquals(EditErrorReason.INVALID_TRANSACTION_DATE, (res as EditTransactionResult.Error).reason)

        // Via ViewModel
        val vm = AddTransactionViewModel(repository, initialFriendId = friendId, transactionIdToEdit = txId)
        vm.uiState.first { it.amount == "300" }

        vm.setTransactionDate(futureDate)
        val stateAfterFuture = vm.uiState.first { it.errorMessage != null }
        assertEquals("Transaction date cannot be in the future.", stateAfterFuture.errorMessage)
    }

    @Test
    fun editTransaction_partiallyRepaid_dateCannotBeDirectlyEdited() = runTest {
        val friendId = repository.insertFriend("Irfan")
        val txId = repository.addTransaction(
            friendId = friendId,
            amount = 1000.0,
            direction = TransactionDirection.LENT
        )

        repository.recordRepayment(txId, 300.0)

        val newDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -3) }.timeInMillis
        val res = repository.updateOpenUnpaidTransaction(
            transactionId = txId,
            amount = 1000.0,
            direction = TransactionDirection.LENT,
            createdDate = newDate
        )

        assertTrue(res is EditTransactionResult.Error)
        assertEquals(EditErrorReason.HAS_REPAYMENTS, (res as EditTransactionResult.Error).reason)
    }

    @Test
    fun editTransaction_settled_dateCannotBeDirectlyEdited() = runTest {
        val friendId = repository.insertFriend("Jagdish")
        val txId = repository.addTransaction(
            friendId = friendId,
            amount = 500.0,
            direction = TransactionDirection.LENT
        )

        repository.markTransactionAsPaid(txId)

        val newDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -5) }.timeInMillis
        val res = repository.updateOpenUnpaidTransaction(
            transactionId = txId,
            amount = 500.0,
            direction = TransactionDirection.LENT,
            createdDate = newDate
        )

        assertTrue(res is EditTransactionResult.Error)
        assertEquals(EditErrorReason.NOT_OPEN, (res as EditTransactionResult.Error).reason)
    }

    @Test
    fun history_backdatedTransactionSortsChronologicallyInTimeline() = runTest {
        val friendId = repository.insertFriend("Karan")

        val oct1 = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 1, 12, 0, 0)
        }.timeInMillis
        val oct3 = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 3, 12, 0, 0)
        }.timeInMillis
        val oct7 = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 7, 12, 0, 0)
        }.timeInMillis

        // Add 1 Oct, 3 Oct, 7 Oct
        repository.addTransaction(friendId, 500.0, TransactionDirection.LENT, "Oct 1", createdDate = oct1)
        repository.addTransaction(friendId, 800.0, TransactionDirection.LENT, "Oct 3", createdDate = oct3)
        repository.addTransaction(friendId, 200.0, TransactionDirection.LENT, "Oct 7", createdDate = oct7)

        // Today add backdated 2 Oct
        val oct2 = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 2, 12, 0, 0)
        }.timeInMillis
        repository.addTransaction(friendId, 1000.0, TransactionDirection.LENT, "Oct 2 Backdated", createdDate = oct2)

        // Fetch friend transactions (ordered by created_date DESC)
        val txs = transactionDao.getTransactionsForFriend(friendId).first()
        assertEquals(4, txs.size)
        // Descending order: Oct 7, Oct 3, Oct 2, Oct 1
        assertEquals("Oct 7", txs[0].note)
        assertEquals("Oct 3", txs[1].note)
        assertEquals("Oct 2 Backdated", txs[2].note)
        assertEquals("Oct 1", txs[3].note)
    }

    @Test
    fun history_activityChronologyConsistent_andRepaymentSettlementTimestampsNotBackdated() = runTest {
        val friendId = repository.insertFriend("Lalit")

        val threeDaysAgo = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -3)
            set(Calendar.HOUR_OF_DAY, 12)
        }.timeInMillis

        val txId = repository.addTransaction(
            friendId = friendId,
            amount = 1000.0,
            direction = TransactionDirection.LENT,
            createdDate = threeDaysAgo
        )

        // Partial repayment happens now
        val now = System.currentTimeMillis()
        repository.recordRepayment(txId, 400.0, settledAt = now)

        val activities = activityDao.getAllActivitiesList()
        val createdAct = activities.find { it.type == ActivityType.TRANSACTION_CREATED }
        val repayAct = activities.find { it.type == ActivityType.PARTIAL_REPAYMENT }

        assertNotNull(createdAct)
        assertNotNull(repayAct)
        // Created activity reflects backdated transaction date
        assertEquals(threeDaysAgo, createdAct!!.createdAt)
        // Repayment activity retains actual current repayment timestamp
        assertEquals(now, repayAct!!.createdAt)
    }

    @Test
    fun dueDate_remainsIndependentOfTransactionDate() = runTest {
        val friendId = repository.insertFriend("Manish")
        val fiveDaysAgo = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -5)
            set(Calendar.HOUR_OF_DAY, 12)
        }.timeInMillis

        val tomorrow = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 12)
        }.timeInMillis

        val txId = repository.addTransaction(
            friendId = friendId,
            amount = 1000.0,
            direction = TransactionDirection.LENT,
            dueDate = tomorrow,
            createdDate = fiveDaysAgo
        )

        val tx = repository.getTransactionById(txId)
        assertNotNull(tx)
        assertEquals(fiveDaysAgo, tx!!.createdDate)
        assertEquals(tomorrow, tx.dueDate)
        assertEquals(DueState.UPCOMING, tx.dueInfo.state)
        assertFalse(tx.dueInfo.isActivelyOverdue)
    }

    @Test
    fun backupRestore_backdatedDateSurvivesJsonRoundTrip() = runTest {
        val friendId = repository.insertFriend("Naveen")
        val sep20 = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 20, 12, 0, 0)
        }.timeInMillis

        val txId = repository.addTransaction(
            friendId = friendId,
            amount = 2500.0,
            direction = TransactionDirection.LENT,
            note = "Backdated trip",
            createdDate = sep20
        )

        val backupJson = PhittoosBackupManager.exportBackup(repository, userPreferences)

        // Wipe DB and restore
        repository.clearAllData()
        assertEquals(0, repository.getAllFriendsForExport().size)

        val restoreResult = PhittoosBackupManager.restoreBackup(backupJson, repository, userPreferences, db)
        assertTrue(restoreResult.isSuccess)

        val restoredTxs = repository.getAllTransactionsForExport()
        assertEquals(1, restoredTxs.size)
        assertEquals(sep20, restoredTxs[0].createdDate)
        assertEquals(2500.0, restoredTxs[0].amount, 0.001)
        assertEquals("Backdated trip", restoredTxs[0].note)
    }

    @Test
    fun csvExport_reflectsSelectedBackdatedDate() = runTest {
        val friendId = repository.insertFriend("Omkar")
        val tz = TimeZone.getTimeZone("Asia/Kolkata")
        val oct2 = Calendar.getInstance(tz).apply {
            set(2026, Calendar.OCTOBER, 2, 12, 0, 0)
        }.timeInMillis

        val txId = repository.addTransaction(
            friendId = friendId,
            amount = 999.0,
            direction = TransactionDirection.LENT,
            note = "CSV Test",
            createdDate = oct2
        )

        val txs = repository.getAllTransactionsForExport()
        val friends = repository.getAllFriendsForExport()
        val csv = PhittoosCsvExporter.buildCsv(txs, friends, timeZone = tz)

        assertTrue(csv.contains("999"))
        assertTrue(csv.contains("2026-10-02"))
    }

    @Test
    fun timezoneSafety_indiaTimeZoneAndUtcMidnightSafe_noOneDayShift() = runTest {
        val indiaTz = TimeZone.getTimeZone("Asia/Kolkata") // UTC+05:30

        // Test date near midnight: 2 Oct 2026 00:05 AM in India
        val oct2Early = Calendar.getInstance(indiaTz).apply {
            set(2026, Calendar.OCTOBER, 2, 0, 5, 0)
        }.timeInMillis

        val normalized = DueDateHelper.normalizeToMidday(oct2Early, indiaTz)
        val normalizedCal = Calendar.getInstance(indiaTz).apply { timeInMillis = normalized }

        assertEquals(2026, normalizedCal.get(Calendar.YEAR))
        assertEquals(Calendar.OCTOBER, normalizedCal.get(Calendar.MONTH))
        assertEquals(2, normalizedCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(12, normalizedCal.get(Calendar.HOUR_OF_DAY))

        // Formatted string in India timezone must be 2 Oct
        val formatted = Formatters.formatFullDate(normalized)
        assertTrue(formatted.contains("02 Oct 2026") || formatted.contains("2 Oct 2026"))
    }

    @Test
    fun accountingInvariant_mixedDirectionZeroNetNotAllSettled_unaffectedByBackdate() = runTest {
        val friendId = repository.insertFriend("Prashant")
        val threeDaysAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -3) }.timeInMillis
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.timeInMillis

        // LENT 1000 3 days ago, BORROWED 1000 yesterday
        repository.addTransaction(friendId, 1000.0, TransactionDirection.LENT, createdDate = threeDaysAgo)
        repository.addTransaction(friendId, 1000.0, TransactionDirection.BORROWED, createdDate = yesterday)

        val friendWithBal = repository.getFriendWithBalance(friendId).first()
        assertNotNull(friendWithBal)
        assertEquals(0.0, friendWithBal!!.netBalance, 0.001)
        assertEquals(2, friendWithBal.openTransactionsCount)

        val totals = repository.dashboardTotals.first()
        assertEquals(1000.0, totals.youWillGetBack, 0.001)
        assertEquals(1000.0, totals.youOwe, 0.001)
        assertEquals(0.0, totals.netPosition, 0.001)
        assertEquals(2, totals.openTransactionsCount)
    }
}
