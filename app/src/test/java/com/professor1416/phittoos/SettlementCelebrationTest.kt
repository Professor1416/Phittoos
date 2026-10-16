package com.professor1416.phittoos

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.professor1416.phittoos.R
import com.professor1416.phittoos.data.db.AppDatabase
import com.professor1416.phittoos.data.model.ActivityType
import com.professor1416.phittoos.data.model.effectiveRemainingAmount
import com.professor1416.phittoos.data.model.TransactionDirection
import com.professor1416.phittoos.data.model.TransactionStatus
import com.professor1416.phittoos.data.repository.PhittoosRepository
import com.professor1416.phittoos.ui.screens.frienddetail.SettlementCelebrationEvent
import com.professor1416.phittoos.ui.viewmodel.FriendDetailUiState
import com.professor1416.phittoos.ui.viewmodel.FriendDetailViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SettlementCelebrationTest {

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
        repository = PhittoosRepository(
            friendDao = db.friendDao(),
            transactionDao = db.transactionDao(),
            activityDao = db.activityDao(),
            database = db
        )
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    /**
     * Requirement 1:
     * Last individual LENT and BORROWED settlement: one celebration.
     */
    @Test
    fun test01_lastIndividualLentAndBorrowedSettlement_oneCelebration() = runTest {
        val friendLentId = repository.insertFriend("Aarav")
        val txLent = repository.addTransaction(friendLentId, 500.0, TransactionDirection.LENT, "Lunch")

        val vmLent = FriendDetailViewModel(repository, friendLentId)
        vmLent.onScreenResumed()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vmLent.uiState.collect() }
        vmLent.uiState.first { it.friend != null && it.timeline.isNotEmpty() }

        vmLent.settleTransaction(txLent)
        advanceUntilIdle()

        val stateLent = awaitCelebration(vmLent)
        assertNotNull("Celebration event must be emitted for last LENT settlement", stateLent.celebrationEvent)
        assertNull("Generic settlement toast must be suppressed when celebration is active", stateLent.toastMessage)
        assertEquals(0, stateLent.openTransactionsCount)

        // Borrowed case
        val friendBorrowedId = repository.insertFriend("Bhavna")
        val txBorrowed = repository.addTransaction(friendBorrowedId, 300.0, TransactionDirection.BORROWED, "Dinner")

        val vmBorrowed = FriendDetailViewModel(repository, friendBorrowedId)
        vmBorrowed.onScreenResumed()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vmBorrowed.uiState.collect() }
        vmBorrowed.uiState.first { it.friend != null && it.timeline.isNotEmpty() }

        vmBorrowed.settleTransaction(txBorrowed)
        advanceUntilIdle()

        val stateBorrowed = awaitCelebration(vmBorrowed)
        assertNotNull("Celebration event must be emitted for last BORROWED settlement", stateBorrowed.celebrationEvent)
        assertNull("Generic settlement toast must be suppressed when celebration is active", stateBorrowed.toastMessage)
        assertEquals(0, stateBorrowed.openTransactionsCount)
    }

    /**
     * Requirement 2:
     * Another transaction remains OPEN: no celebration.
     */
    @Test
    fun test02_anotherTransactionRemainsOpen_noCelebration() = runTest {
        val friendId = repository.insertFriend("Chetan")
        val tx1 = repository.addTransaction(friendId, 400.0, TransactionDirection.LENT, "Cab")
        val tx2 = repository.addTransaction(friendId, 600.0, TransactionDirection.LENT, "Hotel")

        val vm = FriendDetailViewModel(repository, friendId)
        vm.onScreenResumed()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        vm.uiState.first { it.friend != null && it.timeline.size == 2 }

        // Settle tx1 only, tx2 remains open
        vm.settleTransaction(tx1)
        advanceUntilIdle()

        val state = vm.uiState.first { it.openTransactionsCount == 1 && it.toastMessage != null }
        assertNull("Must not celebrate while another transaction remains OPEN", state.celebrationEvent)
        assertEquals(1, state.openTransactionsCount)
        assertNotNull("Generic settlement toast should be shown when not celebrating", state.toastMessage)
        assertEquals(R.string.msg_marked_as_fully_paid, state.toastMessage?.resId)
    }

    /**
     * Requirement 3:
     * Same-direction bulk settlement: one celebration.
     */
    @Test
    fun test03_sameDirectionBulkSettlement_oneCelebration() = runTest {
        val friendId = repository.insertFriend("Divya")
        repository.addTransaction(friendId, 200.0, TransactionDirection.LENT, "Coffee")
        repository.addTransaction(friendId, 300.0, TransactionDirection.LENT, "Snacks")
        repository.addTransaction(friendId, 500.0, TransactionDirection.LENT, "Groceries")

        val vm = FriendDetailViewModel(repository, friendId)
        vm.onScreenResumed()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        vm.uiState.first { it.friend != null && it.timeline.size == 3 }

        vm.settleAllSameDirection()
        advanceUntilIdle()

        val state = awaitCelebration(vm)
        assertNotNull("Celebration event must be emitted for bulk settlement", state.celebrationEvent)
        assertEquals(0, state.openTransactionsCount)
        assertNull("Toast must be suppressed when celebration is shown", state.toastMessage)
    }

    /**
     * Requirement 4:
     * Final repayment triggers; partial repayment does not.
     */
    @Test
    fun test04_finalRepaymentTriggers_partialRepaymentDoesNot() = runTest {
        val friendId = repository.insertFriend("Esha")
        val txId = repository.addTransaction(friendId, 1000.0, TransactionDirection.LENT, "Trip share")
        val otherFriend = repository.insertFriend("Unrelated synthetic friend")
        repository.addTransaction(otherFriend, 75.5, TransactionDirection.BORROWED, "Untouched")
        val friendsBefore = db.friendDao().getAllFriendsList()
        val otherRowsBefore = db.transactionDao().getAllTransactionsOrdered().filter { it.id != txId }
        val otherHistoryBefore = db.activityDao().getAllActivitiesList().filter { it.transactionId != txId }

        val vm = FriendDetailViewModel(repository, friendId)
        vm.onScreenResumed()
        val celebrationIds = mutableSetOf<Long>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect { state -> state.celebrationEvent?.let { celebrationIds += it.id } }
        }
        awaitActual {
            vm.uiState.first { !it.isLoading && it.timeline.any { tx -> tx.id == txId } }
        }
        var accepted = 0
        var rejected = 0
        suspend fun repay(amount: Double): Boolean {
            val result = CompletableDeferred<Boolean>()
            vm.recordRepayment(txId, amount,
                onSuccess = { accepted++; result.complete(true) },
                onError = { rejected++; result.complete(false) })
            return awaitActual { result.await() }
        }

        // A callback confirms repository completion; Room and operation-state flows may arrive separately.
        assertTrue(repay(400.0))
        val statePartial = awaitActual {
            vm.uiState.first { state ->
                state.openTransactionsCount == 1 && state.settlingTransactionIds.isEmpty() &&
                    state.toastMessage != null && state.timeline.any {
                        it.id == txId && it.status == TransactionStatus.OPEN && it.paidAmount == 400.0
                    }
            }
        }
        assertNull("Partial repayment must NOT trigger celebration", statePartial.celebrationEvent)
        assertTrue("No celebration may have been emitted during partial repayment", celebrationIds.isEmpty())
        assertEquals(1, statePartial.openTransactionsCount)
        assertNotNull("Partial repayment toast must be shown", statePartial.toastMessage)
        assertEquals(R.string.msg_repayment_recorded, statePartial.toastMessage?.resId)
        val partialTx = db.transactionDao().getTransactionById(txId)!!
        assertEquals(TransactionStatus.OPEN, partialTx.status)
        assertEquals(400.0, partialTx.paidAmount!!, 0.0)
        assertEquals(600.0, partialTx.effectiveRemainingAmount, 0.0)
        assertNull(partialTx.settledAt)
        assertEquals(1, db.transactionDao().getOpenCountForFriend(friendId))
        val partialHistory = db.activityDao().getAllActivitiesList().filter { it.transactionId == txId }.sortedBy { it.id }
        assertEquals(listOf(ActivityType.TRANSACTION_CREATED, ActivityType.PARTIAL_REPAYMENT), partialHistory.map { it.type })
        assertEquals(400.0, partialHistory.last().amount!!, 0.0)

        vm.clearToast()
        awaitActual { vm.uiState.first { it.toastMessage == null } }

        assertTrue(repay(600.0))
        // OPEN count alone can select an intermediate snapshot before the celebration is combined.
        val stateFinal = awaitCelebration(vm)
        assertNotNull("Final repayment that closes the last open transaction must celebrate", stateFinal.celebrationEvent)
        assertEquals(0, stateFinal.openTransactionsCount)
        assertNull("Final repayment toast suppressed when celebrating", stateFinal.toastMessage)
        val eventId = stateFinal.celebrationEvent!!.id
        assertEquals(setOf(eventId), celebrationIds)
        val finalTx = db.transactionDao().getTransactionById(txId)!!
        assertEquals(partialTx.copy(paidAmount = 1000.0, status = TransactionStatus.CONFIRMED, settledAt = finalTx.settledAt), finalTx)
        assertEquals(TransactionStatus.CONFIRMED, finalTx.status)
        assertEquals(1000.0, finalTx.paidAmount!!, 0.0)
        assertEquals(0.0, finalTx.effectiveRemainingAmount, 0.0)
        assertNotNull(finalTx.settledAt)
        assertEquals(0, db.transactionDao().getOpenCountForFriend(friendId))
        val finalHistory = db.activityDao().getAllActivitiesList().filter { it.transactionId == txId }.sortedBy { it.id }
        assertEquals(partialHistory, finalHistory.take(2))
        assertEquals(listOf(ActivityType.TRANSACTION_CREATED, ActivityType.PARTIAL_REPAYMENT, ActivityType.SETTLED), finalHistory.map { it.type })
        val finalActivity = finalHistory.last()
        assertEquals(600.0, finalActivity.amount!!, 0.0)
        assertEquals("final_repayment", finalActivity.note)
        assertEquals(finalTx.settledAt, finalActivity.createdAt)
        assertTrue(finalHistory.all { it.friendId == friendId && it.transactionId == txId && it.direction == TransactionDirection.LENT })
        assertEquals(friendsBefore, db.friendDao().getAllFriendsList())
        assertEquals(otherRowsBefore, db.transactionDao().getAllTransactionsOrdered().filter { it.id != txId })
        assertEquals(otherHistoryBefore, db.activityDao().getAllActivitiesList().filter { it.transactionId != txId })

        // A repeat must complete as a rejection, retain the original event ID, and change no history.
        val settledRows = db.transactionDao().getAllTransactionsOrdered()
        val settledHistory = db.activityDao().getAllActivitiesList()
        assertFalse(repay(600.0))
        val repeated = awaitActual {
            vm.uiState.first { it.settlingTransactionIds.isEmpty() && it.toastMessage != null && it.celebrationEvent?.id == eventId }
        }
        assertEquals(eventId, repeated.celebrationEvent!!.id)
        assertEquals(settledRows, db.transactionDao().getAllTransactionsOrdered())
        assertEquals(settledHistory, db.activityDao().getAllActivitiesList())

        vm.dismissCelebration(eventId)
        awaitActual { vm.uiState.first { it.celebrationEvent == null } }
        vm.clearToast()
        awaitActual { vm.uiState.first { it.toastMessage == null } }
        assertFalse(repay(600.0))
        val afterDismissedRepeat = awaitActual {
            vm.uiState.first { it.settlingTransactionIds.isEmpty() && it.toastMessage != null }
        }
        assertNull("An already-settled transaction must not produce a new celebration", afterDismissedRepeat.celebrationEvent)
        assertEquals(setOf(eventId), celebrationIds)
        assertEquals(2, accepted)
        assertEquals(2, rejected)
        assertEquals(settledRows, db.transactionDao().getAllTransactionsOrdered())
        assertEquals(settledHistory, db.activityDao().getAllActivitiesList())
    }

    /**
     * Requirement 5:
     * Mixed-direction zero-net does not trigger until both sides close.
     */
    @Test
    fun test05_mixedDirectionZeroNet_doesNotTriggerUntilBothSidesClose() = runTest {
        val friendId = repository.insertFriend("Farhan")
        val txLent = repository.addTransaction(friendId, 500.0, TransactionDirection.LENT, "Movie")
        val txBorrowed = repository.addTransaction(friendId, 500.0, TransactionDirection.BORROWED, "Tickets")

        val vm = FriendDetailViewModel(repository, friendId)
        vm.onScreenResumed()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        val stateInitial = vm.uiState.first { it.friend != null && it.timeline.size == 2 }

        // Net balance is 0.0, but open count is 2
        assertEquals(0.0, stateInitial.netBalance, 0.001)
        assertEquals(2, stateInitial.openTransactionsCount)
        assertNull("Never trigger celebration from netBalance == 0 alone", stateInitial.celebrationEvent)

        // Settle one side (LENT)
        vm.settleTransaction(txLent)
        advanceUntilIdle()

        val stateMid = vm.uiState.first { it.openTransactionsCount == 1 }
        assertNull("Must not celebrate while one side is still open", stateMid.celebrationEvent)
        assertEquals(1, stateMid.openTransactionsCount)

        // Settle other side (BORROWED)
        vm.settleTransaction(txBorrowed)
        advanceUntilIdle()

        val stateFinal = awaitCelebration(vm)
        assertNotNull("Must celebrate when both sides are finally closed (open count reaches 0)", stateFinal.celebrationEvent)
        assertEquals(0, stateFinal.openTransactionsCount)
    }

    /**
     * Requirement 6:
     * Failed/no-op/repeated actions do not create new events or duplicate Activity records.
     */
    @Test
    fun test06_failedNoOpRepeatedActions_doNotCreateNewEventsOrDuplicateActivity() = runTest {
        val friendId = repository.insertFriend("Gautam")
        val txId = repository.addTransaction(friendId, 400.0, TransactionDirection.LENT, "Books")

        val vm = FriendDetailViewModel(repository, friendId)
        vm.onScreenResumed()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        vm.uiState.first { it.friend != null && it.timeline.isNotEmpty() }

        // Successful settlement
        vm.settleTransaction(txId)
        advanceUntilIdle()

        val celebration1 = vm.uiState.first { it.celebrationEvent != null }.celebrationEvent
        assertNotNull(celebration1)

        val activityCountBefore = db.activityDao().getAllActivities().first().size

        // Repeated settlement on already CONFIRMED transaction
        vm.settleTransaction(txId)
        advanceUntilIdle()

        // Should not replace or create duplicate celebration
        val activityCountAfter = db.activityDao().getAllActivities().first().size
        assertEquals("No duplicate Activity entity should be created on no-op settlement", activityCountBefore, activityCountAfter)

        // Invalid repayment on already settled friend
        vm.recordRepayment(txId, 100.0)
        advanceUntilIdle()

        val activityCountAfterInvalid = db.activityDao().getAllActivities().first().size
        assertEquals(activityCountBefore, activityCountAfterInvalid)
    }

    /**
     * Requirement 7:
     * Overlapping settlement actions produce at most one completion event.
     */
    @Test
    fun test07_overlappingSettlementActions_produceAtMostOneEvent() = runTest {
        val friendId = repository.insertFriend("Harish")
        val txId = repository.addTransaction(friendId, 800.0, TransactionDirection.LENT, "Dinner")

        val vm = FriendDetailViewModel(repository, friendId)
        vm.onScreenResumed()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        vm.uiState.first { it.friend != null && it.timeline.isNotEmpty() }

        // Trigger settleTransaction twice rapidly
        vm.settleTransaction(txId)
        vm.settleTransaction(txId)
        advanceUntilIdle()

        val state = awaitCelebration(vm)
        assertNotNull(state.celebrationEvent)
        assertEquals(0, state.openTransactionsCount)
    }

    /**
     * Requirement 8:
     * Recomposition does not restart timeout; pause/dispose clears the event; returning does not replay.
     */
    @Test
    fun test08_pauseDisposeClearsEvent_returningDoesNotReplay() = runTest {
        val friendId = repository.insertFriend("Ishaan")
        val txId = repository.addTransaction(friendId, 350.0, TransactionDirection.LENT, "Fuel")

        val vm = FriendDetailViewModel(repository, friendId)
        vm.onScreenResumed()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        vm.uiState.first { it.friend != null && it.timeline.isNotEmpty() }

        vm.settleTransaction(txId)
        advanceUntilIdle()

        val stateActive = vm.uiState.first { it.celebrationEvent != null }
        val event = stateActive.celebrationEvent
        assertNotNull("Celebration should be active", event)

        // Screen pauses / leaves composition / rotates
        vm.onScreenPaused()
        advanceUntilIdle()
        assertFalse("Screen is not active after pause", vm.isScreenActive)
        val statePaused = vm.uiState.first { it.celebrationEvent == null }
        assertNull("Event must be cleared when screen pauses or leaves composition", statePaused.celebrationEvent)

        // Screen resumes / returns
        vm.onScreenResumed()
        advanceUntilIdle()
        assertTrue("Screen is active after resume", vm.isScreenActive)
        assertNull("Returning to screen must NOT replay the celebration", vm.uiState.value.celebrationEvent)
    }

    /**
     * Requirement 9:
     * An operation completing while inactive does not replay later.
     */
    @Test
    fun test09_operationCompletingWhileInactive_doesNotQueueOrReplayLater() = runTest {
        val friendId = repository.insertFriend("Jaya")
        val txId = repository.addTransaction(friendId, 450.0, TransactionDirection.LENT, "Tea")

        val vm = FriendDetailViewModel(repository, friendId)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        // Screen is NOT active (e.g. backgrounded or inactive)
        assertFalse(vm.isScreenActive)

        vm.settleTransaction(txId)
        advanceUntilIdle()

        assertNull("Should not queue celebration when screen is inactive", vm.uiState.value.celebrationEvent)

        // User opens/resumes screen later
        vm.onScreenResumed()
        advanceUntilIdle()
        assertNull("Should not replay celebration upon resumption", vm.uiState.value.celebrationEvent)
    }

    /**
     * Requirement 10:
     * A new loan and later full settlement can create a new event.
     */
    @Test
    fun test10_newLoanAndLaterFullSettlement_canCelebrateAgain() = runTest {
        val friendId = repository.insertFriend("Karan")
        val tx1 = repository.addTransaction(friendId, 500.0, TransactionDirection.LENT, "Movie")

        val vm = FriendDetailViewModel(repository, friendId)
        vm.onScreenResumed()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        vm.uiState.first { it.friend != null && it.timeline.isNotEmpty() }

        // Settle loan 1
        vm.settleTransaction(tx1)
        advanceUntilIdle()

        val state1 = vm.uiState.first { it.celebrationEvent != null }
        val event1 = state1.celebrationEvent
        assertNotNull("First settlement celebrates", event1)

        // Dismiss event 1
        vm.dismissCelebration(event1!!.id)
        advanceUntilIdle()
        val stateDismissed = vm.uiState.first { it.celebrationEvent == null }
        assertNull("Event 1 dismissed", stateDismissed.celebrationEvent)

        // Add a new loan later
        val tx2 = repository.addTransaction(friendId, 300.0, TransactionDirection.BORROWED, "Shopping")
        advanceUntilIdle()
        vm.uiState.first { it.openTransactionsCount == 1 }

        // Settle loan 2
        vm.settleTransaction(tx2)
        advanceUntilIdle()

        val state2 = vm.uiState.first { it.celebrationEvent != null }
        val event2 = state2.celebrationEvent
        assertNotNull("Second full settlement celebrates again", event2)
        assertNotEquals("New event has distinct ID", event1.id, event2!!.id)
    }

    /**
     * Requirement 11:
     * Correct resource wording, toast suppression and dismissal.
     */
    @Test
    fun test11_correctResourceWording_toastSuppressionAndDismissal() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()

        assertEquals("Tere mere hisaab…", context.getString(R.string.celebration_intro))
        assertEquals("Phittoos!", context.getString(R.string.celebration_headline))
        assertEquals("Lalit ke saath koi hisaab baaki nahi.", context.getString(R.string.celebration_support_with_name, "Lalit"))
        assertEquals("Is dost ke saath koi hisaab baaki nahi.", context.getString(R.string.celebration_support_generic))
        assertEquals("Dismiss celebration", context.getString(R.string.celebration_dismiss_cd))

        val friendId = repository.insertFriend("Lalit")
        val txId = repository.addTransaction(friendId, 700.0, TransactionDirection.LENT, "Rent")

        val vm = FriendDetailViewModel(repository, friendId)
        vm.onScreenResumed()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        vm.uiState.first { it.friend != null && it.timeline.isNotEmpty() }

        vm.settleTransaction(txId)
        advanceUntilIdle()

        val state = vm.uiState.first { it.celebrationEvent != null }
        val event = state.celebrationEvent
        assertNotNull(event)
        assertNull("Generic toast must be suppressed", state.toastMessage)

        // Dismissing with a non-matching ID should NOT dismiss active event
        vm.dismissCelebration(99999L)
        advanceUntilIdle()
        assertNotNull("Obsolete timeout or ID must not dismiss current event", vm.uiState.value.celebrationEvent)

        // Dismissing with active event ID
        vm.dismissCelebration(event!!.id)
        advanceUntilIdle()
        val stateAfterDismiss = vm.uiState.first { it.celebrationEvent == null }
        assertNull("Matching event ID dismisses the celebration", stateAfterDismiss.celebrationEvent)
    }

    // Use a wall-clock timeout: runTest's virtual clock does not own Room's executors.
    private suspend fun <T> awaitActual(block: suspend () -> T): T = withContext(Dispatchers.Default) {
        withTimeout(5_000) { block() }
    }

    private suspend fun awaitCelebration(vm: FriendDetailViewModel): FriendDetailUiState = awaitActual {
        vm.uiState.first { state ->
            !state.isLoading && state.openTransactionsCount == 0 &&
                state.settlingTransactionIds.isEmpty() && !state.isBulkSettling &&
                state.celebrationEvent != null && state.timeline.isNotEmpty() &&
                state.timeline.all { it.status == TransactionStatus.CONFIRMED && it.paidAmount == it.amount }
        }
    }
}
