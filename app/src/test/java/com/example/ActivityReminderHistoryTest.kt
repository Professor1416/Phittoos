package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.dao.ActivityDao
import com.example.data.dao.FriendDao
import com.example.data.dao.TransactionDao
import com.example.data.db.AppDatabase
import com.example.data.model.ActivityEntity
import com.example.data.model.ActivityType
import com.example.data.model.Friend
import com.example.data.model.TransactionDirection
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionStatus
import com.example.data.repository.PhittoosRepository
import com.example.ui.util.Formatters
import com.example.ui.viewmodel.ActivityViewModel
import kotlinx.coroutines.Dispatchers
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ActivityReminderHistoryTest {

    private lateinit var db: AppDatabase
    private lateinit var friendDao: FriendDao
    private lateinit var transactionDao: TransactionDao
    private lateinit var activityDao: ActivityDao
    private lateinit var repository: PhittoosRepository
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        friendDao = db.friendDao()
        transactionDao = db.transactionDao()
        activityDao = db.activityDao()
        repository = PhittoosRepository(friendDao, transactionDao, activityDao)
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    /**
     * TEST A: Creating a transaction logs a TRANSACTION_CREATED event
     */
    @Test
    fun testA_creatingTransactionLogsTransactionCreatedEvent() = runTest(testDispatcher) {
        val friendId = friendDao.insertFriend(Friend(name = "Rahul"))
        val txId = repository.addTransaction(
            friendId = friendId,
            amount = 500.0,
            direction = TransactionDirection.LENT,
            note = "Dinner"
        )

        val activities = activityDao.getAllActivities().first()
        assertEquals(1, activities.size)
        val event = activities[0]
        assertEquals(ActivityType.TRANSACTION_CREATED, event.type)
        assertEquals(friendId, event.friendId)
        assertEquals(txId, event.transactionId)
        assertEquals(500.0, event.amount ?: 0.0, 0.001)
        assertEquals("Dinner", event.note)
    }

    /**
     * TEST B: Lent transaction has direction = LENT
     */
    @Test
    fun testB_lentTransactionHasDirectionLent() = runTest(testDispatcher) {
        val friendId = friendDao.insertFriend(Friend(name = "Priya"))
        repository.addTransaction(
            friendId = friendId,
            amount = 1200.0,
            direction = TransactionDirection.LENT
        )

        val activities = activityDao.getAllActivities().first()
        assertEquals(1, activities.size)
        assertEquals(TransactionDirection.LENT, activities[0].direction)
    }

    /**
     * TEST C: Borrowed transaction has direction = BORROWED
     */
    @Test
    fun testC_borrowedTransactionHasDirectionBorrowed() = runTest(testDispatcher) {
        val friendId = friendDao.insertFriend(Friend(name = "Amit"))
        repository.addTransaction(
            friendId = friendId,
            amount = 800.0,
            direction = TransactionDirection.BORROWED
        )

        val activities = activityDao.getAllActivities().first()
        assertEquals(1, activities.size)
        assertEquals(TransactionDirection.BORROWED, activities[0].direction)
    }

    /**
     * TEST D: Partial repayment logs a PARTIAL_REPAYMENT event with correct repayment amount
     */
    @Test
    fun testD_partialRepaymentLogsPartialRepaymentEvent() = runTest(testDispatcher) {
        val friendId = friendDao.insertFriend(Friend(name = "Kunal"))
        val txId = repository.addTransaction(
            friendId = friendId,
            amount = 1000.0,
            direction = TransactionDirection.LENT
        )

        val result = repository.recordRepayment(txId, 400.0)
        assertTrue(result is com.example.data.repository.RepaymentResult.Success)

        val activities = activityDao.getAllActivities().first()
        // Should have: 1 TRANSACTION_CREATED + 1 PARTIAL_REPAYMENT
        assertEquals(2, activities.size)
        val repaymentEvent = activities.first { it.type == ActivityType.PARTIAL_REPAYMENT }
        assertEquals(400.0, repaymentEvent.amount ?: 0.0, 0.001)
        assertEquals(friendId, repaymentEvent.friendId)
        assertEquals(txId, repaymentEvent.transactionId)
        assertEquals(TransactionDirection.LENT, repaymentEvent.direction)
    }

    /**
     * TEST E: Full settlement logs a SETTLED event
     */
    @Test
    fun testE_fullSettlementLogsSettledEvent() = runTest(testDispatcher) {
        val friendId = friendDao.insertFriend(Friend(name = "Rohan"))
        val txId = repository.addTransaction(
            friendId = friendId,
            amount = 500.0,
            direction = TransactionDirection.LENT
        )

        repository.markTransactionAsPaid(txId)

        val activities = activityDao.getAllActivities().first()
        assertEquals(2, activities.size)
        val settledEvent = activities.first { it.type == ActivityType.SETTLED }
        assertEquals(500.0, settledEvent.amount ?: 0.0, 0.001)
        assertEquals(friendId, settledEvent.friendId)
        assertEquals(txId, settledEvent.transactionId)
    }

    /**
     * TEST F: Second manual settlement attempt does not log a duplicate SETTLED event (idempotency)
     */
    @Test
    fun testF_secondSettlementAttemptDoesNotLogDuplicateSettledEvent() = runTest(testDispatcher) {
        val friendId = friendDao.insertFriend(Friend(name = "Neha"))
        val txId = repository.addTransaction(
            friendId = friendId,
            amount = 600.0,
            direction = TransactionDirection.LENT
        )

        repository.markTransactionAsPaid(txId)
        val countAfterFirst = activityDao.getAllActivities().first().count { it.type == ActivityType.SETTLED }
        assertEquals(1, countAfterFirst)

        // Attempt second settlement on already-settled transaction
        repository.markTransactionAsPaid(txId)
        val countAfterSecond = activityDao.getAllActivities().first().count { it.type == ActivityType.SETTLED }
        assertEquals(1, countAfterSecond)
    }

    /**
     * TEST G: Activity screen displays events in reverse chronological order
     */
    @Test
    fun testG_activityEventsInReverseChronologicalOrder() = runTest(testDispatcher) {
        val friendId = friendDao.insertFriend(Friend(name = "Dev"))
        activityDao.insertActivity(
            ActivityEntity(
                type = ActivityType.TRANSACTION_CREATED,
                friendId = friendId,
                amount = 100.0,
                createdAt = 1000L
            )
        )
        activityDao.insertActivity(
            ActivityEntity(
                type = ActivityType.TRANSACTION_CREATED,
                friendId = friendId,
                amount = 200.0,
                createdAt = 2000L
            )
        )
        activityDao.insertActivity(
            ActivityEntity(
                type = ActivityType.TRANSACTION_CREATED,
                friendId = friendId,
                amount = 300.0,
                createdAt = 3000L
            )
        )

        val activities = activityDao.getAllActivities().first()
        assertEquals(3, activities.size)
        assertEquals(3000L, activities[0].createdAt)
        assertEquals(2000L, activities[1].createdAt)
        assertEquals(1000L, activities[2].createdAt)
    }

    /**
     * TEST H: Needs Attention section shows overdue transactions with non-zero remaining balance
     */
    @Test
    fun testH_needsAttentionShowsOverdueWithNonZeroBalance() = runTest(testDispatcher) {
        val friendId = friendDao.insertFriend(Friend(name = "Vikas"))
        val yesterday = System.currentTimeMillis() - 86_400_000L * 2

        transactionDao.insertTransaction(
            TransactionEntity(
                friendId = friendId,
                amount = 1500.0,
                direction = TransactionDirection.LENT,
                dueDate = yesterday,
                status = TransactionStatus.OPEN
            )
        )

        val needsAttention = repository.needsAttentionItems.first()
        assertEquals(1, needsAttention.size)
        assertEquals("Vikas", needsAttention[0].friendName)
        assertEquals(1500.0, needsAttention[0].remainingAmount, 0.001)
        assertTrue(needsAttention[0].overdueDays >= 1)
    }

    /**
     * TEST I: Overdue transaction with remaining balance = 0 does not appear in Needs Attention
     */
    @Test
    fun testI_overdueWithZeroRemainingDoesNotAppearInNeedsAttention() = runTest(testDispatcher) {
        val friendId = friendDao.insertFriend(Friend(name = "Suresh"))
        val yesterday = System.currentTimeMillis() - 86_400_000L * 2

        transactionDao.insertTransaction(
            TransactionEntity(
                friendId = friendId,
                amount = 1000.0,
                direction = TransactionDirection.LENT,
                dueDate = yesterday,
                status = TransactionStatus.CONFIRMED,
                paidAmount = 1000.0,
                settledAt = System.currentTimeMillis()
            )
        )

        val needsAttention = repository.needsAttentionItems.first()
        assertTrue(needsAttention.isEmpty())
    }

    /**
     * TEST J: Partially repaid overdue transaction shows the remaining balance, not original amount
     */
    @Test
    fun testJ_partiallyRepaidOverdueShowsRemainingBalance() = runTest(testDispatcher) {
        val friendId = friendDao.insertFriend(Friend(name = "Manish"))
        val yesterday = System.currentTimeMillis() - 86_400_000L * 3

        val txId = transactionDao.insertTransaction(
            TransactionEntity(
                friendId = friendId,
                amount = 2000.0,
                direction = TransactionDirection.LENT,
                dueDate = yesterday,
                status = TransactionStatus.OPEN,
                paidAmount = 500.0
            )
        )

        val needsAttention = repository.needsAttentionItems.first()
        assertEquals(1, needsAttention.size)
        assertEquals(1500.0, needsAttention[0].remainingAmount, 0.001)
    }

    /**
     * TEST K: Settling an overdue transaction removes it from Needs Attention immediately
     */
    @Test
    fun testK_settlingOverdueTransactionRemovesFromNeedsAttention() = runTest(testDispatcher) {
        val friendId = friendDao.insertFriend(Friend(name = "Gaurav"))
        val yesterday = System.currentTimeMillis() - 86_400_000L * 2

        val txId = transactionDao.insertTransaction(
            TransactionEntity(
                friendId = friendId,
                amount = 750.0,
                direction = TransactionDirection.LENT,
                dueDate = yesterday,
                status = TransactionStatus.OPEN
            )
        )

        assertEquals(1, repository.needsAttentionItems.first().size)

        repository.markTransactionAsPaid(txId)

        assertEquals(0, repository.needsAttentionItems.first().size)
    }

    /**
     * TEST L: Deleting a friend cascades and removes associated activity entries (or keeps DB consistent)
     */
    @Test
    fun testL_deletingFriendCascadesAndRemovesActivityEntries() = runTest(testDispatcher) {
        val friendId = friendDao.insertFriend(Friend(name = "Sameer"))
        repository.addTransaction(
            friendId = friendId,
            amount = 450.0,
            direction = TransactionDirection.LENT
        )

        assertEquals(1, activityDao.getAllActivities().first().size)

        val friend = friendDao.getFriendById(friendId).first()
        assertNotNull(friend)
        friendDao.deleteFriend(friend!!)

        assertEquals(0, activityDao.getAllActivities().first().size)
    }

    /**
     * TEST M: Activity events for multiple friends appear in unified chronological feed
     */
    @Test
    fun testM_activityEventsForMultipleFriendsInUnifiedChronologicalFeed() = runTest(testDispatcher) {
        val f1 = friendDao.insertFriend(Friend(name = "Friend 1"))
        val f2 = friendDao.insertFriend(Friend(name = "Friend 2"))
        val f3 = friendDao.insertFriend(Friend(name = "Friend 3"))

        repository.addTransaction(f1, 100.0, TransactionDirection.LENT)
        repository.addTransaction(f2, 200.0, TransactionDirection.BORROWED)
        repository.addTransaction(f3, 300.0, TransactionDirection.LENT)

        val activities = repository.activitiesWithFriend.first()
        assertEquals(3, activities.size)
        // Check friends are associated properly
        val friendNames = activities.map { it.friendName }.toSet()
        assertTrue(friendNames.contains("Friend 1"))
        assertTrue(friendNames.contains("Friend 2"))
        assertTrue(friendNames.contains("Friend 3"))
    }

    /**
     * TEST N: Timestamps format correctly: 'Today', 'Yesterday', 'd MMM'
     */
    @Test
    fun testN_timestampsFormatCorrectly() {
        val tz = TimeZone.getTimeZone("UTC")
        val calendar = Calendar.getInstance(tz).apply {
            set(2026, Calendar.SEPTEMBER, 15, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nowMillis = calendar.timeInMillis

        // Same day -> "Today"
        val todayStr = Formatters.formatDate(nowMillis, nowMillis = nowMillis, timeZone = tz)
        assertEquals("Today", todayStr)

        // Yesterday -> "Yesterday"
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayMillis = calendar.timeInMillis
        val yesterdayStr = Formatters.formatDate(yesterdayMillis, nowMillis = nowMillis, timeZone = tz)
        assertEquals("Yesterday", yesterdayStr)

        // 10 days ago in same year -> "5 Sep"
        calendar.set(2026, Calendar.SEPTEMBER, 5, 10, 0, 0)
        val sep5Millis = calendar.timeInMillis
        val sep5Str = Formatters.formatDate(sep5Millis, nowMillis = nowMillis, timeZone = tz)
        assertEquals("5 Sep", sep5Str)
    }

    /**
     * TEST O: Database migration test: existing DB with version 2 upgrades to version 3 without data loss
     */
    @Test
    fun testO_migrationFrom2To3PreservesData() = runTest(testDispatcher) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbFile = context.getDatabasePath("test_migration_2_3.db")
        dbFile.delete()

        // 1. Create a DB at version 2 manually
        val helperFactory = FrameworkSQLiteOpenHelperFactory()
        val config = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name("test_migration_2_3.db")
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(2) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `friends` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `contact_info` TEXT, `reliability_tier` TEXT NOT NULL)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `friend_id` INTEGER NOT NULL, `amount` REAL NOT NULL, `direction` TEXT NOT NULL, `note` TEXT, `created_date` INTEGER NOT NULL, `due_date` INTEGER, `status` TEXT NOT NULL, `paid_amount` REAL, `settled_at` INTEGER, FOREIGN KEY(`friend_id`) REFERENCES `friends`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_friend_id` ON `transactions` (`friend_id`)")
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()
        val helper = helperFactory.create(config)
        val sqliteDb = helper.writableDatabase

        // Insert legacy data into v2
        sqliteDb.execSQL("INSERT INTO friends (id, name, contact_info, reliability_tier) VALUES (1, 'LegacyFriend', '9999999999', 'HIGH')")
        sqliteDb.execSQL("INSERT INTO transactions (id, friend_id, amount, direction, note, created_date, due_date, status, paid_amount, settled_at) VALUES (1, 1, 750.0, 'LENT', 'LegacyTx', 1000, NULL, 'OPEN', 0.0, NULL)")
        sqliteDb.close()

        // 2. Open with Room specifying MIGRATION_2_3
        val upgradedDb = Room.databaseBuilder(context, AppDatabase::class.java, "test_migration_2_3.db")
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .allowMainThreadQueries()
            .build()

        // 3. Verify legacy data is intact
        val friends = upgradedDb.friendDao().getFriendByName("LegacyFriend")
        assertNotNull(friends)
        assertEquals("LegacyFriend", friends?.name)
        assertEquals("HIGH", friends?.reliabilityTier)

        val txs = upgradedDb.transactionDao().getTransactionById(1)
        assertNotNull(txs)
        assertEquals(750.0, txs?.amount ?: 0.0, 0.001)

        // 4. Verify activityDao works on version 3
        upgradedDb.activityDao().insertActivity(
            ActivityEntity(
                type = ActivityType.TRANSACTION_CREATED,
                friendId = 1,
                amount = 750.0,
                createdAt = 2000L
            )
        )
        val acts = upgradedDb.activityDao().getActivitiesForFriendDirect(1)
        assertEquals(1, acts.size)
        assertEquals(750.0, acts[0].amount ?: 0.0, 0.001)

        upgradedDb.close()
        dbFile.delete()
    }

    /**
     * TEST P: Activity DAO: querying by friend_id returns only that friend's events
     */
    @Test
    fun testP_activityDaoQueryingByFriendIdReturnsOnlyThatFriend() = runTest(testDispatcher) {
        val f1 = friendDao.insertFriend(Friend(name = "TargetFriend"))
        val f2 = friendDao.insertFriend(Friend(name = "OtherFriend"))

        activityDao.insertActivity(ActivityEntity(type = ActivityType.TRANSACTION_CREATED, friendId = f1, amount = 100.0))
        activityDao.insertActivity(ActivityEntity(type = ActivityType.PARTIAL_REPAYMENT, friendId = f1, amount = 50.0))
        activityDao.insertActivity(ActivityEntity(type = ActivityType.TRANSACTION_CREATED, friendId = f2, amount = 200.0))

        val f1Activities = activityDao.getActivitiesForFriend(f1).first()
        assertEquals(2, f1Activities.size)
        assertTrue(f1Activities.all { it.friendId == f1 })

        val f2Activities = activityDao.getActivitiesForFriend(f2).first()
        assertEquals(1, f2Activities.size)
        assertEquals(f2, f2Activities[0].friendId)
    }

    /**
     * TEST Q: Activity DAO: limit query returns at most N events
     */
    @Test
    fun testQ_activityDaoLimitQueryReturnsAtMostNEvents() = runTest(testDispatcher) {
        val friendId = friendDao.insertFriend(Friend(name = "LimitTest"))
        for (i in 1..10) {
            activityDao.insertActivity(
                ActivityEntity(
                    type = ActivityType.TRANSACTION_CREATED,
                    friendId = friendId,
                    amount = i * 100.0,
                    createdAt = i * 1000L
                )
            )
        }

        val limited = activityDao.getRecentActivities(3).first()
        assertEquals(3, limited.size)
        // Most recent first: 10000L, 9000L, 8000L
        assertEquals(10000L, limited[0].createdAt)
        assertEquals(9000L, limited[1].createdAt)
        assertEquals(8000L, limited[2].createdAt)
    }
}
