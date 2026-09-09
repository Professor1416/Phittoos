package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.example.data.dao.ActivityDao
import com.example.data.dao.FriendDao
import com.example.data.dao.TransactionDao
import com.example.data.db.AppDatabase
import com.example.data.model.ActivityEntity
import com.example.data.model.ActivityType
import com.example.data.model.Friend
import com.example.data.model.ReminderStage
import com.example.data.model.TransactionDirection
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionStatus
import com.example.data.repository.PhittoosRepository
import com.example.domain.DueDateHelper
import com.example.reminder.ReminderNotificationHelper
import com.example.reminder.SmartReminderEngine
import com.example.reminder.SmartReminderWorker
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
class SmartReminderEngineTest {

    private lateinit var db: AppDatabase
    private lateinit var friendDao: FriendDao
    private lateinit var transactionDao: TransactionDao
    private lateinit var activityDao: ActivityDao
    private lateinit var repository: PhittoosRepository
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
    private val tz = TimeZone.getTimeZone("UTC")

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

    private fun getMillisForDaysAgo(nowMillis: Long, daysAgo: Int): Long {
        val cal = Calendar.getInstance(tz).apply {
            timeInMillis = nowMillis
            add(Calendar.DAY_OF_YEAR, -daysAgo)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun getNowMillis(): Long {
        val cal = Calendar.getInstance(tz).apply {
            set(2026, Calendar.SEPTEMBER, 15, 14, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    // A. Overdue LENT transaction at exactly 7 days -> sends DAY_7 reminder
    @Test
    fun testA_overdueLentAt7Days_sendsDay7Reminder() = runTest(testDispatcher) {
        val friendId = friendDao.insertFriend(Friend(name = "Aman"))
        val now = getNowMillis()
        val due7DaysAgo = getMillisForDaysAgo(now, 7)
        val txId = transactionDao.insertTransaction(
            TransactionEntity(
                friendId = friendId,
                amount = 1000.0,
                direction = TransactionDirection.LENT,
                createdDate = due7DaysAgo - 86400000L,
                dueDate = due7DaysAgo,
                status = TransactionStatus.OPEN
            )
        )

        val notifications = mutableListOf<Triple<String, Double, ReminderStage>>()
        val summary = SmartReminderEngine.checkAndSendReminders(
            repository = repository,
            context = ApplicationProvider.getApplicationContext(),
            nowMillis = now,
            timeZone = tz,
            notificationSender = { friendName, amount, stage, _, _ ->
                notifications.add(Triple(friendName, amount, stage))
                true
            }
        )

        assertEquals(1, summary.remindersSentCount)
        assertEquals(1, notifications.size)
        assertEquals("Aman", notifications[0].first)
        assertEquals(1000.0, notifications[0].second, 0.001)
        assertEquals(ReminderStage.DAY_7, notifications[0].third)

        val sentStages = repository.getSentReminderStagesForTransaction(txId)
        assertTrue(sentStages.contains(ReminderStage.DAY_7))
    }

    // B. Overdue LENT transaction at 8 days without prior reminder -> sends DAY_7 reminder
    @Test
    fun testB_overdueLentAt8DaysWithoutPrior_sendsDay7() = runTest(testDispatcher) {
        val friendId = friendDao.insertFriend(Friend(name = "Aman"))
        val now = getNowMillis()
        val due8DaysAgo = getMillisForDaysAgo(now, 8)
        val txId = transactionDao.insertTransaction(
            TransactionEntity(
                friendId = friendId,
                amount = 500.0,
                direction = TransactionDirection.LENT,
                createdDate = due8DaysAgo - 86400000L,
                dueDate = due8DaysAgo,
                status = TransactionStatus.OPEN
            )
        )

        var sentStage: ReminderStage? = null
        SmartReminderEngine.checkAndSendReminders(
            repository = repository,
            context = ApplicationProvider.getApplicationContext(),
            nowMillis = now,
            timeZone = tz,
            notificationSender = { _, _, stage, _, _ ->
                sentStage = stage
                true
            }
        )

        assertEquals(ReminderStage.DAY_7, sentStage)
    }

    // C. Overdue LENT transaction at 8 days WITH DAY_7 already sent -> sends NO reminder
    @Test
    fun testC_overdueLentAt8DaysWithDay7Sent_sendsNoReminder() = runTest(testDispatcher) {
        val friendId = friendDao.insertFriend(Friend(name = "Aman"))
        val now = getNowMillis()
        val due8DaysAgo = getMillisForDaysAgo(now, 8)
        val txId = transactionDao.insertTransaction(
            TransactionEntity(
                friendId = friendId,
                amount = 500.0,
                direction = TransactionDirection.LENT,
                createdDate = due8DaysAgo - 86400000L,
                dueDate = due8DaysAgo,
                status = TransactionStatus.OPEN
            )
        )
        // Mark DAY_7 as already sent
        repository.recordReminderSent(txId, friendId, ReminderStage.DAY_7, 500.0)

        var sentCount = 0
        SmartReminderEngine.checkAndSendReminders(
            repository = repository,
            context = ApplicationProvider.getApplicationContext(),
            nowMillis = now,
            timeZone = tz,
            notificationSender = { _, _, _, _, _ ->
                sentCount++
                true
            }
        )

        assertEquals(0, sentCount)
    }

    // D. Overdue LENT transaction at 15 days WITH DAY_7 already sent -> sends DAY_15 reminder
    @Test
    fun testD_overdueLentAt15DaysWithDay7Sent_sendsDay15() = runTest(testDispatcher) {
        val friendId = friendDao.insertFriend(Friend(name = "Rohit"))
        val now = getNowMillis()
        val due15DaysAgo = getMillisForDaysAgo(now, 15)
        val txId = transactionDao.insertTransaction(
            TransactionEntity(
                friendId = friendId,
                amount = 2000.0,
                direction = TransactionDirection.LENT,
                createdDate = due15DaysAgo - 86400000L,
                dueDate = due15DaysAgo,
                status = TransactionStatus.OPEN
            )
        )
        repository.recordReminderSent(txId, friendId, ReminderStage.DAY_7, 2000.0)

        var sentStage: ReminderStage? = null
        SmartReminderEngine.checkAndSendReminders(
            repository = repository,
            context = ApplicationProvider.getApplicationContext(),
            nowMillis = now,
            timeZone = tz,
            notificationSender = { _, _, stage, _, _ ->
                sentStage = stage
                true
            }
        )

        assertEquals(ReminderStage.DAY_15, sentStage)
        val allSent = repository.getSentReminderStagesForTransaction(txId)
        assertTrue(allSent.contains(ReminderStage.DAY_7))
        assertTrue(allSent.contains(ReminderStage.DAY_15))
    }

    // E. Overdue LENT transaction at 30 days WITH DAY_15 already sent -> sends DAY_30 reminder
    @Test
    fun testE_overdueLentAt30DaysWithDay15Sent_sendsDay30() = runTest(testDispatcher) {
        val friendId = friendDao.insertFriend(Friend(name = "Rohit"))
        val now = getNowMillis()
        val due30DaysAgo = getMillisForDaysAgo(now, 30)
        val txId = transactionDao.insertTransaction(
            TransactionEntity(
                friendId = friendId,
                amount = 2000.0,
                direction = TransactionDirection.LENT,
                createdDate = due30DaysAgo - 86400000L,
                dueDate = due30DaysAgo,
                status = TransactionStatus.OPEN
            )
        )
        repository.recordReminderSent(txId, friendId, ReminderStage.DAY_7, 2000.0)
        repository.recordReminderSent(txId, friendId, ReminderStage.DAY_15, 2000.0)

        var sentStage: ReminderStage? = null
        SmartReminderEngine.checkAndSendReminders(
            repository = repository,
            context = ApplicationProvider.getApplicationContext(),
            nowMillis = now,
            timeZone = tz,
            notificationSender = { _, _, stage, _, _ ->
                sentStage = stage
                true
            }
        )

        assertEquals(ReminderStage.DAY_30, sentStage)
    }

    // F. Overdue LENT transaction at 31 days with all 3 stages sent -> sends NO reminder
    @Test
    fun testF_overdueLentAt31DaysAllSent_sendsNoReminder() = runTest(testDispatcher) {
        val friendId = friendDao.insertFriend(Friend(name = "Rohit"))
        val now = getNowMillis()
        val due31DaysAgo = getMillisForDaysAgo(now, 31)
        val txId = transactionDao.insertTransaction(
            TransactionEntity(
                friendId = friendId,
                amount = 2000.0,
                direction = TransactionDirection.LENT,
                createdDate = due31DaysAgo - 86400000L,
                dueDate = due31DaysAgo,
                status = TransactionStatus.OPEN
            )
        )
        repository.recordReminderSent(txId, friendId, ReminderStage.DAY_7, 2000.0)
        repository.recordReminderSent(txId, friendId, ReminderStage.DAY_15, 2000.0)
        repository.recordReminderSent(txId, friendId, ReminderStage.DAY_30, 2000.0)

        var sentCount = 0
        SmartReminderEngine.checkAndSendReminders(
            repository = repository,
            context = ApplicationProvider.getApplicationContext(),
            nowMillis = now,
            timeZone = tz,
            notificationSender = { _, _, _, _, _ ->
                sentCount++
                true
            }
        )

        assertEquals(0, sentCount)
    }

    // G. Overdue LENT transaction at 18 days with NO prior reminder -> sends DAY_15 only (highest unsent stage due)
    @Test
    fun testG_overdueLentAt18DaysNoPrior_sendsDay15Only() = runTest(testDispatcher) {
        val friendId = friendDao.insertFriend(Friend(name = "Suresh"))
        val now = getNowMillis()
        val due18DaysAgo = getMillisForDaysAgo(now, 18)
        val txId = transactionDao.insertTransaction(
            TransactionEntity(
                friendId = friendId,
                amount = 1500.0,
                direction = TransactionDirection.LENT,
                createdDate = due18DaysAgo - 86400000L,
                dueDate = due18DaysAgo,
                status = TransactionStatus.OPEN
            )
        )

        val sentStages = mutableListOf<ReminderStage>()
        SmartReminderEngine.checkAndSendReminders(
            repository = repository,
            context = ApplicationProvider.getApplicationContext(),
            nowMillis = now,
            timeZone = tz,
            notificationSender = { _, _, stage, _, _ ->
                sentStages.add(stage)
                true
            }
        )

        assertEquals(1, sentStages.size)
        assertEquals(ReminderStage.DAY_15, sentStages[0])
        assertFalse(sentStages.contains(ReminderStage.DAY_7))
    }

    // H. Overdue LENT transaction at 31 days with NO prior reminder -> sends DAY_30 only
    @Test
    fun testH_overdueLentAt31DaysNoPrior_sendsDay30Only() = runTest(testDispatcher) {
        val friendId = friendDao.insertFriend(Friend(name = "Suresh"))
        val now = getNowMillis()
        val due31DaysAgo = getMillisForDaysAgo(now, 31)
        val txId = transactionDao.insertTransaction(
            TransactionEntity(
                friendId = friendId,
                amount = 1500.0,
                direction = TransactionDirection.LENT,
                createdDate = due31DaysAgo - 86400000L,
                dueDate = due31DaysAgo,
                status = TransactionStatus.OPEN
            )
        )

        val sentStages = mutableListOf<ReminderStage>()
        SmartReminderEngine.checkAndSendReminders(
            repository = repository,
            context = ApplicationProvider.getApplicationContext(),
            nowMillis = now,
            timeZone = tz,
            notificationSender = { _, _, stage, _, _ ->
                sentStages.add(stage)
                true
            }
        )

        assertEquals(1, sentStages.size)
        assertEquals(ReminderStage.DAY_30, sentStages[0])
    }

    // I. Overdue BORROWED transaction at 7, 15, or 30 days -> NEVER sends reminder
    @Test
    fun testI_borrowedOverdueTransaction_neverSendsReminder() = runTest(testDispatcher) {
        val friendId = friendDao.insertFriend(Friend(name = "LenderFriend"))
        val now = getNowMillis()
        val due15DaysAgo = getMillisForDaysAgo(now, 15)
        transactionDao.insertTransaction(
            TransactionEntity(
                friendId = friendId,
                amount = 1000.0,
                direction = TransactionDirection.BORROWED, // User owes money!
                createdDate = due15DaysAgo - 86400000L,
                dueDate = due15DaysAgo,
                status = TransactionStatus.OPEN
            )
        )

        var sentCount = 0
        val summary = SmartReminderEngine.checkAndSendReminders(
            repository = repository,
            context = ApplicationProvider.getApplicationContext(),
            nowMillis = now,
            timeZone = tz,
            notificationSender = { _, _, _, _, _ ->
                sentCount++
                true
            }
        )

        assertEquals(0, sentCount)
        assertEquals(0, summary.remindersSentCount)
    }

    // J. Transaction without due date -> NEVER sends reminder
    @Test
    fun testJ_transactionWithoutDueDate_neverSendsReminder() = runTest(testDispatcher) {
        val friendId = friendDao.insertFriend(Friend(name = "Kavita"))
        transactionDao.insertTransaction(
            TransactionEntity(
                friendId = friendId,
                amount = 1000.0,
                direction = TransactionDirection.LENT,
                createdDate = 1000L,
                dueDate = null, // No due date
                status = TransactionStatus.OPEN
            )
        )

        var sentCount = 0
        SmartReminderEngine.checkAndSendReminders(
            repository = repository,
            context = ApplicationProvider.getApplicationContext(),
            nowMillis = getNowMillis(),
            timeZone = tz,
            notificationSender = { _, _, _, _, _ ->
                sentCount++
                true
            }
        )

        assertEquals(0, sentCount)
    }

    // K. Fully settled transaction -> NEVER sends reminder
    @Test
    fun testK_settledTransaction_neverSendsReminder() = runTest(testDispatcher) {
        val friendId = friendDao.insertFriend(Friend(name = "Pooja"))
        val now = getNowMillis()
        val due15DaysAgo = getMillisForDaysAgo(now, 15)
        transactionDao.insertTransaction(
            TransactionEntity(
                friendId = friendId,
                amount = 1000.0,
                direction = TransactionDirection.LENT,
                createdDate = due15DaysAgo - 86400000L,
                dueDate = due15DaysAgo,
                status = TransactionStatus.CONFIRMED, // Settled
                settledAt = now - 86400000L
            )
        )

        var sentCount = 0
        SmartReminderEngine.checkAndSendReminders(
            repository = repository,
            context = ApplicationProvider.getApplicationContext(),
            nowMillis = now,
            timeZone = tz,
            notificationSender = { _, _, _, _, _ ->
                sentCount++
                true
            }
        )

        assertEquals(0, sentCount)
    }

    // L. Partial repayment -> reminder states accurate remaining balance
    @Test
    fun testL_partialRepayment_reminderStatesRemainingBalance() = runTest(testDispatcher) {
        val friendId = friendDao.insertFriend(Friend(name = "Vikram"))
        val now = getNowMillis()
        val due7DaysAgo = getMillisForDaysAgo(now, 7)
        val txId = transactionDao.insertTransaction(
            TransactionEntity(
                friendId = friendId,
                amount = 500.0,
                paidAmount = 300.0, // Remaining balance = 200.0
                direction = TransactionDirection.LENT,
                createdDate = due7DaysAgo - 86400000L,
                dueDate = due7DaysAgo,
                status = TransactionStatus.OPEN
            )
        )

        var capturedAmount: Double? = null
        SmartReminderEngine.checkAndSendReminders(
            repository = repository,
            context = ApplicationProvider.getApplicationContext(),
            nowMillis = now,
            timeZone = tz,
            notificationSender = { _, remainingAmount, _, _, _ ->
                capturedAmount = remainingAmount
                true
            }
        )

        assertEquals(200.0, capturedAmount ?: 0.0, 0.001)

        // Verify recorded activity has remaining amount 200.0
        val activities = activityDao.getReminderActivitiesForTransaction(txId)
        assertEquals(1, activities.size)
        assertEquals(200.0, activities[0].amount ?: 0.0, 0.001)
    }

    // M. Partial repayment reducing remaining balance to zero -> NEVER sends reminder
    @Test
    fun testM_partialRepaymentZeroRemaining_neverSendsReminder() = runTest(testDispatcher) {
        val friendId = friendDao.insertFriend(Friend(name = "Vikram"))
        val now = getNowMillis()
        val due7DaysAgo = getMillisForDaysAgo(now, 7)
        transactionDao.insertTransaction(
            TransactionEntity(
                friendId = friendId,
                amount = 500.0,
                paidAmount = 500.0, // Remaining balance = 0.0
                direction = TransactionDirection.LENT,
                createdDate = due7DaysAgo - 86400000L,
                dueDate = due7DaysAgo,
                status = TransactionStatus.OPEN
            )
        )

        var sentCount = 0
        SmartReminderEngine.checkAndSendReminders(
            repository = repository,
            context = ApplicationProvider.getApplicationContext(),
            nowMillis = now,
            timeZone = tz,
            notificationSender = { _, _, _, _, _ ->
                sentCount++
                true
            }
        )

        assertEquals(0, sentCount)
    }

    // N. Mixed friends: only overdue LENT receives reminder
    @Test
    fun testN_mixedFriends_onlyOverdueLentReceivesReminder() = runTest(testDispatcher) {
        val friendLentOverdue = friendDao.insertFriend(Friend(name = "LentOverdue"))
        val friendLentNotOverdue = friendDao.insertFriend(Friend(name = "LentOnTime"))
        val friendBorrowedOverdue = friendDao.insertFriend(Friend(name = "BorrowedOverdue"))

        val now = getNowMillis()
        val due8DaysAgo = getMillisForDaysAgo(now, 8)
        val dueFuture = now + (86400000L * 5)

        // Lent overdue 8 days -> eligible for DAY_7
        transactionDao.insertTransaction(
            TransactionEntity(
                friendId = friendLentOverdue,
                amount = 800.0,
                direction = TransactionDirection.LENT,
                createdDate = due8DaysAgo - 86400000L,
                dueDate = due8DaysAgo,
                status = TransactionStatus.OPEN
            )
        )
        // Lent on time (future due date) -> NOT eligible
        transactionDao.insertTransaction(
            TransactionEntity(
                friendId = friendLentNotOverdue,
                amount = 600.0,
                direction = TransactionDirection.LENT,
                createdDate = now - 86400000L,
                dueDate = dueFuture,
                status = TransactionStatus.OPEN
            )
        )
        // Borrowed overdue -> NOT eligible
        transactionDao.insertTransaction(
            TransactionEntity(
                friendId = friendBorrowedOverdue,
                amount = 400.0,
                direction = TransactionDirection.BORROWED,
                createdDate = due8DaysAgo - 86400000L,
                dueDate = due8DaysAgo,
                status = TransactionStatus.OPEN
            )
        )

        val sentTo = mutableListOf<String>()
        SmartReminderEngine.checkAndSendReminders(
            repository = repository,
            context = ApplicationProvider.getApplicationContext(),
            nowMillis = now,
            timeZone = tz,
            notificationSender = { friendName, _, _, _, _ ->
                sentTo.add(friendName)
                true
            }
        )

        assertEquals(1, sentTo.size)
        assertEquals("LentOverdue", sentTo[0])
    }

    // O. Multiple overdue LENT friends -> each gets reminder with distinct notification ID
    @Test
    fun testO_multipleFriends_distinctNotificationIds() = runTest(testDispatcher) {
        val f1 = friendDao.insertFriend(Friend(name = "Friend1"))
        val f2 = friendDao.insertFriend(Friend(name = "Friend2"))

        val now = getNowMillis()
        val due7DaysAgo = getMillisForDaysAgo(now, 7)

        val tx1 = transactionDao.insertTransaction(
            TransactionEntity(
                friendId = f1,
                amount = 100.0,
                direction = TransactionDirection.LENT,
                createdDate = due7DaysAgo - 86400000L,
                dueDate = due7DaysAgo,
                status = TransactionStatus.OPEN
            )
        )
        val tx2 = transactionDao.insertTransaction(
            TransactionEntity(
                friendId = f2,
                amount = 200.0,
                direction = TransactionDirection.LENT,
                createdDate = due7DaysAgo - 86400000L,
                dueDate = due7DaysAgo,
                status = TransactionStatus.OPEN
            )
        )

        val id1 = ReminderNotificationHelper.generateNotificationId(tx1, ReminderStage.DAY_7)
        val id2 = ReminderNotificationHelper.generateNotificationId(tx2, ReminderStage.DAY_7)

        assertTrue("Notification IDs must be distinct", id1 != id2)
        assertTrue(id1 > 0)
        assertTrue(id2 > 0)
    }

    // P. Idempotency: calling twice in same day does NOT duplicate reminders or activity rows
    @Test
    fun testP_idempotency_callingTwiceDoesNotDuplicate() = runTest(testDispatcher) {
        val friendId = friendDao.insertFriend(Friend(name = "Anil"))
        val now = getNowMillis()
        val due8DaysAgo = getMillisForDaysAgo(now, 8)
        val txId = transactionDao.insertTransaction(
            TransactionEntity(
                friendId = friendId,
                amount = 900.0,
                direction = TransactionDirection.LENT,
                createdDate = due8DaysAgo - 86400000L,
                dueDate = due8DaysAgo,
                status = TransactionStatus.OPEN
            )
        )

        var notificationCount = 0
        val sender: (String, Double, ReminderStage, Long, Long) -> Boolean = { _, _, _, _, _ ->
            notificationCount++
            true
        }

        // First run
        val res1 = SmartReminderEngine.checkAndSendReminders(
            repository = repository,
            context = ApplicationProvider.getApplicationContext(),
            nowMillis = now,
            timeZone = tz,
            notificationSender = sender
        )
        assertEquals(1, res1.remindersSentCount)
        assertEquals(1, notificationCount)

        // Second run 1 hour later
        val res2 = SmartReminderEngine.checkAndSendReminders(
            repository = repository,
            context = ApplicationProvider.getApplicationContext(),
            nowMillis = now + 3600000L,
            timeZone = tz,
            notificationSender = sender
        )
        assertEquals(0, res2.remindersSentCount)
        assertEquals(1, notificationCount) // No second notification!

        val activities = activityDao.getReminderActivitiesForTransaction(txId)
        assertEquals(1, activities.size)
    }

    // Q. Notification permission denied -> does NOT crash, does NOT insert REMINDER_SENT
    @Test
    fun testQ_permissionDenied_doesNotCrashAndDoesNotRecordActivity() = runTest(testDispatcher) {
        val friendId = friendDao.insertFriend(Friend(name = "Karan"))
        val now = getNowMillis()
        val due8DaysAgo = getMillisForDaysAgo(now, 8)
        val txId = transactionDao.insertTransaction(
            TransactionEntity(
                friendId = friendId,
                amount = 1200.0,
                direction = TransactionDirection.LENT,
                createdDate = due8DaysAgo - 86400000L,
                dueDate = due8DaysAgo,
                status = TransactionStatus.OPEN
            )
        )

        // Simulated permission denied returning false
        val summary = SmartReminderEngine.checkAndSendReminders(
            repository = repository,
            context = ApplicationProvider.getApplicationContext(),
            nowMillis = now,
            timeZone = tz,
            notificationSender = { _, _, _, _, _ -> false }
        )

        assertEquals(0, summary.remindersSentCount)
        assertFalse(summary.details[0].posted)
        assertFalse(summary.details[0].recordedInActivity)

        // No REMINDER_SENT activity record must exist!
        val sentStages = repository.getSentReminderStagesForTransaction(txId)
        assertTrue(sentStages.isEmpty())
        val activities = activityDao.getReminderActivitiesForTransaction(txId)
        assertTrue(activities.isEmpty())
    }

    // R. Successful reminder -> inserts exactly one REMINDER_SENT activity record with stage
    @Test
    fun testR_successfulReminder_recordsActivityWithStage() = runTest(testDispatcher) {
        val friendId = friendDao.insertFriend(Friend(name = "Deepak"))
        val now = getNowMillis()
        val due15DaysAgo = getMillisForDaysAgo(now, 15)
        val txId = transactionDao.insertTransaction(
            TransactionEntity(
                friendId = friendId,
                amount = 750.0,
                direction = TransactionDirection.LENT,
                createdDate = due15DaysAgo - 86400000L,
                dueDate = due15DaysAgo,
                status = TransactionStatus.OPEN
            )
        )

        SmartReminderEngine.checkAndSendReminders(
            repository = repository,
            context = ApplicationProvider.getApplicationContext(),
            nowMillis = now,
            timeZone = tz,
            notificationSender = { _, _, _, _, _ -> true }
        )

        val activities = activityDao.getReminderActivitiesForTransaction(txId)
        assertEquals(1, activities.size)
        val record = activities[0]
        assertEquals(ActivityType.REMINDER_SENT, record.type)
        assertEquals(ReminderStage.DAY_15, record.reminderStage)
        assertEquals(750.0, record.amount ?: 0.0, 0.001)
        assertEquals(TransactionDirection.LENT, record.direction)
    }

    // S. Activity screen -> maps REMINDER_SENT with stage into user-friendly text
    @Test
    fun testS_activityScreenMapsReminderSentWithStage() = runTest(testDispatcher) {
        val friendId = friendDao.insertFriend(Friend(name = "Tanvi"))
        val vm = ActivityViewModel(repository)

        activityDao.insertActivity(
            ActivityEntity(
                type = ActivityType.REMINDER_SENT,
                friendId = friendId,
                transactionId = 1L,
                amount = 500.0,
                direction = TransactionDirection.LENT,
                reminderStage = ReminderStage.DAY_7,
                createdAt = 1000L
            )
        )
        activityDao.insertActivity(
            ActivityEntity(
                type = ActivityType.REMINDER_SENT,
                friendId = friendId,
                transactionId = 2L,
                amount = 800.0,
                direction = TransactionDirection.LENT,
                reminderStage = ReminderStage.DAY_15,
                createdAt = 2000L
            )
        )

        testDispatcher.scheduler.advanceUntilIdle()
        val uiState = vm.uiState.first { it.totalActivityCount >= 2 }
        val allActivities = uiState.groupedActivities.values.flatten()

        val itemDay15 = allActivities.find { it.reminderStage == ReminderStage.DAY_15 }
        val itemDay7 = allActivities.find { it.reminderStage == ReminderStage.DAY_7 }

        val context = ApplicationProvider.getApplicationContext<Context>()
        assertNotNull(itemDay15)
        assertEquals("Reminder for you · 15 days overdue", itemDay15?.eventDescription?.asString(context))

        assertNotNull(itemDay7)
        assertEquals("Reminder for you · 7 days overdue", itemDay7?.eventDescription?.asString(context))
    }

    // T. Database migration 3 -> 4 preserves data and allows null reminder_stage
    @Test
    fun testT_databaseMigration3To4PreservesData() = runTest(testDispatcher) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbFile = context.getDatabasePath("test_migration_3_4.db")
        dbFile.delete()

        // 1. Create a DB at version 3 manually
        val helperFactory = FrameworkSQLiteOpenHelperFactory()
        val config = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name("test_migration_3_4.db")
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(3) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `friends` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `contact_info` TEXT, `reliability_tier` TEXT NOT NULL)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `friend_id` INTEGER NOT NULL, `amount` REAL NOT NULL, `direction` TEXT NOT NULL, `note` TEXT, `created_date` INTEGER NOT NULL, `due_date` INTEGER, `status` TEXT NOT NULL, `paid_amount` REAL, `settled_at` INTEGER, FOREIGN KEY(`friend_id`) REFERENCES `friends`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_friend_id` ON `transactions` (`friend_id`)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `activity_records` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `type` TEXT NOT NULL, `friend_id` INTEGER NOT NULL, `transaction_id` INTEGER, `amount` REAL, `direction` TEXT, `note` TEXT, `created_at` INTEGER NOT NULL, FOREIGN KEY(`friend_id`) REFERENCES `friends`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_records_friend_id` ON `activity_records` (`friend_id`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_records_created_at` ON `activity_records` (`created_at`)")
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()
        val helper = helperFactory.create(config)
        val sqliteDb = helper.writableDatabase

        // Insert legacy data into v3
        sqliteDb.execSQL("INSERT INTO friends (id, name, contact_info, reliability_tier) VALUES (1, 'V3Friend', '1111111111', 'HIGH')")
        sqliteDb.execSQL("INSERT INTO transactions (id, friend_id, amount, direction, note, created_date, due_date, status, paid_amount, settled_at) VALUES (1, 1, 500.0, 'LENT', 'LegacyTx', 1000, NULL, 'OPEN', 0.0, NULL)")
        sqliteDb.execSQL("INSERT INTO activity_records (id, type, friend_id, transaction_id, amount, direction, note, created_at) VALUES (1, 'TRANSACTION_CREATED', 1, 1, 500.0, 'LENT', NULL, 1000)")
        sqliteDb.close()

        // 2. Open with Room specifying MIGRATION_3_4
        val upgradedDb = Room.databaseBuilder(context, AppDatabase::class.java, "test_migration_3_4.db")
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4)
            .allowMainThreadQueries()
            .build()

        // 3. Verify legacy data is preserved
        val friend = upgradedDb.friendDao().getFriendByName("V3Friend")
        assertNotNull(friend)
        assertEquals("V3Friend", friend?.name)

        val activities = upgradedDb.activityDao().getAllActivitiesList()
        assertEquals(1, activities.size)
        assertNull(activities[0].reminderStage) // Legacy row has null reminder_stage

        // 4. Insert new row with reminder_stage on v4
        upgradedDb.activityDao().insertActivity(
            ActivityEntity(
                type = ActivityType.REMINDER_SENT,
                friendId = 1,
                transactionId = 1,
                amount = 500.0,
                direction = TransactionDirection.LENT,
                reminderStage = ReminderStage.DAY_7,
                createdAt = 2000L
            )
        )
        val stages = upgradedDb.activityDao().getSentReminderStagesForTransaction(1)
        assertEquals(listOf(ReminderStage.DAY_7), stages)

        upgradedDb.close()
        dbFile.delete()
    }

    // U. Balances and dashboard totals remain unchanged before and after reminders
    @Test
    fun testU_monetaryBalancesUnchangedAfterReminders() = runTest(testDispatcher) {
        val friendId = friendDao.insertFriend(Friend(name = "Neha"))
        val now = getNowMillis()
        val due8DaysAgo = getMillisForDaysAgo(now, 8)
        transactionDao.insertTransaction(
            TransactionEntity(
                friendId = friendId,
                amount = 1000.0,
                direction = TransactionDirection.LENT,
                createdDate = due8DaysAgo - 86400000L,
                dueDate = due8DaysAgo,
                status = TransactionStatus.OPEN
            )
        )

        val totalsBefore = repository.dashboardTotals.first()
        assertEquals(1000.0, totalsBefore.youWillGetBack, 0.001)
        assertEquals(0.0, totalsBefore.youOwe, 0.001)

        SmartReminderEngine.checkAndSendReminders(
            repository = repository,
            context = ApplicationProvider.getApplicationContext(),
            nowMillis = now,
            timeZone = tz,
            notificationSender = { _, _, _, _, _ -> true }
        )

        val totalsAfter = repository.dashboardTotals.first()
        assertEquals(totalsBefore.youWillGetBack, totalsAfter.youWillGetBack, 0.001)
        assertEquals(totalsBefore.youOwe, totalsAfter.youOwe, 0.001)
        assertEquals(totalsBefore.netPosition, totalsAfter.netPosition, 0.001)
    }

    // V. Due date calculation is timezone-safe
    @Test
    fun testV_dueStateCalculation_timezoneSafe() {
        val now = getNowMillis()
        val due7DaysAgo = getMillisForDaysAgo(now, 7)

        val utcState = DueDateHelper.calculateDueState(due7DaysAgo, TransactionStatus.OPEN, now, TimeZone.getTimeZone("UTC"))
        val istState = DueDateHelper.calculateDueState(due7DaysAgo, TransactionStatus.OPEN, now, TimeZone.getTimeZone("Asia/Kolkata"))

        assertTrue(utcState.isActivelyOverdue)
        assertTrue(istState.isActivelyOverdue)
        assertEquals(7, utcState.overdueDays)
    }

    // W. WorkManager Worker returns success
    @Test
    fun testW_workerExecution_returnsSuccess() = runTest(testDispatcher) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val worker = TestListenableWorkerBuilder<SmartReminderWorker>(context).build()
        val result = worker.doWork()
        assertTrue(result is ListenableWorker.Result.Success)
    }

    // X. ReminderNotificationHelper template title and body formats
    @Test
    fun testX_notificationTitlesAndBodies() {
        val friend = "Rahul"
        val amt = "₹1,500"

        assertEquals("Payment reminder · Rahul", ReminderStage.DAY_7.formatTitle(friend))
        assertEquals("₹1,500 is still pending. It’s been 7 days since the due date.", ReminderStage.DAY_7.formatBody(amt))

        assertEquals("Payment follow-up · Rahul", ReminderStage.DAY_15.formatTitle(friend))
        assertEquals("₹1,500 is still pending after 15 days.", ReminderStage.DAY_15.formatBody(amt))

        assertEquals("Payment still pending · Rahul", ReminderStage.DAY_30.formatTitle(friend))
        assertEquals("₹1,500 remains unsettled after 30 days.", ReminderStage.DAY_30.formatBody(amt))
    }
}
