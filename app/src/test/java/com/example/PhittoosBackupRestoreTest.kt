package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.*
import com.example.data.preferences.UserPreferences
import com.example.data.repository.PhittoosRepository
import com.example.export.PhittoosBackupManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PhittoosBackupRestoreTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: PhittoosRepository
    private lateinit var preferences: UserPreferences

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = androidx.room.Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = PhittoosRepository(
            friendDao = database.friendDao(),
            transactionDao = database.transactionDao(),
            activityDao = database.activityDao(),
            database = database
        )
        preferences = UserPreferences(context).apply {
            userName = "Test User"
            hasCompletedOnboarding = true
            remindersEnabled = true
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `test 1 backup and restore reproduces equivalent ledger state`() = runBlocking {
        // Insert some initial data
        val fId1 = database.friendDao().insertFriend(Friend(name = "Amit"))
        val fId2 = database.friendDao().insertFriend(Friend(name = "Sumit"))

        val tId1 = database.transactionDao().insertTransaction(
            TransactionEntity(
                friendId = fId1,
                amount = 200.0,
                direction = TransactionDirection.LENT,
                status = TransactionStatus.OPEN
            )
        )

        val actId1 = database.activityDao().insertActivity(
            ActivityEntity(
                type = ActivityType.TRANSACTION_CREATED,
                friendId = fId1,
                transactionId = tId1,
                amount = 200.0,
                direction = TransactionDirection.LENT,
                note = "Initial lent"
            )
        )

        // Generate backup
        val backupJson = PhittoosBackupManager.exportBackup(repository, preferences)

        // Modify local state to represent changes
        database.friendDao().deleteAllFriends()
        database.transactionDao().deleteAllTransactions()
        database.activityDao().deleteAllActivities()
        preferences.userName = "Someone Else"

        // Restore backup
        val restoreResult = PhittoosBackupManager.restoreBackup(backupJson, repository, preferences, database)
        assertTrue(restoreResult.isSuccess)

        // Verify recovered state matches original exactly
        assertEquals("Test User", preferences.userName)
        assertTrue(preferences.hasCompletedOnboarding)
        assertTrue(preferences.remindersEnabled)

        val restoredFriends = database.friendDao().getAllFriendsList()
        assertEquals(2, restoredFriends.size)
        assertEquals("Amit", restoredFriends.first { it.id == fId1 }.name)
        assertEquals("Sumit", restoredFriends.first { it.id == fId2 }.name)

        val restoredTxs = database.transactionDao().getAllTransactionsOrdered()
        assertEquals(1, restoredTxs.size)
        val tx = restoredTxs.first()
        assertEquals(tId1, tx.id)
        assertEquals(fId1, tx.friendId)
        assertEquals(200.0, tx.amount, 0.0)
        assertEquals(TransactionDirection.LENT, tx.direction)
        assertEquals(TransactionStatus.OPEN, tx.status)

        val restoredActs = database.activityDao().getAllActivitiesList()
        assertEquals(1, restoredActs.size)
        assertEquals(actId1, restoredActs.first().id)
        assertEquals(tId1, restoredActs.first().transactionId)
    }

    @Test
    fun `test 2 partial repayment survives restore`() = runBlocking {
        val fId = database.friendDao().insertFriend(Friend(name = "Rahul"))
        val tId = database.transactionDao().insertTransaction(
            TransactionEntity(
                friendId = fId,
                amount = 100.0,
                direction = TransactionDirection.LENT,
                status = TransactionStatus.OPEN,
                paidAmount = 40.0
            )
        )

        val backupJson = PhittoosBackupManager.exportBackup(repository, preferences)

        // Clear and restore
        repository.clearAllData()
        val restoreResult = PhittoosBackupManager.restoreBackup(backupJson, repository, preferences, database)
        assertTrue(restoreResult.isSuccess)

        val restoredTxs = database.transactionDao().getAllTransactionsOrdered()
        assertEquals(1, restoredTxs.size)
        val restoredTx = restoredTxs.first()
        assertEquals(tId, restoredTx.id)
        assertEquals(100.0, restoredTx.amount, 0.0)
        assertEquals(40.0, restoredTx.paidAmount ?: 0.0, 0.0)
        assertEquals(60.0, restoredTx.effectiveRemainingAmount, 0.0)
    }

    @Test
    fun `test 3 settled transaction survives restore`() = runBlocking {
        val fId = database.friendDao().insertFriend(Friend(name = "Rahul"))
        val tId = database.transactionDao().insertTransaction(
            TransactionEntity(
                friendId = fId,
                amount = 100.0,
                direction = TransactionDirection.LENT,
                status = TransactionStatus.CONFIRMED,
                paidAmount = 100.0,
                settledAt = 123456789L
            )
        )

        val backupJson = PhittoosBackupManager.exportBackup(repository, preferences)

        repository.clearAllData()
        val restoreResult = PhittoosBackupManager.restoreBackup(backupJson, repository, preferences, database)
        assertTrue(restoreResult.isSuccess)

        val restoredTxs = database.transactionDao().getAllTransactionsOrdered()
        assertEquals(1, restoredTxs.size)
        val restoredTx = restoredTxs.first()
        assertEquals(tId, restoredTx.id)
        assertEquals(TransactionStatus.CONFIRMED, restoredTx.status)
        assertEquals(100.0, restoredTx.paidAmount ?: 0.0, 0.0)
        assertEquals(123456789L, restoredTx.settledAt)
        assertEquals(0.0, restoredTx.effectiveRemainingAmount, 0.0)
    }

    @Test
    fun `test 4 mixed LENT and BORROWED friend restores correctly`() = runBlocking {
        val fId = database.friendDao().insertFriend(Friend(name = "Karan"))
        database.transactionDao().insertTransaction(
            TransactionEntity(
                friendId = fId,
                amount = 500.0,
                direction = TransactionDirection.LENT,
                status = TransactionStatus.OPEN
            )
        )
        database.transactionDao().insertTransaction(
            TransactionEntity(
                friendId = fId,
                amount = 300.0,
                direction = TransactionDirection.BORROWED,
                status = TransactionStatus.OPEN
            )
        )

        val backupJson = PhittoosBackupManager.exportBackup(repository, preferences)

        repository.clearAllData()
        val restoreResult = PhittoosBackupManager.restoreBackup(backupJson, repository, preferences, database)
        assertTrue(restoreResult.isSuccess)

        val restoredTxs = database.transactionDao().getAllTransactionsOrdered()
        assertEquals(2, restoredTxs.size)
        val lentTx = restoredTxs.first { it.direction == TransactionDirection.LENT }
        val borrowedTx = restoredTxs.first { it.direction == TransactionDirection.BORROWED }
        assertEquals(500.0, lentTx.amount, 0.0)
        assertEquals(300.0, borrowedTx.amount, 0.0)
        assertEquals(fId, lentTx.friendId)
        assertEquals(fId, borrowedTx.friendId)
    }

    @Test
    fun `test 5 malformed backup is rejected without modifying existing data`() = runBlocking {
        // Establish original state
        val fId = database.friendDao().insertFriend(Friend(name = "KeepMe"))
        preferences.userName = "OriginalUser"

        val malformedJson = "{ \"metadata\": { \"backupVersion\": 1 }, \"preferences\": { " // truncated/invalid JSON

        val result = PhittoosBackupManager.restoreBackup(malformedJson, repository, preferences, database)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Malformed JSON") ?: false)

        // Verify data remains untouched
        assertEquals("OriginalUser", preferences.userName)
        val friends = database.friendDao().getAllFriendsList()
        assertEquals(1, friends.size)
        assertEquals("KeepMe", friends.first().name)
    }

    @Test
    fun `test 6 unsupported backupVersion is rejected`() = runBlocking {
        // Prepare unsupported JSON with version 99
        val unsupportedJson = """
            {
              "metadata": {
                "backupVersion": 99,
                "appVersion": "1.0.0",
                "createdAt": 1726394000000
              },
              "preferences": {
                "userName": "Rejected",
                "hasCompletedOnboarding": true,
                "remindersEnabled": true
              },
              "friends": [],
              "transactions": []
            }
        """.trimIndent()

        val result = PhittoosBackupManager.restoreBackup(unsupportedJson, repository, preferences, database)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Unsupported backup version") ?: false)
    }

    @Test
    fun `test 7 failed restore leaves original database unchanged`() = runBlocking {
        // Setup initial healthy database
        val fId = database.friendDao().insertFriend(Friend(name = "Rahul"))
        database.transactionDao().insertTransaction(
            TransactionEntity(
                friendId = fId,
                amount = 100.0,
                direction = TransactionDirection.LENT,
                status = TransactionStatus.OPEN
            )
        )
        preferences.userName = "OriginalUser"

        // Invalid JSON because transaction references non-existent friend ID 999 (Referential integrity failure)
        val brokenRefJson = """
            {
              "metadata": {
                "backupVersion": 1,
                "createdAt": 12345
              },
              "preferences": {
                "userName": "BadUser",
                "hasCompletedOnboarding": true,
                "remindersEnabled": true
              },
              "friends": [
                {
                  "id": 1,
                  "name": "FriendOne",
                  "reliabilityTier": "NEW"
                }
              ],
              "transactions": [
                {
                  "id": 10,
                  "friendId": 999,
                  "amount": 100.0,
                  "direction": "LENT",
                  "status": "OPEN"
                }
              ]
            }
        """.trimIndent()

        val result = PhittoosBackupManager.restoreBackup(brokenRefJson, repository, preferences, database)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Referential Integrity Check Failed") ?: false)

        // Verify original data is completely untouched (atomic transaction safety)
        assertEquals("OriginalUser", preferences.userName)
        val restoredFriends = database.friendDao().getAllFriendsList()
        assertEquals(1, restoredFriends.size)
        assertEquals("Rahul", restoredFriends.first().name)

        val restoredTxs = database.transactionDao().getAllTransactionsOrdered()
        assertEquals(1, restoredTxs.size)
        assertEquals(100.0, restoredTxs.first().amount, 0.0)
    }
}
