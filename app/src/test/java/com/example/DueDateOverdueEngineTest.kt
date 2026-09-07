package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.TransactionDirection
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionStatus
import com.example.data.model.dueInfo
import com.example.data.model.effectivePaidAmount
import com.example.data.model.effectiveRemainingAmount
import com.example.data.repository.PhittoosRepository
import com.example.data.repository.RepaymentResult
import com.example.domain.DueDateHelper
import com.example.domain.DueState
import com.example.ui.viewmodel.AddTransactionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
class DueDateOverdueEngineTest {

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
     * Helper to construct a timestamp on a specific calendar date and time.
     */
    private fun createCalendarMillis(
        year: Int,
        month: Int, // 1-12
        day: Int,
        hour: Int = 12,
        minute: Int = 0,
        second: Int = 0,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        return Calendar.getInstance(timeZone).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, second)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    // =========================================================================
    // SCENARIO A: No due date
    // OPEN transaction -> due state NONE
    // =========================================================================
    @Test
    fun testScenarioA_noDueDate_openTransaction_dueStateNone() = runTest {
        val now = createCalendarMillis(2026, 9, 10, 14, 30)
        val info = DueDateHelper.calculateDueState(
            dueDateMillis = null,
            status = TransactionStatus.OPEN,
            nowMillis = now
        )

        assertEquals(DueState.NONE, info.state)
        assertEquals(0, info.overdueDays)
        assertFalse(info.isActivelyOverdue)
        assertEquals("", info.formattedStatus)
    }

    // =========================================================================
    // SCENARIO B: Future date
    // Expected: UPCOMING
    // =========================================================================
    @Test
    fun testScenarioB_futureDate_upcoming() = runTest {
        val now = createCalendarMillis(2026, 9, 10, 10, 0)
        val futureDueDate = createCalendarMillis(2026, 9, 15, 12, 0)

        val info = DueDateHelper.calculateDueState(
            dueDateMillis = futureDueDate,
            status = TransactionStatus.OPEN,
            nowMillis = now
        )

        assertEquals(DueState.UPCOMING, info.state)
        assertEquals(0, info.overdueDays)
        assertFalse(info.isActivelyOverdue)
        assertTrue(info.formattedStatus.startsWith("Due "))
        assertTrue(info.formattedStatus.contains("15 Sep"))
    }

    // =========================================================================
    // SCENARIO C: Due today
    // Expected: DUE_TODAY, not overdue
    // =========================================================================
    @Test
    fun testScenarioC_dueToday_dueTodayNotOverdue() = runTest {
        // Today is 10 Sep 2026 at 09:15 AM
        val nowMorning = createCalendarMillis(2026, 9, 10, 9, 15)
        // Due date is set for 10 Sep 2026 (e.g. noon or end of day)
        val dueDate = createCalendarMillis(2026, 9, 10, 23, 59)

        val infoMorning = DueDateHelper.calculateDueState(
            dueDateMillis = dueDate,
            status = TransactionStatus.OPEN,
            nowMillis = nowMorning
        )

        assertEquals(DueState.DUE_TODAY, infoMorning.state)
        assertEquals(0, infoMorning.overdueDays)
        assertFalse(infoMorning.isActivelyOverdue)
        assertEquals("Due today", infoMorning.formattedStatus)

        // Even later in the day at 23:45 PM, it remains DUE_TODAY and NOT overdue
        val nowNight = createCalendarMillis(2026, 9, 10, 23, 45)
        val infoNight = DueDateHelper.calculateDueState(
            dueDateMillis = dueDate,
            status = TransactionStatus.OPEN,
            nowMillis = nowNight
        )
        assertEquals(DueState.DUE_TODAY, infoNight.state)
        assertFalse(infoNight.isActivelyOverdue)
        assertEquals("Due today", infoNight.formattedStatus)
    }

    // =========================================================================
    // SCENARIO D: One day overdue
    // Expected: OVERDUE, 1 day
    // =========================================================================
    @Test
    fun testScenarioD_oneDayOverdue_overdueOneDay() = runTest {
        // Due date was 5 Sep
        val dueDate = createCalendarMillis(2026, 9, 5, 12, 0)
        // Today is 6 Sep
        val now = createCalendarMillis(2026, 9, 6, 8, 30)

        val info = DueDateHelper.calculateDueState(
            dueDateMillis = dueDate,
            status = TransactionStatus.OPEN,
            nowMillis = now
        )

        assertEquals(DueState.OVERDUE, info.state)
        assertEquals(1, info.overdueDays)
        assertTrue(info.isActivelyOverdue)
        assertEquals("Overdue by 1 day", info.formattedStatus)
    }

    // =========================================================================
    // SCENARIO E: Multiple days overdue
    // Expected: correct calendar difference
    // =========================================================================
    @Test
    fun testScenarioE_multipleDaysOverdue_correctCalendarDifference() = runTest {
        val dueDate = createCalendarMillis(2026, 9, 5, 12, 0)

        // Today is 9 Sep (4 calendar days after 5 Sep)
        val now4Days = createCalendarMillis(2026, 9, 9, 15, 0)
        val info4Days = DueDateHelper.calculateDueState(
            dueDateMillis = dueDate,
            status = TransactionStatus.OPEN,
            nowMillis = now4Days
        )
        assertEquals(DueState.OVERDUE, info4Days.state)
        assertEquals(4, info4Days.overdueDays)
        assertTrue(info4Days.isActivelyOverdue)
        assertEquals("Overdue by 4 days", info4Days.formattedStatus)

        // Today is 10 Sep (5 calendar days after 5 Sep)
        val now5Days = createCalendarMillis(2026, 9, 10, 10, 0)
        val info5Days = DueDateHelper.calculateDueState(
            dueDateMillis = dueDate,
            status = TransactionStatus.OPEN,
            nowMillis = now5Days
        )
        assertEquals(DueState.OVERDUE, info5Days.state)
        assertEquals(5, info5Days.overdueDays)
        assertTrue(info5Days.isActivelyOverdue)
        assertEquals("Overdue by 5 days", info5Days.formattedStatus)
    }

    // =========================================================================
    // SCENARIO F: Settled past-due transaction
    // Expected: not actively overdue
    // =========================================================================
    @Test
    fun testScenarioF_settledPastDueTransaction_notActivelyOverdue() = runTest {
        val dueDate = createCalendarMillis(2026, 9, 1, 12, 0)
        val now = createCalendarMillis(2026, 9, 6, 12, 0)

        val info = DueDateHelper.calculateDueState(
            dueDateMillis = dueDate,
            status = TransactionStatus.CONFIRMED,
            nowMillis = now
        )

        // Settled transactions should never be treated as currently overdue
        assertFalse(info.isActivelyOverdue)
        assertEquals(0, info.overdueDays)
        assertEquals(DueState.NONE, info.state)
        // Preserves historical due date record
        assertTrue(info.formattedStatus.startsWith("Due "))
        assertTrue(info.formattedStatus.contains("1 Sep"))
    }

    // =========================================================================
    // SCENARIO G: Partial repayment overdue
    // ₹500 original, ₹300 paid, ₹200 remaining
    // Expected: OPEN, remaining ₹200, overdue state preserved
    // =========================================================================
    @Test
    fun testScenarioG_partialRepaymentOverdue_openRemaining200OverduePreserved() = runTest {
        val friendId = repository.insertFriend("Akash")
        val pastDueDate = createCalendarMillis(2026, 9, 1, 12, 0)

        val txId = repository.addTransaction(
            friendId = friendId,
            amount = 500.0,
            direction = TransactionDirection.LENT,
            dueDate = pastDueDate
        )

        // Record partial repayment of ₹300
        val result = repository.recordRepayment(txId, 300.0)
        assertTrue(result is RepaymentResult.Success)

        val tx = repository.getTransactionById(txId)
        assertNotNull(tx)
        assertEquals(TransactionStatus.OPEN, tx!!.status)
        assertEquals(300.0, tx.effectivePaidAmount, 0.001)
        assertEquals(200.0, tx.effectiveRemainingAmount, 0.001)

        // Check overdue state with current date = 5 Sep
        val now = createCalendarMillis(2026, 9, 5, 12, 0)
        val dueInfo = DueDateHelper.calculateDueState(
            dueDateMillis = tx.dueDate,
            status = tx.status,
            nowMillis = now
        )

        assertEquals(DueState.OVERDUE, dueInfo.state)
        assertEquals(4, dueInfo.overdueDays)
        assertTrue(dueInfo.isActivelyOverdue)
        assertEquals("Overdue by 4 days", dueInfo.formattedStatus)

        // Repository dashboard count should only count remaining ₹200
        val totals = repository.dashboardTotals.first()
        assertEquals(200.0, totals.youWillGetBack, 0.001)
        assertEquals(200.0, totals.netPosition, 0.001)
    }

    // =========================================================================
    // SCENARIO H: Full repayment of overdue transaction
    // Expected: CONFIRMED, remaining ₹0, active overdue state removed
    // =========================================================================
    @Test
    fun testScenarioH_fullRepaymentOfOverdueTransaction_confirmedRemaining0ActiveOverdueRemoved() = runTest {
        val friendId = repository.insertFriend("Akash")
        val pastDueDate = createCalendarMillis(2026, 9, 1, 12, 0)

        val txId = repository.addTransaction(
            friendId = friendId,
            amount = 500.0,
            direction = TransactionDirection.LENT,
            dueDate = pastDueDate
        )

        // Pay full remaining amount (₹500)
        val result = repository.recordRepayment(txId, 500.0)
        assertTrue(result is RepaymentResult.Success)
        val success = result as RepaymentResult.Success
        assertTrue(success.isFullySettled)

        val tx = repository.getTransactionById(txId)
        assertNotNull(tx)
        assertEquals(TransactionStatus.CONFIRMED, tx!!.status)
        assertEquals(500.0, tx.effectivePaidAmount, 0.001)
        assertEquals(0.0, tx.effectiveRemainingAmount, 0.001)

        // With status CONFIRMED, active overdue state is immediately removed
        val now = createCalendarMillis(2026, 9, 6, 12, 0)
        val dueInfo = DueDateHelper.calculateDueState(
            dueDateMillis = tx.dueDate,
            status = tx.status,
            nowMillis = now
        )
        assertFalse(dueInfo.isActivelyOverdue)
        assertEquals(0, dueInfo.overdueDays)
        assertEquals(DueState.NONE, dueInfo.state)
    }

    // =========================================================================
    // SCENARIO I: Mixed directions
    // Two transactions with different due dates
    // Expected: independent due-state calculations
    // =========================================================================
    @Test
    fun testScenarioI_mixedDirections_independentDueStates() = runTest {
        val friendId = repository.insertFriend("Akash")
        val now = createCalendarMillis(2026, 9, 10, 12, 0)

        // Tx1: Lent ₹500, due 5 Sep (overdue by 5 days)
        val dueDateLent = createCalendarMillis(2026, 9, 5, 12, 0)
        val tx1Id = repository.addTransaction(
            friendId = friendId,
            amount = 500.0,
            direction = TransactionDirection.LENT,
            dueDate = dueDateLent
        )

        // Tx2: Borrowed ₹300, due 15 Sep (upcoming in 5 days)
        val dueDateBorrowed = createCalendarMillis(2026, 9, 15, 12, 0)
        val tx2Id = repository.addTransaction(
            friendId = friendId,
            amount = 300.0,
            direction = TransactionDirection.BORROWED,
            dueDate = dueDateBorrowed
        )

        val tx1 = repository.getTransactionById(tx1Id)!!
        val tx2 = repository.getTransactionById(tx2Id)!!

        val dueInfo1 = DueDateHelper.calculateDueState(tx1.dueDate, tx1.status, now)
        val dueInfo2 = DueDateHelper.calculateDueState(tx2.dueDate, tx2.status, now)

        // Verify independent states: Tx1 is overdue, Tx2 is upcoming
        assertEquals(DueState.OVERDUE, dueInfo1.state)
        assertEquals(5, dueInfo1.overdueDays)
        assertTrue(dueInfo1.isActivelyOverdue)

        assertEquals(DueState.UPCOMING, dueInfo2.state)
        assertEquals(0, dueInfo2.overdueDays)
        assertFalse(dueInfo2.isActivelyOverdue)

        // Friend net balance is ₹500 - ₹300 = ₹200, but overdue count is 1
        val friends = repository.friendsWithBalance.first()
        val akash = friends.first { it.friend.id == friendId }
        assertEquals(200.0, akash.netBalance, 0.001)
        assertEquals(1, akash.overdueTransactionsCount)
    }

    // =========================================================================
    // SCENARIO J: Past date validation on new transaction
    // Expected: invalid input rejected
    // =========================================================================
    @Test
    fun testScenarioJ_pastDateValidationOnNewTransaction_invalidInputRejected() = runTest {
        val now = createCalendarMillis(2026, 9, 10, 12, 0)
        val yesterday = createCalendarMillis(2026, 9, 9, 12, 0)
        val today = createCalendarMillis(2026, 9, 10, 8, 0)
        val tomorrow = createCalendarMillis(2026, 9, 11, 12, 0)

        // Direct helper checks
        assertTrue(DueDateHelper.isPastDate(yesterday, now))
        assertFalse(DueDateHelper.isPastDate(today, now))
        assertFalse(DueDateHelper.isPastDate(tomorrow, now))

        // ViewModel validation check
        val vm = AddTransactionViewModel(repository)
        val friendId = repository.insertFriend("Rohan")
        val friend = repository.getFriendById(friendId).first()!!
        vm.selectFriend(friend)
        vm.setAmount("250")

        // For ViewModel test, use timestamps relative to System.currentTimeMillis()
        val realPastDate = System.currentTimeMillis() - 86400000L * 2
        val realFutureDate = System.currentTimeMillis() + 86400000L * 2

        // User accidentally selects a past date
        vm.setDueDate(realPastDate)
        var state = vm.uiState.first { it.dueDate == realPastDate }
        assertEquals("Due date cannot be in the past.", state.errorMessage)

        // Attempt to save should fail and not insert transaction
        var savedSuccessToast: String? = null
        vm.saveTransaction { toast -> savedSuccessToast = toast }
        testDispatcher.scheduler.advanceUntilIdle()
        assertNull(savedSuccessToast)

        // User clears due date or picks a valid future date
        vm.setDueDate(realFutureDate)
        state = vm.uiState.first { it.dueDate == realFutureDate }
        assertNull(state.errorMessage)

        // Now saving succeeds
        vm.saveTransaction { toast -> savedSuccessToast = toast }
        var attempts = 0
        while (savedSuccessToast == null && attempts < 30) {
            testDispatcher.scheduler.advanceUntilIdle()
            Thread.sleep(30)
            testDispatcher.scheduler.advanceUntilIdle()
            attempts++
        }
        assertNotNull(savedSuccessToast)
    }

    // =========================================================================
    // SCENARIO K: Timezone/calendar boundary
    // Due date is today in local timezone
    // Expected: DUE_TODAY, not incorrectly overdue
    // =========================================================================
    @Test
    fun testScenarioK_timezoneCalendarBoundary_notIncorrectlyOverdue() = runTest {
        val kolkataTz = TimeZone.getTimeZone("Asia/Kolkata")
        val dueDate = createCalendarMillis(2026, 9, 7, 12, 0, 0, kolkataTz)

        // Test at 12:01 AM on 7 Sep (just after midnight)
        val nowEarly = createCalendarMillis(2026, 9, 7, 0, 1, 0, kolkataTz)
        val infoEarly = DueDateHelper.calculateDueState(
            dueDateMillis = dueDate,
            status = TransactionStatus.OPEN,
            nowMillis = nowEarly,
            timeZone = kolkataTz
        )
        assertEquals(DueState.DUE_TODAY, infoEarly.state)
        assertFalse(infoEarly.isActivelyOverdue)
        assertEquals("Due today", infoEarly.formattedStatus)

        // Test at 11:59 PM on 7 Sep
        val nowLate = createCalendarMillis(2026, 9, 7, 23, 59, 0, kolkataTz)
        val infoLate = DueDateHelper.calculateDueState(
            dueDateMillis = dueDate,
            status = TransactionStatus.OPEN,
            nowMillis = nowLate,
            timeZone = kolkataTz
        )
        assertEquals(DueState.DUE_TODAY, infoLate.state)
        assertFalse(infoLate.isActivelyOverdue)
        assertEquals("Due today", infoLate.formattedStatus)

        // Only on 8 Sep at 12:01 AM does it transition to OVERDUE by 1 day
        val nowNextDay = createCalendarMillis(2026, 9, 8, 0, 1, 0, kolkataTz)
        val infoNextDay = DueDateHelper.calculateDueState(
            dueDateMillis = dueDate,
            status = TransactionStatus.OPEN,
            nowMillis = nowNextDay,
            timeZone = kolkataTz
        )
        assertEquals(DueState.OVERDUE, infoNextDay.state)
        assertEquals(1, infoNextDay.overdueDays)
        assertTrue(infoNextDay.isActivelyOverdue)
        assertEquals("Overdue by 1 day", infoNextDay.formattedStatus)
    }

    // =========================================================================
    // SCENARIO L: No accounting regression
    // Changing/adding/removing a due date must NOT change:
    // You'll get back, You owe, Net, Friend balance
    // =========================================================================
    @Test
    fun testScenarioL_noAccountingRegression_dueDateDoesNotAffectMoney() = runTest {
        val friendId = repository.insertFriend("Priya")

        // 1. Transaction without due date
        val txId = repository.addTransaction(
            friendId = friendId,
            amount = 1000.0,
            direction = TransactionDirection.LENT,
            dueDate = null
        )

        var totals = repository.dashboardTotals.first()
        var friendBalance = repository.getFriendWithBalance(friendId).first()!!

        assertEquals(1000.0, totals.youWillGetBack, 0.001)
        assertEquals(0.0, totals.youOwe, 0.001)
        assertEquals(1000.0, totals.netPosition, 0.001)
        assertEquals(1000.0, friendBalance.netBalance, 0.001)

        // 2. Update with future due date
        val futureDate = createCalendarMillis(2026, 10, 1)
        var tx = repository.getTransactionById(txId)!!
        db.transactionDao().updateTransaction(tx.copy(dueDate = futureDate))

        totals = repository.dashboardTotals.first()
        friendBalance = repository.getFriendWithBalance(friendId).first()!!
        assertEquals(1000.0, totals.youWillGetBack, 0.001)
        assertEquals(0.0, totals.youOwe, 0.001)
        assertEquals(1000.0, totals.netPosition, 0.001)
        assertEquals(1000.0, friendBalance.netBalance, 0.001)

        // 3. Update with overdue due date
        val pastDate = createCalendarMillis(2026, 8, 1)
        tx = repository.getTransactionById(txId)!!
        db.transactionDao().updateTransaction(tx.copy(dueDate = pastDate))

        totals = repository.dashboardTotals.first()
        friendBalance = repository.getFriendWithBalance(friendId).first()!!
        assertEquals(1000.0, totals.youWillGetBack, 0.001)
        assertEquals(0.0, totals.youOwe, 0.001)
        assertEquals(1000.0, totals.netPosition, 0.001)
        assertEquals(1000.0, friendBalance.netBalance, 0.001)

        // 4. Reset back to null
        tx = repository.getTransactionById(txId)!!
        db.transactionDao().updateTransaction(tx.copy(dueDate = null))

        totals = repository.dashboardTotals.first()
        friendBalance = repository.getFriendWithBalance(friendId).first()!!
        assertEquals(1000.0, totals.youWillGetBack, 0.001)
        assertEquals(0.0, totals.youOwe, 0.001)
        assertEquals(1000.0, totals.netPosition, 0.001)
        assertEquals(1000.0, friendBalance.netBalance, 0.001)
    }

    // =========================================================================
    // ADDITIONAL TEST: Query open transactions with due dates for reminders
    // =========================================================================
    @Test
    fun testOpenTransactionsWithDueDate_queryWorksCorrectly() = runTest {
        val friendId = repository.insertFriend("Vikram")

        val date1 = createCalendarMillis(2026, 9, 15)
        val date2 = createCalendarMillis(2026, 9, 20)

        // Tx with due date
        repository.addTransaction(
            friendId = friendId,
            amount = 300.0,
            direction = TransactionDirection.LENT,
            dueDate = date2
        )
        // Tx with earlier due date
        repository.addTransaction(
            friendId = friendId,
            amount = 150.0,
            direction = TransactionDirection.LENT,
            dueDate = date1
        )
        // Tx without due date
        repository.addTransaction(
            friendId = friendId,
            amount = 100.0,
            direction = TransactionDirection.LENT,
            dueDate = null
        )

        val openWithDueDate = repository.getOpenTransactionsWithDueDate()
        assertEquals(2, openWithDueDate.size)
        // Ordered by due date ASC
        assertEquals(date1, openWithDueDate[0].dueDate)
        assertEquals(date2, openWithDueDate[1].dueDate)
    }
}
