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

    // Synthetic compatibility fixtures. All rejection tests exercise the real Room-backed restore path.
    private fun validFinancialBackup(): org.json.JSONObject = org.json.JSONObject("""
        {
          "metadata": {"backupVersion": 1, "createdAt": 1000},
          "preferences": {"userName": "Imported synthetic owner", "hasCompletedOnboarding": true, "remindersEnabled": true},
          "friends": [{"id": 101, "name": "Synthetic A"}, {"id": 102, "name": "Synthetic B"}],
          "transactions": [{"id": 201, "friendId": 101, "amount": 1000, "direction": "LENT", "status": "OPEN", "createdDate": 0, "paidAmount": null, "settledAt": null}],
          "activities": [{"id": 301, "type": "TRANSACTION_CREATED", "friendId": 101, "transactionId": 201, "amount": 1000, "direction": "LENT", "createdAt": 0}]
        }
    """.trimIndent())

    private fun org.json.JSONObject.transaction() = getJSONArray("transactions").getJSONObject(0)
    private fun org.json.JSONObject.activity() = getJSONArray("activities").getJSONObject(0)

    private suspend fun assertRejectedWithoutMutation(json: String, case: String) {
        if (database.friendDao().getAllFriendsList().isEmpty()) {
            val friend = repository.insertFriend("Existing synthetic ledger")
            val transaction = repository.addTransaction(friend, 125.50, TransactionDirection.BORROWED, "Preserve", createdDate = 0L)
            assertTrue(repository.recordRepayment(transaction, 25.50, settledAt = 0L) is com.professor1416.phittoos.data.repository.RepaymentResult.Success)
        }
        preferences.userName = "Existing synthetic owner"
        preferences.hasCompletedOnboarding = false
        preferences.remindersEnabled = false
        preferences.lastBackupTime = 9876L
        preferences.themeMode = com.professor1416.phittoos.data.preferences.AppThemeMode.DARK
        val beforeFriends = database.friendDao().getAllFriendsList()
        val beforeTransactions = database.transactionDao().getAllTransactionsOrdered()
        val beforeActivities = database.activityDao().getAllActivitiesList()
        val beforePreferences = context.getSharedPreferences("phittoos_prefs", Context.MODE_PRIVATE).all.toMap()
        val beforeTheme = preferences.themeModeFlow.value
        assertTrue("$case must fail validation", PhittoosBackupManager.validateBackup(json).isFailure)
        val result = PhittoosBackupManager.restoreBackup(json, repository, preferences, database)
        assertTrue("$case must fail restore", result.isFailure)
        assertFalse("$case must explain failure", result.exceptionOrNull()?.message.isNullOrBlank())
        assertEquals("$case friends", beforeFriends, database.friendDao().getAllFriendsList())
        assertEquals("$case transactions", beforeTransactions, database.transactionDao().getAllTransactionsOrdered())
        assertEquals("$case activities", beforeActivities, database.activityDao().getAllActivitiesList())
        assertEquals("$case preferences", beforePreferences, context.getSharedPreferences("phittoos_prefs", Context.MODE_PRIVATE).all)
        assertEquals("$case theme state", beforeTheme, preferences.themeModeFlow.value)
    }

    private suspend fun assertRoundTripPreservesRecords() {
        val friends = database.friendDao().getAllFriendsList()
        val transactions = database.transactionDao().getAllTransactionsOrdered()
        val activities = database.activityDao().getAllActivitiesList()
        val backup = PhittoosBackupManager.exportBackup(repository, preferences)
        assertTrue(PhittoosBackupManager.validateBackup(backup).isSuccess)
        assertEquals(1, org.json.JSONObject(backup).getJSONObject("metadata").getInt("backupVersion"))
        repository.clearAllData()
        assertTrue(PhittoosBackupManager.restoreBackup(backup, repository, preferences, database).isSuccess)
        assertEquals(friends, database.friendDao().getAllFriendsList())
        assertEquals(transactions, database.transactionDao().getAllTransactionsOrdered())
        assertEquals(activities, database.activityDao().getAllActivitiesList())
    }

    @Test fun rejectsCrossFriendFinancialActivityWithoutMutation() = runBlocking {
        val root = validFinancialBackup()
        root.activity().put("friendId", 102)
        assertRejectedWithoutMutation(root.toString(), "cross-friend activity")
    }

    @Test fun rejectsContradictoryFinancialDirectionWithoutMutation() = runBlocking {
        for (type in listOf("TRANSACTION_CREATED", "PARTIAL_REPAYMENT", "SETTLED")) {
            val root = validFinancialBackup()
            root.transaction().put("status", "CONFIRMED").put("paidAmount", 1000)
            root.activity().put("type", type).put("direction", "BORROWED")
            assertRejectedWithoutMutation(root.toString(), "$type direction")
        }
    }

    @Test fun rejectsContradictoryOpenPaymentStateWithoutMutation() = runBlocking {
        for ((paid, settled) in listOf(1000 to org.json.JSONObject.NULL, 400 to 0L, 0 to 1000L)) {
            val root = validFinancialBackup()
            root.transaction().put("paidAmount", paid).put("settledAt", settled)
            assertRejectedWithoutMutation(root.toString(), "OPEN paid=$paid settled=$settled")
        }
    }

    @Test fun rejectsConfirmedExplicitUnderpaymentWithoutMutation() = runBlocking {
        for (paid in listOf(0.0, 400.0, 999.999)) {
            val root = validFinancialBackup()
            root.transaction().put("status", "CONFIRMED").put("paidAmount", paid)
            assertRejectedWithoutMutation(root.toString(), "CONFIRMED paid=$paid")
        }
    }

    @Test fun rejectsSettlementEventOnOpenTransactionWithoutMutation() = runBlocking {
        val root = validFinancialBackup()
        root.activity().put("type", "SETTLED")
        assertRejectedWithoutMutation(root.toString(), "settlement event on OPEN")
    }

    @Test fun rejectsMalformedMoneyWithoutMutation() = runBlocking {
        for (bad in listOf<Any>("1000", "NaN", "Infinity", true, org.json.JSONObject(), org.json.JSONObject.NULL)) {
            val root = validFinancialBackup()
            root.transaction().put("amount", bad)
            assertRejectedWithoutMutation(root.toString(), "malformed principal $bad")
        }
        for (field in listOf("paidAmount", "amount")) {
            val root = validFinancialBackup()
            if (field == "paidAmount") root.transaction().put(field, "400") else root.activity().put(field, "400")
            assertRejectedWithoutMutation(root.toString(), "coerced $field")
        }
        val root = validFinancialBackup().toString().replace("\"amount\":1000", "\"amount\":1e400")
        assertRejectedWithoutMutation(root, "overflowing amount")
    }

    @Test fun rejectsMalformedIdentifiersWithoutMutation() = runBlocking {
        for (field in listOf("id", "friendId")) {
            val root = validFinancialBackup()
            root.transaction().put(field, if (field == "id") 201.5 else "101")
            assertRejectedWithoutMutation(root.toString(), "malformed transaction $field")
        }
        val root = validFinancialBackup()
        root.activity().put("transactionId", 201.5)
        assertRejectedWithoutMutation(root.toString(), "fractional activity reference")
    }

    @Test fun rejectsMalformedPresentTimestampsWithoutMutation() = runBlocking {
        for (field in listOf("createdDate", "dueDate", "settledAt")) {
            for (bad in listOf<Any>("not-a-date", 0.5, true, "9223372036854775808", Double.MAX_VALUE)) {
                val root = validFinancialBackup()
                root.transaction().put(field, bad)
                assertRejectedWithoutMutation(root.toString(), "$field=$bad")
            }
        }
        for (section in listOf("metadata", "activities")) {
            val root = validFinancialBackup()
            val record = if (section == "metadata") root.getJSONObject("metadata") else root.activity()
            record.put("createdAt", "invalid")
            assertRejectedWithoutMutation(root.toString(), "$section timestamp")
        }
    }

    @Test fun rejectsImpossibleFinancialEventValuesWithoutMutation() = runBlocking {
        for ((type, amount) in listOf("TRANSACTION_CREATED" to 999.0, "PARTIAL_REPAYMENT" to 1001.0, "SETTLED" to 1001.0)) {
            val root = validFinancialBackup()
            root.transaction().put("status", "CONFIRMED").put("paidAmount", 1000)
            root.activity().put("type", type).put("amount", amount)
            assertRejectedWithoutMutation(root.toString(), "$type amount=$amount")
        }
    }

    @Test fun realUntouchedPartialManualAndFinalRecordsRoundTripExactly() = runBlocking {
        val friend = repository.insertFriend("Synthetic lifecycle friend")
        repository.addTransaction(friend, 100.0, TransactionDirection.LENT, createdDate = 0L)
        val partial = repository.addTransaction(friend, 1000.0, TransactionDirection.BORROWED, createdDate = 0L)
        assertTrue(repository.recordRepayment(partial, 400.0, settledAt = 0L) is com.professor1416.phittoos.data.repository.RepaymentResult.Success)
        val manual = repository.addTransaction(friend, 500.0, TransactionDirection.LENT, createdDate = 0L)
        assertTrue(repository.markTransactionAsPaid(manual, settledAt = 0L).success)
        val final = repository.addTransaction(friend, 1000.0, TransactionDirection.LENT, createdDate = 0L)
        repository.recordRepayment(final, 400.0, settledAt = 0L)
        assertTrue(repository.recordRepayment(final, 600.0, settledAt = 1L) is com.professor1416.phittoos.data.repository.RepaymentResult.Success)
        assertRoundTripPreservesRecords()
        assertEquals(400.0, database.transactionDao().getTransactionById(partial)!!.paidAmount!!, 0.0)
        assertEquals(1000.0, database.transactionDao().getTransactionById(final)!!.paidAmount!!, 0.0)
    }

    @Test fun legacyNullableConfirmedAndHigherPrecisionRecordsRoundTripExactly() = runBlocking {
        val friend = repository.insertFriend("Synthetic legacy friend")
        // 1281ee4 settled status only; migration 1->2 adds NULL settled_at; neither backfills history.
        for ((index, paid) in listOf<Double?>(null, 1000.1234).withIndex()) {
            database.transactionDao().insertTransaction(TransactionEntity(
                friendId = friend, amount = 1000.1234, direction = TransactionDirection.LENT,
                status = TransactionStatus.CONFIRMED, paidAmount = paid, settledAt = null, createdDate = index.toLong()
            ))
        }
        database.transactionDao().insertTransaction(TransactionEntity(
            friendId = friend, amount = 1000.1234, direction = TransactionDirection.BORROWED,
            paidAmount = 0.001, createdDate = -1L, dueDate = 0L
        ))
        assertRoundTripPreservesRecords()
        assertTrue(database.activityDao().getAllActivitiesList().isEmpty())
    }

    @Test fun omittedLegacyOptionalFieldsRemainRestorable() = runBlocking {
        val root = validFinancialBackup()
        root.getJSONObject("metadata").remove("createdAt")
        for (field in listOf("createdDate", "dueDate", "paidAmount", "settledAt", "note")) root.transaction().remove(field)
        for (field in listOf("createdAt", "transactionId", "amount", "direction", "reminderStage", "note")) root.activity().remove(field)
        assertTrue(PhittoosBackupManager.validateBackup(root.toString()).isSuccess)
        assertTrue(PhittoosBackupManager.restoreBackup(root.toString(), repository, preferences, database).isSuccess)
        val tx = database.transactionDao().getTransactionById(201)!!
        assertNull(tx.paidAmount)
        assertNull(tx.settledAt)
        assertNull(tx.dueDate)
        val activity = database.activityDao().getActivityById(301)!!
        assertNull(activity.transactionId)
        assertNull(activity.amount)
        assertNull(activity.direction)
    }

    @Test fun unlinkedNullableActivitiesRoundTripWithoutInventingHistory() = runBlocking {
        val friend = repository.insertFriend("Synthetic unlinked events")
        for (type in ActivityType.entries) {
            database.activityDao().insertActivity(ActivityEntity(type = type, friendId = friend, createdAt = 0L))
        }
        assertRoundTripPreservesRecords()
    }

    @Test fun repaymentEpsilonAndSubPaiseEventAmountsRemainRecoverable() = runBlocking {
        val friend = repository.insertFriend("Synthetic repayment tolerance")
        val final = repository.addTransaction(friend, 100.0, TransactionDirection.LENT, createdDate = 0L)
        assertTrue(repository.recordRepayment(final, 100.0001, settledAt = 0L) is com.professor1416.phittoos.data.repository.RepaymentResult.Success)
        val partial = repository.addTransaction(friend, 100.0, TransactionDirection.BORROWED, createdDate = 0L)
        assertTrue(repository.recordRepayment(partial, 0.001, settledAt = 0L) is com.professor1416.phittoos.data.repository.RepaymentResult.Success)
        assertEquals(0.0, database.transactionDao().getTransactionById(partial)!!.paidAmount!!, 0.0)
        assertRoundTripPreservesRecords()
    }

    @Test fun historicalReminderSnapshotSurvivesEligibleTransactionEdit() = runBlocking {
        val friend = repository.insertFriend("Synthetic reminder history")
        val id = repository.addTransaction(friend, 100.0, TransactionDirection.LENT, createdDate = 0L)
        repository.recordReminderSent(id, friend, ReminderStage.entries.first(), 100.0, TransactionDirection.LENT, timestamp = 0L)
        val edited = repository.updateOpenUnpaidTransaction(id, 50.0, TransactionDirection.BORROWED, createdDate = 0L)
        assertTrue(edited is com.professor1416.phittoos.data.repository.EditTransactionResult.Success)
        assertRoundTripPreservesRecords()
        val reminder = database.activityDao().getAllActivitiesList().single { it.type == ActivityType.REMINDER_SENT }
        assertEquals(100.0, reminder.amount!!, 0.0)
        assertEquals(TransactionDirection.LENT, reminder.direction)
    }

    @Test fun existingVersionAcceptanceIsCharacterizedWithoutChangingPolicy() {
        for ((value, accepted) in listOf(
            org.json.JSONObject.NULL to false, -1 to false, -2 to true, 0 to true,
            1 to true, 2 to false, 1.5 to true, "1" to true, "invalid" to false, true to false
        )) {
            val root = validFinancialBackup()
            root.getJSONObject("metadata").put("backupVersion", value)
            assertEquals("Version assessment only: $value", accepted, PhittoosBackupManager.validateBackup(root.toString()).isSuccess)
        }
        val root = validFinancialBackup()
        root.getJSONObject("metadata").remove("backupVersion")
        assertTrue(PhittoosBackupManager.validateBackup(root.toString()).isFailure)
    }


    @Test fun existingRejectionPathsPreserveAllRecordsAndPreferences() = runBlocking {
        val variants = listOf<Pair<String, (org.json.JSONObject) -> Unit>>(
            "duplicate friend" to { r -> r.getJSONArray("friends").put(org.json.JSONObject(r.getJSONArray("friends").getJSONObject(0).toString())) },
            "duplicate transaction" to { r -> r.getJSONArray("transactions").put(org.json.JSONObject(r.transaction().toString())) },
            "duplicate activity" to { r -> r.getJSONArray("activities").put(org.json.JSONObject(r.activity().toString())) },
            "missing friend" to { r -> r.transaction().put("friendId", 999) },
            "missing transaction" to { r -> r.activity().put("transactionId", 999) },
            "bad direction" to { r -> r.transaction().put("direction", "UNKNOWN") },
            "bad status" to { r -> r.transaction().put("status", "UNKNOWN") },
            "bad activity type" to { r -> r.activity().put("type", "UNKNOWN") },
            "bad reminder stage" to { r -> r.activity().put("reminderStage", "UNKNOWN") },
            "future version" to { r -> r.getJSONObject("metadata").put("backupVersion", 99) }
        )
        for ((label, mutate) in variants) {
            val root = validFinancialBackup()
            mutate(root)
            assertRejectedWithoutMutation(root.toString(), label)
        }
        assertRejectedWithoutMutation("{invalid", "malformed JSON")
        assertRejectedWithoutMutation("Friend,Amount\nSynthetic,100", "CSV")
    }

    @Test fun invalidPaidAndEventNumbersPreserveAllRecordsAndPreferences() = runBlocking {
        for (bad in listOf<Any>(-1.0, 1001.0, "NaN", "Infinity", false)) {
            val root = validFinancialBackup()
            root.transaction().put("paidAmount", bad)
            assertRejectedWithoutMutation(root.toString(), "paid=$bad")
        }
        for (bad in listOf<Any>(-1.0, 0.0, "NaN", "Infinity", false)) {
            val root = validFinancialBackup()
            root.activity().put("amount", bad)
            assertRejectedWithoutMutation(root.toString(), "event=$bad")
        }
    }

    @Test fun longBoundaryIdsAndHistoricalDatesRoundTripExactly() = runBlocking {
        database.friendDao().insertFriend(Friend(id = Long.MAX_VALUE, name = "Synthetic boundary ID"))
        database.transactionDao().insertTransaction(TransactionEntity(
            id = Long.MAX_VALUE, friendId = Long.MAX_VALUE, amount = 1.2345,
            direction = TransactionDirection.LENT, createdDate = Long.MIN_VALUE, dueDate = Long.MAX_VALUE
        ))
        database.activityDao().insertActivity(ActivityEntity(
            id = Long.MAX_VALUE, type = ActivityType.TRANSACTION_CREATED, friendId = Long.MAX_VALUE,
            transactionId = Long.MAX_VALUE, amount = 1.2345, direction = TransactionDirection.LENT, createdAt = Long.MIN_VALUE
        ))
        assertRoundTripPreservesRecords()
    }


    @Test fun skippedProfileBackupsRestoreBothEmptyAndPopulatedLedgers() = runBlocking {
        for (populated in listOf(false, true)) {
            repository.clearAllData()
            if (populated) {
                val friend = repository.insertFriend("Synthetic skipped profile")
                repository.addTransaction(friend, 12.34, TransactionDirection.BORROWED, createdDate = 0L)
            }
            // OnboardingScreen.completeAndGoHome permits Skip without assigning a name.
            preferences.userName = ""
            preferences.hasCompletedOnboarding = true
            preferences.remindersEnabled = false
            val friends = database.friendDao().getAllFriendsList()
            val transactions = database.transactionDao().getAllTransactionsOrdered()
            val activities = database.activityDao().getAllActivitiesList()
            val backup = PhittoosBackupManager.exportBackup(repository, preferences)
            val validation = PhittoosBackupManager.validateBackup(backup)
            assertTrue(validation.isSuccess)
            assertEquals("", validation.getOrThrow().profileName)
            preferences.userName = "Later synthetic profile"
            preferences.hasCompletedOnboarding = false
            preferences.remindersEnabled = true
            repository.clearAllData()
            assertTrue(PhittoosBackupManager.restoreBackup(backup, repository, preferences, database).isSuccess)
            assertEquals("", preferences.userName)
            assertTrue(preferences.hasCompletedOnboarding)
            assertFalse(preferences.remindersEnabled)
            assertEquals(friends, database.friendDao().getAllFriendsList())
            assertEquals(transactions, database.transactionDao().getAllTransactionsOrdered())
            assertEquals(activities, database.activityDao().getAllActivitiesList())
        }
    }

    @Test fun missingOrNonStringProfileNameIsRejectedWithoutMutation() = runBlocking {
        for (bad in listOf<Any>(org.json.JSONObject.NULL, false, 123, org.json.JSONObject())) {
            val root = validFinancialBackup()
            root.getJSONObject("preferences").put("userName", bad)
            assertRejectedWithoutMutation(root.toString(), "malformed profile $bad")
        }
        val root = validFinancialBackup()
        root.getJSONObject("preferences").remove("userName")
        assertRejectedWithoutMutation(root.toString(), "missing profile field")
    }

}
