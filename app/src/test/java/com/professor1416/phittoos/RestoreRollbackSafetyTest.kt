package com.professor1416.phittoos

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.professor1416.phittoos.data.dao.ActivityDao
import com.professor1416.phittoos.data.dao.FriendDao
import com.professor1416.phittoos.data.db.AppDatabase
import com.professor1416.phittoos.data.model.ActivityEntity
import com.professor1416.phittoos.data.model.ActivityType
import com.professor1416.phittoos.data.model.Friend
import com.professor1416.phittoos.data.model.TransactionDirection
import com.professor1416.phittoos.data.model.TransactionEntity
import com.professor1416.phittoos.data.model.TransactionStatus
import com.professor1416.phittoos.data.preferences.AppThemeMode
import com.professor1416.phittoos.data.preferences.UserPreferences
import com.professor1416.phittoos.data.repository.PhittoosRepository
import com.professor1416.phittoos.data.repository.RepaymentResult
import com.professor1416.phittoos.export.PhittoosBackupManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

/** Real on-disk Room rollback tests; all data and fault-injection delegates are test-owned. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RestoreRollbackSafetyTest {
    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var preferences: UserPreferences
    private lateinit var databaseName: String
    private var failTransactionCompletion: (() -> Unit)? = null

    private data class Ledger(
        val friends: List<Friend>,
        val transactions: List<TransactionEntity>,
        val activities: List<ActivityEntity>
    )

    private class InjectedRestoreFailure : IllegalStateException("Synthetic restore insertion failure")

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        databaseName = "synthetic-restore-${UUID.randomUUID()}.db"
        database = openDatabase()
        preferences = UserPreferences(context)
        preferences.clearAll()
        preferences.userName = "Existing synthetic owner"
        preferences.hasCompletedOnboarding = false
        preferences.remindersEnabled = false
        preferences.themeMode = AppThemeMode.DARK
        preferences.lastBackupTime = 123456L
    }

    private fun openDatabase(): AppDatabase = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
        .openHelperFactory { configuration ->
            val helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
            fun wrap(actual: SupportSQLiteDatabase): SupportSQLiteDatabase =
                object : SupportSQLiteDatabase by actual {
                    override fun setTransactionSuccessful() {
                        // One-shot fault before Room can mark the real SQLite transaction successful.
                        // All queries, rollback and persistence still use the real database.
                        val failure = failTransactionCompletion
                        if (failure != null) {
                            failTransactionCompletion = null
                            assertTrue(actual.inTransaction())
                            failure()
                        }
                        actual.setTransactionSuccessful()
                    }
                }
            object : SupportSQLiteOpenHelper by helper {
                override val writableDatabase: SupportSQLiteDatabase get() = wrap(helper.writableDatabase)
                override val readableDatabase: SupportSQLiteDatabase get() = wrap(helper.readableDatabase)
            }
        }
        .allowMainThreadQueries()
        .build()

    private fun repository(friendDao: FriendDao = database.friendDao(), activityDao: ActivityDao = database.activityDao()) =
        PhittoosRepository(friendDao, database.transactionDao(), activityDao, database)

    private suspend fun ledger() = Ledger(
        database.friendDao().getAllFriendsList(),
        database.transactionDao().getAllTransactionsOrdered(),
        database.activityDao().getAllActivitiesList()
    )

    private fun preferenceSnapshot() = context.getSharedPreferences("phittoos_prefs", Context.MODE_PRIVATE).all.toMap()

    private suspend fun seedExistingLedger(): Ledger {
        val repo = repository()
        val first = repo.insertFriend("Synthetic original A")
        val second = repo.insertFriend("Synthetic original B")
        val partial = repo.addTransaction(first, 1000.0, TransactionDirection.LENT, "Retain partial", dueDate = 100L, createdDate = 0L)
        assertTrue(repo.recordRepayment(partial, 400.0, settledAt = 200L) is RepaymentResult.Success)
        val settled = repo.addTransaction(second, 250.50, TransactionDirection.BORROWED, "Retain settled", createdDate = 1L)
        assertTrue(repo.markTransactionAsPaid(settled, settledAt = 300L).success)
        repo.addTransaction(first, 50.25, TransactionDirection.BORROWED, "Retain open", createdDate = 2L)
        return ledger().also {
            assertEquals(2, it.friends.size)
            assertEquals(3, it.transactions.size)
            assertEquals(5, it.activities.size)
            assertTrue(it.transactions.any { tx -> tx.status == TransactionStatus.OPEN && tx.paidAmount == 400.0 })
            assertTrue(it.transactions.any { tx -> tx.status == TransactionStatus.CONFIRMED && tx.settledAt == 300L })
        }
    }

    private fun importedBackup(): String = """
        {
          "metadata": {"backupVersion": 1, "createdAt": 1000},
          "preferences": {"userName": "Imported synthetic owner", "hasCompletedOnboarding": true, "remindersEnabled": true},
          "friends": [{"id": 101, "name": "Synthetic imported A"}, {"id": 102, "name": "Synthetic imported B"}],
          "transactions": [
            {"id": 201, "friendId": 101, "amount": 500, "paidAmount": 100, "direction": "LENT", "status": "OPEN", "createdDate": 10, "dueDate": 20},
            {"id": 202, "friendId": 102, "amount": 75.1234, "paidAmount": null, "direction": "BORROWED", "status": "CONFIRMED", "createdDate": 11, "settledAt": null}
          ],
          "activities": [
            {"id": 301, "type": "TRANSACTION_CREATED", "friendId": 101, "transactionId": 201, "amount": 500, "direction": "LENT", "createdAt": 10},
            {"id": 302, "type": "PARTIAL_REPAYMENT", "friendId": 101, "transactionId": 201, "amount": 100, "direction": "LENT", "createdAt": 12}
          ]
        }
    """.trimIndent()

    private suspend fun reopenAndAssert(expected: Ledger, expectedPreferences: Map<String, *>) {
        val expectedProfile = Triple(preferences.userName, preferences.hasCompletedOnboarding, preferences.remindersEnabled)
        database.close()
        database = openDatabase()
        assertEquals("Exact financial state must survive close/reopen", expected, ledger())
        assertEquals(expectedPreferences, preferenceSnapshot())
        val freshPreferences = UserPreferences(context)
        assertEquals(expectedProfile.first, freshPreferences.userName)
        assertEquals(expectedProfile.second, freshPreferences.hasCompletedOnboarding)
        assertEquals(expectedProfile.third, freshPreferences.remindersEnabled)
        assertEquals(AppThemeMode.DARK, freshPreferences.themeMode)
        assertEquals(123456L, freshPreferences.lastBackupTime)
    }

    private suspend fun assertSuccessfulReplacement(backup: String) {
        assertTrue(PhittoosBackupManager.restoreBackup(backup, repository(), preferences, database).isSuccess)
        val expected = Ledger(
            listOf(Friend(id = 101, name = "Synthetic imported A"), Friend(id = 102, name = "Synthetic imported B")),
            listOf(
                TransactionEntity(id = 201, friendId = 101, amount = 500.0, paidAmount = 100.0, direction = TransactionDirection.LENT, createdDate = 10L, dueDate = 20L),
                TransactionEntity(id = 202, friendId = 102, amount = 75.1234, direction = TransactionDirection.BORROWED, status = TransactionStatus.CONFIRMED, createdDate = 11L)
            ),
            listOf(
                ActivityEntity(id = 302, type = ActivityType.PARTIAL_REPAYMENT, friendId = 101, transactionId = 201, amount = 100.0, direction = TransactionDirection.LENT, createdAt = 12L),
                ActivityEntity(id = 301, type = ActivityType.TRANSACTION_CREATED, friendId = 101, transactionId = 201, amount = 500.0, direction = TransactionDirection.LENT, createdAt = 10L)
            )
        )
        assertEquals("Successful retry must replace the complete ledger exactly", expected, ledger())
        assertEquals("Imported synthetic owner", preferences.userName)
        assertTrue(preferences.hasCompletedOnboarding)
        assertTrue(preferences.remindersEnabled)
        assertEquals(AppThemeMode.DARK, preferences.themeMode)
        assertEquals(123456L, preferences.lastBackupTime)
        reopenAndAssert(expected, preferenceSnapshot())
    }

    @Test fun failureAfterAllDeletesRollsBackAndValidRetrySucceeds() = runBlocking {
        val before = seedExistingLedger()
        val beforePreferences = preferenceSnapshot()
        val backup = importedBackup()
        assertTrue("Injection input must pass validation", PhittoosBackupManager.validateBackup(backup).isSuccess)
        var injected = false
        val failure = InjectedRestoreFailure()
        val realFriends = database.friendDao()
        val failingFriends = object : FriendDao by realFriends {
            override suspend fun insertFriend(friend: Friend): Long {
                assertTrue("Replacement must already be inside Room's transaction", database.inTransaction())
                assertEquals(Ledger(emptyList(), emptyList(), emptyList()), ledger())
                assertEquals(beforePreferences, preferenceSnapshot())
                injected = true
                throw failure
            }
        }
        val result = PhittoosBackupManager.restoreBackup(backup, repository(friendDao = failingFriends), preferences, database)
        assertTrue("Fault must occur after the destructive SQL, not validation", injected)
        assertTrue(result.isFailure)
        assertSame(failure, result.exceptionOrNull())
        assertFalse(database.inTransaction())
        assertEquals(before, ledger())
        assertEquals(beforePreferences, preferenceSnapshot())
        reopenAndAssert(before, beforePreferences)
        assertSuccessfulReplacement(backup)
    }

    @Test fun failureDuringLaterActivityInsertRollsBackAllImportedRowsAndHistory() = runBlocking {
        val before = seedExistingLedger()
        val beforePreferences = preferenceSnapshot()
        val backup = importedBackup()
        assertTrue(PhittoosBackupManager.validateBackup(backup).isSuccess)
        var insertedActivities = 0
        var injected = false
        val failure = InjectedRestoreFailure()
        val realActivities = database.activityDao()
        val failingActivities = object : ActivityDao by realActivities {
            override suspend fun insertActivity(activity: ActivityEntity): Long {
                if (activity.id == 302L) {
                    assertTrue(database.inTransaction())
                    val partial = ledger()
                    assertEquals(setOf(101L, 102L), partial.friends.map { it.id }.toSet())
                    assertEquals(setOf(201L, 202L), partial.transactions.map { it.id }.toSet())
                    assertEquals(listOf(301L), partial.activities.map { it.id })
                    assertEquals(1, insertedActivities)
                    assertEquals(beforePreferences, preferenceSnapshot())
                    injected = true
                    throw failure
                }
                return realActivities.insertActivity(activity).also { insertedActivities++ }
            }
        }
        val result = PhittoosBackupManager.restoreBackup(backup, repository(activityDao = failingActivities), preferences, database)
        assertTrue(injected)
        assertTrue(result.isFailure)
        assertSame(failure, result.exceptionOrNull())
        assertFalse(database.inTransaction())
        assertEquals("No imported IDs or changed original fields may survive", before, ledger())
        assertEquals(beforePreferences, preferenceSnapshot())
        reopenAndAssert(before, beforePreferences)
        assertSuccessfulReplacement(backup)
    }


    @Test fun completionFailureRollsBackLedgerWithoutChangingPreferences() = runBlocking {
        val before = seedExistingLedger()
        val beforePreferences = preferenceSnapshot()
        val backup = importedBackup()
        assertTrue(PhittoosBackupManager.validateBackup(backup).isSuccess)
        var injected = false
        var preferencesAtCompletion: Map<String, *>? = null
        val failure = InjectedRestoreFailure()
        val realActivities = database.activityDao()
        val armAfterLastInsert = object : ActivityDao by realActivities {
            override suspend fun insertActivity(activity: ActivityEntity): Long {
                val id = realActivities.insertActivity(activity)
                if (activity.id == 302L) {
                    assertTrue(database.inTransaction())
                    val imported = ledger()
                    assertEquals(setOf(101L, 102L), imported.friends.map { it.id }.toSet())
                    assertEquals(setOf(201L, 202L), imported.transactions.map { it.id }.toSet())
                    assertEquals(listOf(302L, 301L), imported.activities.map { it.id })
                    assertEquals(beforePreferences, preferenceSnapshot())
                    // DAO-owned nested work is complete before arming the outer completion fault.
                    failTransactionCompletion = {
                        preferencesAtCompletion = preferenceSnapshot()
                        injected = true
                        throw failure
                    }
                }
                return id
            }
        }
        val result = PhittoosBackupManager.restoreBackup(backup, repository(activityDao = armAfterLastInsert), preferences, database)
        assertTrue("Failure must reach the outer transaction success-mark boundary", injected)
        assertTrue(result.isFailure)
        assertSame(failure, result.exceptionOrNull())
        assertFalse(database.inTransaction())
        assertEquals("SQLite rollback must retain every original row", before, ledger())
        assertEquals("Preferences must not change before successful Room completion", beforePreferences, preferencesAtCompletion)
        assertEquals(beforePreferences, preferenceSnapshot())
        reopenAndAssert(before, beforePreferences)
        assertSuccessfulReplacement(backup)
    }

    @After fun tearDown() {
        if (::database.isInitialized) database.close()
        if (::databaseName.isInitialized) context.deleteDatabase(databaseName)
        if (::preferences.isInitialized) preferences.clearAll()
    }
}
