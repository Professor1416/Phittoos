package com.professor1416.phittoos

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.professor1416.phittoos.data.db.AppDatabase
import com.professor1416.phittoos.data.model.ActivityType
import com.professor1416.phittoos.data.model.TransactionDirection
import com.professor1416.phittoos.data.repository.DeleteErrorReason
import com.professor1416.phittoos.data.repository.DeleteTransactionResult
import com.professor1416.phittoos.data.repository.EditErrorReason
import com.professor1416.phittoos.data.repository.EditTransactionResult
import com.professor1416.phittoos.data.repository.PhittoosRepository
import com.professor1416.phittoos.ui.viewmodel.AddTransactionViewModel
import com.professor1416.phittoos.ui.viewmodel.FriendDetailViewModel
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
class ContextualAddAndWrongEntryRecoveryTest {

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

    @Test
    fun contextualAdd_withValidFriendId_locksFriend() = runTest {
        val friendId = repository.insertFriend("Ali")
        val viewModel = AddTransactionViewModel(repository, initialFriendId = friendId)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isFriendLocked)
        assertFalse(state.isFriendNotFound)
        assertFalse(state.isEditMode)
        assertNotNull(state.selectedFriend)
        assertEquals("Ali", state.selectedFriend?.name)
    }

    @Test
    fun contextualAdd_withInvalidFriendId_setsFriendNotFound() = runTest {
        val viewModel = AddTransactionViewModel(repository, initialFriendId = 99999L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isFriendLocked)
        assertTrue(state.isFriendNotFound)
        assertNull(state.selectedFriend)
    }

    @Test
    fun genericAdd_withoutFriendId_doesNotLockFriend() = runTest {
        repository.insertFriend("Bilal")
        val viewModel = AddTransactionViewModel(repository, initialFriendId = null)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isFriendLocked)
        assertFalse(state.isFriendNotFound)
        assertFalse(state.isEditMode)
        assertNull(state.selectedFriend)
    }

    @Test
    fun editOpenUntouchedTransaction_successUpdatesValuesAndActivity() = runTest {
        val friendId = repository.insertFriend("Chaudhry")
        val txId = repository.addTransaction(
            friendId = friendId,
            amount = 500.0,
            direction = TransactionDirection.LENT,
            dueDate = null,
            note = "Original Note"
        )

        // Verify initial balance
        val initialFriends = repository.friendsWithBalance.first()
        val initialFriend = initialFriends.find { it.friend.id == friendId }
        assertEquals(500.0, initialFriend?.netBalance ?: 0.0, 0.001)

        // Perform edit
        val editResult = repository.updateOpenUnpaidTransaction(
            transactionId = txId,
            amount = 300.0,
            direction = TransactionDirection.BORROWED,
            dueDate = 1800000000000L,
            note = "Updated Note"
        )

        assertTrue(editResult is EditTransactionResult.Success)
        val updatedTx = repository.getTransactionById(txId)
        assertNotNull(updatedTx)
        assertEquals(txId, updatedTx!!.id)
        assertEquals(300.0, updatedTx.amount, 0.001)
        assertEquals(TransactionDirection.BORROWED, updatedTx.direction)
        assertEquals("Updated Note", updatedTx.note)
        assertEquals(1800000000000L, updatedTx.dueDate)

        // Check updated friend balance (-300.0 because it's now BORROWED)
        val updatedFriends = repository.friendsWithBalance.first()
        val updatedFriend = updatedFriends.find { it.friend.id == friendId }
        assertEquals(-300.0, updatedFriend?.netBalance ?: 0.0, 0.001)

        // Verify activity row updated
        val activities = db.activityDao().getActivitiesForFriend(friendId).first()
        val txActivity = activities.find { it.transactionId == txId && it.type == ActivityType.TRANSACTION_CREATED }
        assertNotNull(txActivity)
        assertEquals(300.0, txActivity?.amount ?: 0.0, 0.001)
        assertEquals(TransactionDirection.BORROWED, txActivity?.direction)
    }

    @Test
    fun editTransaction_rejectedIfHasRepayments() = runTest {
        val friendId = repository.insertFriend("Danish")
        val txId = repository.addTransaction(friendId, 1000.0, TransactionDirection.LENT)
        repository.recordRepayment(txId, 200.0)

        val result = repository.updateOpenUnpaidTransaction(
            transactionId = txId,
            amount = 800.0,
            direction = TransactionDirection.LENT,
            dueDate = null,
            note = "Attempt edit"
        )

        assertTrue(result is EditTransactionResult.Error)
        assertEquals(EditErrorReason.HAS_REPAYMENTS, (result as EditTransactionResult.Error).reason)
    }

    @Test
    fun editTransaction_rejectedIfSettled() = runTest {
        val friendId = repository.insertFriend("Ehsan")
        val txId = repository.addTransaction(friendId, 400.0, TransactionDirection.LENT)
        repository.markTransactionAsPaid(txId)

        val result = repository.updateOpenUnpaidTransaction(
            transactionId = txId,
            amount = 500.0,
            direction = TransactionDirection.LENT,
            dueDate = null,
            note = "Attempt edit"
        )

        assertTrue(result is EditTransactionResult.Error)
        assertEquals(EditErrorReason.NOT_OPEN, (result as EditTransactionResult.Error).reason)
    }

    @Test
    fun editTransaction_rejectedIfInvalidAmount() = runTest {
        val friendId = repository.insertFriend("Fahad")
        val txId = repository.addTransaction(friendId, 600.0, TransactionDirection.LENT)

        val resultZero = repository.updateOpenUnpaidTransaction(
            transactionId = txId,
            amount = 0.0,
            direction = TransactionDirection.LENT,
            dueDate = null,
            note = null
        )
        assertTrue(resultZero is EditTransactionResult.Error)
        assertEquals(EditErrorReason.INVALID_AMOUNT, (resultZero as EditTransactionResult.Error).reason)

        val resultNeg = repository.updateOpenUnpaidTransaction(
            transactionId = txId,
            amount = -100.0,
            direction = TransactionDirection.LENT,
            dueDate = null,
            note = null
        )
        assertTrue(resultNeg is EditTransactionResult.Error)
        assertEquals(EditErrorReason.INVALID_AMOUNT, (resultNeg as EditTransactionResult.Error).reason)
    }

    @Test
    fun deleteOpenUntouchedTransaction_successDeletesAndCleansActivity() = runTest {
        val friendId = repository.insertFriend("Gohar")
        val txId = repository.addTransaction(friendId, 750.0, TransactionDirection.LENT)

        // Verify initial state
        val initialTxs = repository.getTransactionsForFriend(friendId).first()
        assertEquals(1, initialTxs.size)

        // Delete transaction
        val result = repository.deleteOpenUnpaidTransaction(txId)
        assertTrue(result is DeleteTransactionResult.Success)

        // Verify transaction deleted
        val postTxs = repository.getTransactionsForFriend(friendId).first()
        assertEquals(0, postTxs.size)

        // Verify friend balance is now 0
        val friends = repository.friendsWithBalance.first()
        val friend = friends.find { it.friend.id == friendId }
        assertEquals(0.0, friend?.netBalance ?: 0.0, 0.001)

        // Verify activity deleted
        val activities = db.activityDao().getActivitiesForFriend(friendId).first()
        val txActivity = activities.find { it.transactionId == txId }
        assertNull(txActivity)
    }

    @Test
    fun deleteTransaction_rejectedIfHasRepayments() = runTest {
        val friendId = repository.insertFriend("Hamza")
        val txId = repository.addTransaction(friendId, 1000.0, TransactionDirection.LENT)
        repository.recordRepayment(txId, 300.0)

        val result = repository.deleteOpenUnpaidTransaction(txId)
        assertTrue(result is DeleteTransactionResult.Error)
        assertEquals(DeleteErrorReason.HAS_REPAYMENTS, (result as DeleteTransactionResult.Error).reason)
    }

    @Test
    fun deleteTransaction_rejectedIfSettled() = runTest {
        val friendId = repository.insertFriend("Imran")
        val txId = repository.addTransaction(friendId, 500.0, TransactionDirection.LENT)
        repository.markTransactionAsPaid(txId)

        val result = repository.deleteOpenUnpaidTransaction(txId)
        assertTrue(result is DeleteTransactionResult.Error)
        assertEquals(DeleteErrorReason.NOT_OPEN, (result as DeleteTransactionResult.Error).reason)
    }

    @Test
    fun deleteTransaction_rejectedIfNotFound() = runTest {
        val result = repository.deleteOpenUnpaidTransaction(99999L)
        assertTrue(result is DeleteTransactionResult.Error)
        assertEquals(DeleteErrorReason.TRANSACTION_NOT_FOUND, (result as DeleteTransactionResult.Error).reason)
    }

    @Test
    fun addTransactionViewModel_editModeLifecycle() = runTest {
        val friendId = repository.insertFriend("Javed")
        val txId = repository.addTransaction(
            friendId = friendId,
            amount = 1200.0,
            direction = TransactionDirection.LENT,
            dueDate = null,
            note = "Original Note"
        )

        val viewModel = AddTransactionViewModel(
            repository = repository,
            initialFriendId = friendId,
            transactionIdToEdit = txId
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isEditMode)
        assertTrue(state.isFriendLocked)
        assertEquals("1200", state.amount)
        assertEquals(TransactionDirection.LENT, state.direction)
        assertEquals("Original Note", state.note)

        // Change amount & direction
        viewModel.setAmount("1500")
        viewModel.setDirection(TransactionDirection.BORROWED)
        viewModel.setNote("Updated Note via VM")

        var saved = false
        viewModel.saveTransaction {
            saved = true
        }
        advanceUntilIdle()

        assertTrue(saved)
        val updatedTx = repository.getTransactionById(txId)
        assertNotNull(updatedTx)
        assertEquals(1500.0, updatedTx?.amount ?: 0.0, 0.001)
        assertEquals(TransactionDirection.BORROWED, updatedTx?.direction)
        assertEquals("Updated Note via VM", updatedTx?.note)
    }

    @Test
    fun friendDetailViewModel_deleteTransactionFlow() = runTest {
        val friendId = repository.insertFriend("Kamran")
        val txId = repository.addTransaction(friendId, 250.0, TransactionDirection.LENT)

        val viewModel = FriendDetailViewModel(repository, friendId)
        advanceUntilIdle()

        var successCalled = false
        viewModel.deleteTransaction(txId, onSuccess = {
            successCalled = true
        })
        advanceUntilIdle()

        assertTrue(successCalled)
        val txs = repository.getTransactionsForFriend(friendId).first()
        assertEquals(0, txs.size)
    }
}
