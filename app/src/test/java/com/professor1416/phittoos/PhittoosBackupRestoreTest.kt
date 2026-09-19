package com.professor1416.phittoos

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.professor1416.phittoos.data.db.AppDatabase
import com.professor1416.phittoos.data.model.*
import com.professor1416.phittoos.data.preferences.UserPreferences
import com.professor1416.phittoos.data.repository.PhittoosRepository
import com.professor1416.phittoos.export.PhittoosBackupManager
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
        assertTrue(result.exceptionOrNull()?.message?.contains("This isn’t a valid Phittoos backup file") ?: false)

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
              "transactions": [],
              "activities": []
            }
        """.trimIndent()

        val result = PhittoosBackupManager.restoreBackup(unsupportedJson, repository, preferences, database)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("This backup was created by a newer version of Phittoos and can’t be restored with this version") ?: false)
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
              ],
              "activities": []
            }
        """.trimIndent()

        val result = PhittoosBackupManager.restoreBackup(brokenRefJson, repository, preferences, database)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("This isn’t a valid Phittoos backup file") ?: false)

        // Verify original data is completely untouched (atomic transaction safety)
        assertEquals("OriginalUser", preferences.userName)
        val restoredFriends = database.friendDao().getAllFriendsList()
        assertEquals(1, restoredFriends.size)
        assertEquals("Rahul", restoredFriends.first().name)

        val restoredTxs = database.transactionDao().getAllTransactionsOrdered()
        assertEquals(1, restoredTxs.size)
        assertEquals(100.0, restoredTxs.first().amount, 0.0)
    }

    @Test
    fun `test 8 csv files are rejected with specific user friendly message`() = runBlocking {
        val csvContent = """
            "Friend","Amount","Direction","Status"
            "Rahul","100.0","LENT","OPEN"
        """.trimIndent()

        val result = PhittoosBackupManager.restoreBackup(csvContent, repository, preferences, database)
        assertTrue(result.isFailure)
        assertEquals(
            "CSV files can’t be restored. CSV export is for viewing or sharing your records. Select a Phittoos backup file instead.",
            result.exceptionOrNull()?.message
        )
    }

    @Test
    fun `test 9 duplicate friend IDs are rejected`() = runBlocking {
        val duplicateFriendsJson = """
            {
              "metadata": { "backupVersion": 1 },
              "preferences": { "userName": "User" },
              "friends": [
                { "id": 1, "name": "FriendOne" },
                { "id": 1, "name": "FriendTwo" }
              ],
              "transactions": [],
              "activities": []
            }
        """.trimIndent()

        val result = PhittoosBackupManager.restoreBackup(duplicateFriendsJson, repository, preferences, database)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("This isn’t a valid Phittoos backup file") ?: false)
    }

    @Test
    fun `test 10 empty valid backup restores successfully`() = runBlocking {
        val emptyBackupJson = """
            {
              "metadata": {
                "backupVersion": 1,
                "appVersion": "1.0.0",
                "createdAt": 1726394000000
              },
              "preferences": {
                "userName": "Empty Snapshot User",
                "hasCompletedOnboarding": true,
                "remindersEnabled": false
              },
              "friends": [],
              "transactions": [],
              "activities": []
            }
        """.trimIndent()

        // Establish original data
        database.friendDao().insertFriend(Friend(name = "Amit"))

        val result = PhittoosBackupManager.restoreBackup(emptyBackupJson, repository, preferences, database)
        assertTrue(result.isSuccess)

        assertEquals("Empty Snapshot User", preferences.userName)
        assertFalse(preferences.remindersEnabled)
        assertTrue(database.friendDao().getAllFriendsList().isEmpty())
    }

    @Test
    fun `test 11 restoring same backup twice works without duplication`() = runBlocking {
        val fId = database.friendDao().insertFriend(Friend(name = "Amit"))
        val backupJson = PhittoosBackupManager.exportBackup(repository, preferences)

        // Restore once
        val res1 = PhittoosBackupManager.restoreBackup(backupJson, repository, preferences, database)
        assertTrue(res1.isSuccess)
        assertEquals(1, database.friendDao().getAllFriendsList().size)

        // Restore twice
        val res2 = PhittoosBackupManager.restoreBackup(backupJson, repository, preferences, database)
        assertTrue(res2.isSuccess)
        assertEquals(1, database.friendDao().getAllFriendsList().size)
    }

    @Test
    fun `test 12 restoring old backup over newer data produces exactly old snapshot`() = runBlocking {
        // Old snapshot setup
        val fId1 = database.friendDao().insertFriend(Friend(name = "Amit"))
        val oldBackup = PhittoosBackupManager.exportBackup(repository, preferences)

        // Add newer data
        val fId2 = database.friendDao().insertFriend(Friend(name = "Sumit"))
        database.transactionDao().insertTransaction(
            TransactionEntity(
                friendId = fId2,
                amount = 150.0,
                direction = TransactionDirection.LENT,
                status = TransactionStatus.OPEN
            )
        )
        assertEquals(2, database.friendDao().getAllFriendsList().size)
        assertEquals(1, database.transactionDao().getAllTransactionsOrdered().size)

        // Restore old snapshot
        val res = PhittoosBackupManager.restoreBackup(oldBackup, repository, preferences, database)
        assertTrue(res.isSuccess)

        val finalFriends = database.friendDao().getAllFriendsList()
        assertEquals(1, finalFriends.size)
        assertEquals("Amit", finalFriends.first().name)
        assertTrue(database.transactionDao().getAllTransactionsOrdered().isEmpty())
    }

    @Test
    fun `test 13 duplicate transaction IDs are rejected`() = runBlocking {
        val json = """
            {
              "metadata": { "backupVersion": 1 },
              "preferences": { "userName": "User" },
              "friends": [
                { "id": 1, "name": "Amit" }
              ],
              "transactions": [
                { "id": 100, "friendId": 1, "amount": 50.0, "direction": "LENT", "status": "OPEN" },
                { "id": 100, "friendId": 1, "amount": 25.0, "direction": "BORROWED", "status": "OPEN" }
              ],
              "activities": []
            }
        """.trimIndent()

        val result = PhittoosBackupManager.restoreBackup(json, repository, preferences, database)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("This isn’t a valid Phittoos backup file") ?: false)
    }

    @Test
    fun `test 14 duplicate activity IDs are rejected`() = runBlocking {
        val json = """
            {
              "metadata": { "backupVersion": 1 },
              "preferences": { "userName": "User" },
              "friends": [
                { "id": 1, "name": "Amit" }
              ],
              "transactions": [
                { "id": 10, "friendId": 1, "amount": 50.0, "direction": "LENT", "status": "OPEN" }
              ],
              "activities": [
                { "id": 5, "type": "TRANSACTION_CREATED", "friendId": 1, "transactionId": 10, "amount": 50.0 },
                { "id": 5, "type": "TRANSACTION_SETTLED", "friendId": 1, "transactionId": 10 }
              ]
            }
        """.trimIndent()

        val result = PhittoosBackupManager.restoreBackup(json, repository, preferences, database)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("This isn’t a valid Phittoos backup file") ?: false)
    }

    @Test
    fun `test 15 activity referencing missing transaction is rejected`() = runBlocking {
        val json = """
            {
              "metadata": { "backupVersion": 1 },
              "preferences": { "userName": "User" },
              "friends": [
                { "id": 1, "name": "Amit" }
              ],
              "transactions": [
                { "id": 10, "friendId": 1, "amount": 50.0, "direction": "LENT", "status": "OPEN" }
              ],
              "activities": [
                { "id": 5, "type": "TRANSACTION_CREATED", "friendId": 1, "transactionId": 999, "amount": 50.0 }
              ]
            }
        """.trimIndent()

        val result = PhittoosBackupManager.restoreBackup(json, repository, preferences, database)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("This isn’t a valid Phittoos backup file") ?: false)
    }

    @Test
    fun `test 16 random non-backup JSON is rejected`() = runBlocking {
        val randomJson = """
            {
              "someRandomKey": "randomValue",
              "anotherKey": 12345
            }
        """.trimIndent()

        val result = PhittoosBackupManager.restoreBackup(randomJson, repository, preferences, database)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("This isn’t a valid Phittoos backup file") ?: false)
    }

    @Test
    fun `test 17 non-positive transaction amount is rejected`() = runBlocking {
        val json = """
            {
              "metadata": { "backupVersion": 1 },
              "preferences": { "userName": "User" },
              "friends": [ { "id": 1, "name": "Amit" } ],
              "transactions": [
                { "id": 10, "friendId": 1, "amount": 0.0, "direction": "LENT", "status": "OPEN" }
              ],
              "activities": []
            }
        """.trimIndent()

        val result = PhittoosBackupManager.restoreBackup(json, repository, preferences, database)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("This isn’t a valid Phittoos backup file") ?: false)
    }

    @Test
    fun `test 18 paidAmount exceeding original amount is rejected`() = runBlocking {
        val json = """
            {
              "metadata": { "backupVersion": 1 },
              "preferences": { "userName": "User" },
              "friends": [ { "id": 1, "name": "Amit" } ],
              "transactions": [
                { "id": 10, "friendId": 1, "amount": 100.0, "paidAmount": 100.01, "direction": "LENT", "status": "OPEN" }
              ],
              "activities": []
            }
        """.trimIndent()

        val result = PhittoosBackupManager.restoreBackup(json, repository, preferences, database)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("This isn’t a valid Phittoos backup file") ?: false)
    }

    @Test
    fun `test 19 invalid direction enum value is rejected`() = runBlocking {
        val json = """
            {
              "metadata": { "backupVersion": 1 },
              "preferences": { "userName": "User" },
              "friends": [ { "id": 1, "name": "Amit" } ],
              "transactions": [
                { "id": 10, "friendId": 1, "amount": 100.0, "direction": "INVALID_ENUM", "status": "OPEN" }
              ],
              "activities": []
            }
        """.trimIndent()

        val result = PhittoosBackupManager.restoreBackup(json, repository, preferences, database)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("This isn’t a valid Phittoos backup file") ?: false)
    }

    @Test
    fun `test 20 current profile name is replaced by backup profile name`() = runBlocking {
        preferences.userName = "New Profile Name"

        val backupJson = """
            {
              "metadata": { "backupVersion": 1 },
              "preferences": {
                "userName": "Backup Profile Name",
                "hasCompletedOnboarding": true,
                "remindersEnabled": true
              },
              "friends": [],
              "transactions": [],
              "activities": []
            }
        """.trimIndent()

        val result = PhittoosBackupManager.restoreBackup(backupJson, repository, preferences, database)
        assertTrue(result.isSuccess)
        assertEquals("Backup Profile Name", preferences.userName)
    }
}
