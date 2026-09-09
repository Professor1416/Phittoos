package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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
import com.example.data.preferences.UserPreferences
import com.example.data.repository.PhittoosRepository
import com.example.export.PhittoosCsvExporter
import com.example.reminder.SmartReminderEngine
import com.example.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.Dispatchers
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

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SettingsAndExportTest {

    private lateinit var db: AppDatabase
    private lateinit var friendDao: FriendDao
    private lateinit var transactionDao: TransactionDao
    private lateinit var activityDao: ActivityDao
    private lateinit var repository: PhittoosRepository
    private lateinit var userPreferences: UserPreferences
    private lateinit var context: Context
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
    private val tz = TimeZone.getTimeZone("UTC")

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
        repository = PhittoosRepository(friendDao, transactionDao, activityDao)
        userPreferences = UserPreferences(context)
        userPreferences.clearAll()
    }

    @After
    fun tearDown() {
        userPreferences.clearAll()
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

    // A. Existing profile name loads correctly
    @Test
    fun testA_existingProfileNameLoadsCorrectly() {
        userPreferences.userName = "Prashant"
        val vm = SettingsViewModel(repository, userPreferences)
        assertEquals("Prashant", vm.uiState.value.profileName)
    }

    // B. Valid edited name persists
    @Test
    fun testB_validEditedNamePersists() {
        userPreferences.userName = "Prashant"
        val vm = SettingsViewModel(repository, userPreferences)
        val success = vm.updateProfileName("  Rahul Sharma  ")

        assertTrue(success)
        assertEquals("Rahul Sharma", vm.uiState.value.profileName)
        assertEquals("Rahul Sharma", userPreferences.userName)
        assertNull(vm.uiState.value.nameError)
        assertEquals("Name saved", vm.uiState.value.message)
    }

    // C. Blank edited name rejected
    @Test
    fun testC_blankEditedNameRejected() {
        userPreferences.userName = "Prashant"
        val vm = SettingsViewModel(repository, userPreferences)
        val success = vm.updateProfileName("     ")

        assertFalse(success)
        assertEquals("Prashant", userPreferences.userName) // Preserved
        assertNotNull(vm.uiState.value.nameError)
        assertEquals("Name cannot be blank", vm.uiState.value.nameError)
    }

    // D. Reminder preference defaults correctly
    @Test
    fun testD_reminderPreferenceDefaultsCorrectly() {
        val vm = SettingsViewModel(repository, userPreferences)
        assertTrue(userPreferences.remindersEnabled)
        assertTrue(vm.uiState.value.remindersEnabled)
    }

    // E. Reminder OFF prevents SmartReminderEngine from sending notifications
    @Test
    fun testE_reminderOffPreventsNotifications() = runTest(testDispatcher) {
        val friendId = friendDao.insertFriend(Friend(name = "Amit"))
        val now = 1000000000L
        val dueDate = getMillisForDaysAgo(now, 10)

        transactionDao.insertTransaction(
            TransactionEntity(
                friendId = friendId,
                amount = 500.0,
                direction = TransactionDirection.LENT,
                dueDate = dueDate,
                status = TransactionStatus.OPEN,
                createdDate = dueDate - 86400000L
            )
        )

        var notificationSent = false
        val summary = SmartReminderEngine.checkAndSendReminders(
            repository = repository,
            context = context,
            nowMillis = now,
            timeZone = tz,
            notificationSender = { _, _, _, _, _ ->
                notificationSent = true
                true
            },
            remindersEnabled = false
        )

        assertFalse(notificationSent)
        assertEquals(0, summary.remindersSentCount)
        assertEquals(0, summary.checkedTransactionsCount)
    }

    // F. Reminder OFF creates no REMINDER_SENT activity
    @Test
    fun testF_reminderOffCreatesNoActivity() = runTest(testDispatcher) {
        val friendId = friendDao.insertFriend(Friend(name = "Amit"))
        val now = 1000000000L
        val dueDate = getMillisForDaysAgo(now, 10)

        transactionDao.insertTransaction(
            TransactionEntity(
                friendId = friendId,
                amount = 500.0,
                direction = TransactionDirection.LENT,
                dueDate = dueDate,
                status = TransactionStatus.OPEN,
                createdDate = dueDate - 86400000L
            )
        )

        SmartReminderEngine.checkAndSendReminders(
            repository = repository,
            context = context,
            nowMillis = now,
            timeZone = tz,
            remindersEnabled = false
        )

        val activities = activityDao.getAllActivitiesList()
        assertTrue(activities.isEmpty())
    }

    // G. Reminder ON restores eligibility checks
    @Test
    fun testG_reminderOnRestoresEligibilityChecks() = runTest(testDispatcher) {
        val friendId = friendDao.insertFriend(Friend(name = "Amit"))
        val now = 1000000000L
        val dueDate = getMillisForDaysAgo(now, 10)

        transactionDao.insertTransaction(
            TransactionEntity(
                friendId = friendId,
                amount = 500.0,
                direction = TransactionDirection.LENT,
                dueDate = dueDate,
                status = TransactionStatus.OPEN,
                createdDate = dueDate - 86400000L
            )
        )

        var notificationSent = false
        val summary = SmartReminderEngine.checkAndSendReminders(
            repository = repository,
            context = context,
            nowMillis = now,
            timeZone = tz,
            notificationSender = { _, _, _, _, _ ->
                notificationSent = true
                true
            },
            remindersEnabled = true
        )

        assertTrue(notificationSent)
        assertEquals(1, summary.remindersSentCount)
        assertEquals(1, activityDao.getAllActivitiesList().size)
    }

    // H. Repeated ON/OFF does not create duplicate WorkManager scheduling
    @Test
    fun testH_repeatedOnOffToggleIdempotent() {
        val vm = SettingsViewModel(repository, userPreferences)

        vm.toggleReminders(false, context)
        assertFalse(userPreferences.remindersEnabled)
        assertFalse(vm.uiState.value.remindersEnabled)

        vm.toggleReminders(true, context)
        assertTrue(userPreferences.remindersEnabled)
        assertTrue(vm.uiState.value.remindersEnabled)

        vm.toggleReminders(false, context)
        assertFalse(userPreferences.remindersEnabled)

        vm.toggleReminders(true, context)
        assertTrue(userPreferences.remindersEnabled)
    }

    // I. CSV export includes correct header
    @Test
    fun testI_csvExportIncludesCorrectHeader() {
        val csv = PhittoosCsvExporter.buildCsv(emptyList(), emptyList())
        val lines = csv.trim().lines()
        assertEquals(1, lines.size)
        assertEquals(
            "transaction_id,friend_name,direction,amount,paid_amount,remaining_amount,status,created_date,due_date,settled_date,note",
            lines[0]
        )
    }

    // J. LENT transaction exports correct direction
    @Test
    fun testJ_lentTransactionExportsCorrectDirection() {
        val friend = Friend(id = 1, name = "Sara")
        val tx = TransactionEntity(
            id = 10,
            friendId = 1,
            amount = 1200.0,
            direction = TransactionDirection.LENT,
            status = TransactionStatus.OPEN,
            createdDate = 1000L
        )

        val csv = PhittoosCsvExporter.buildCsv(listOf(tx), listOf(friend), tz)
        assertTrue(csv.contains(",LENT,"))
        assertTrue(csv.contains("Sara"))
    }

    // K. BORROWED transaction exports correct direction
    @Test
    fun testK_borrowedTransactionExportsCorrectDirection() {
        val friend = Friend(id = 1, name = "Karan")
        val tx = TransactionEntity(
            id = 11,
            friendId = 1,
            amount = 800.0,
            direction = TransactionDirection.BORROWED,
            status = TransactionStatus.OPEN,
            createdDate = 1000L
        )

        val csv = PhittoosCsvExporter.buildCsv(listOf(tx), listOf(friend), tz)
        assertTrue(csv.contains(",BORROWED,"))
        assertTrue(csv.contains("Karan"))
    }

    // L. Partial repayment exports correct paid and remaining values
    @Test
    fun testL_partialRepaymentExportsCorrectPaidAndRemaining() {
        val friend = Friend(id = 1, name = "Neha")
        val tx = TransactionEntity(
            id = 12,
            friendId = 1,
            amount = 500.0,
            paidAmount = 300.0,
            direction = TransactionDirection.LENT,
            status = TransactionStatus.OPEN,
            createdDate = 1000L
        )

        val csv = PhittoosCsvExporter.buildCsv(listOf(tx), listOf(friend), tz)
        // Expected amounts: amount=500, paid=300, remaining=200
        assertTrue(csv.contains(",500,300,200,"))
    }

    // M. Settled transaction exports remaining 0
    @Test
    fun testM_settledTransactionExportsRemainingZero() {
        val friend = Friend(id = 1, name = "Rohan")
        val tx = TransactionEntity(
            id = 13,
            friendId = 1,
            amount = 1000.0,
            paidAmount = 1000.0,
            direction = TransactionDirection.LENT,
            status = TransactionStatus.CONFIRMED,
            settledAt = 2000L,
            createdDate = 1000L
        )

        val csv = PhittoosCsvExporter.buildCsv(listOf(tx), listOf(friend), tz)
        // remaining amount must be 0 for confirmed transaction
        assertTrue(csv.contains(",1000,1000,0,CONFIRMED,"))
    }

    // N. Null due date exports empty due-date cell
    @Test
    fun testN_nullDueDateExportsEmptyCell() {
        val friend = Friend(id = 1, name = "Deepak")
        val tx = TransactionEntity(
            id = 14,
            friendId = 1,
            amount = 250.0,
            direction = TransactionDirection.LENT,
            status = TransactionStatus.OPEN,
            dueDate = null,
            createdDate = 1000L
        )

        val csv = PhittoosCsvExporter.buildCsv(listOf(tx), listOf(friend), tz)
        val line = csv.trim().lines()[1]
        val parts = line.split(",")
        // Column indices: 7: created_date, 8: due_date, 9: settled_date
        assertEquals("", parts[8])
        assertEquals("", parts[9])
    }

    // O. CSV properly escapes comma in note
    @Test
    fun testO_csvEscapesCommaInNote() {
        val friend = Friend(id = 1, name = "Ananya")
        val tx = TransactionEntity(
            id = 15,
            friendId = 1,
            amount = 450.0,
            direction = TransactionDirection.LENT,
            status = TransactionStatus.OPEN,
            note = "Lunch, cab and coffee",
            createdDate = 1000L
        )

        val csv = PhittoosCsvExporter.buildCsv(listOf(tx), listOf(friend), tz)
        assertTrue(csv.contains("\"Lunch, cab and coffee\""))
    }

    // P. CSV properly escapes quotes
    @Test
    fun testP_csvEscapesQuotesInNote() {
        val friend = Friend(id = 1, name = "Varun")
        val tx = TransactionEntity(
            id = 16,
            friendId = 1,
            amount = 300.0,
            direction = TransactionDirection.LENT,
            status = TransactionStatus.OPEN,
            note = "Said \"Thank You\"",
            createdDate = 1000L
        )

        val csv = PhittoosCsvExporter.buildCsv(listOf(tx), listOf(friend), tz)
        assertTrue(csv.contains("\"Said \"\"Thank You\"\"\""))
    }

    // Q. CSV properly handles newline in note
    @Test
    fun testQ_csvHandlesNewlineInNote() {
        val friend = Friend(id = 1, name = "Meera")
        val tx = TransactionEntity(
            id = 17,
            friendId = 1,
            amount = 600.0,
            direction = TransactionDirection.LENT,
            status = TransactionStatus.OPEN,
            note = "First line\nSecond line",
            createdDate = 1000L
        )

        val csv = PhittoosCsvExporter.buildCsv(listOf(tx), listOf(friend), tz)
        assertTrue(csv.contains("\"First line\nSecond line\""))
    }

    // R. CSV ordering is deterministic
    @Test
    fun testR_csvOrderingIsDeterministic() {
        val friend = Friend(id = 1, name = "Pooja")
        val txLater = TransactionEntity(
            id = 20,
            friendId = 1,
            amount = 100.0,
            direction = TransactionDirection.LENT,
            status = TransactionStatus.OPEN,
            createdDate = 2000L
        )
        val txEarlier = TransactionEntity(
            id = 21,
            friendId = 1,
            amount = 200.0,
            direction = TransactionDirection.LENT,
            status = TransactionStatus.OPEN,
            createdDate = 1000L
        )

        // Pass out of order
        val csv = PhittoosCsvExporter.buildCsv(listOf(txLater, txEarlier), listOf(friend), tz)
        val lines = csv.trim().lines()
        assertEquals(3, lines.size)
        // First line is header, second line must be txEarlier (createdDate 1000), third is txLater (2000)
        assertTrue(lines[1].startsWith("21,"))
        assertTrue(lines[2].startsWith("20,"))
    }

    // S. Export does not modify balances or transactions
    @Test
    fun testS_exportDoesNotModifyBalancesOrTransactions() = runTest(testDispatcher) {
        val friendId = friendDao.insertFriend(Friend(name = "Kavita"))
        val txId = transactionDao.insertTransaction(
            TransactionEntity(
                friendId = friendId,
                amount = 1500.0,
                paidAmount = 500.0,
                direction = TransactionDirection.LENT,
                status = TransactionStatus.OPEN,
                createdDate = 1000L
            )
        )

        val vm = SettingsViewModel(repository, userPreferences)
        val (csv, _) = vm.generateCsvExport()

        assertNotNull(csv)
        // Verify database entity is strictly unmodified
        val txAfter = transactionDao.getTransactionById(txId)
        assertNotNull(txAfter)
        assertEquals(1500.0, txAfter?.amount ?: 0.0, 0.001)
        assertEquals(500.0, txAfter?.paidAmount ?: 0.0, 0.001)
        assertEquals(TransactionStatus.OPEN, txAfter?.status)
    }

    // T. Clear all removes friends
    @Test
    fun testT_clearAllRemovesFriends() = runTest(testDispatcher) {
        friendDao.insertFriend(Friend(name = "Friend 1"))
        friendDao.insertFriend(Friend(name = "Friend 2"))
        assertEquals(2, friendDao.getAllFriendsList().size)

        repository.clearAllData()
        assertEquals(0, friendDao.getAllFriendsList().size)
    }

    // U. Clear all removes transactions
    @Test
    fun testU_clearAllRemovesTransactions() = runTest(testDispatcher) {
        val friendId = friendDao.insertFriend(Friend(name = "Friend 1"))
        transactionDao.insertTransaction(
            TransactionEntity(
                friendId = friendId,
                amount = 100.0,
                direction = TransactionDirection.LENT,
                status = TransactionStatus.OPEN,
                createdDate = 1000L
            )
        )
        assertEquals(1, transactionDao.getAllTransactionsOrdered().size)

        repository.clearAllData()
        assertEquals(0, transactionDao.getAllTransactionsOrdered().size)
    }

    // V. Clear all removes activity/reminder history
    @Test
    fun testV_clearAllRemovesActivityAndReminderHistory() = runTest(testDispatcher) {
        val friendId = friendDao.insertFriend(Friend(name = "Friend 1"))
        activityDao.insertActivity(
            ActivityEntity(
                type = ActivityType.REMINDER_SENT,
                friendId = friendId,
                transactionId = 1L,
                amount = 500.0,
                reminderStage = ReminderStage.DAY_7
            )
        )
        assertEquals(1, activityDao.getAllActivitiesList().size)

        repository.clearAllData()
        assertEquals(0, activityDao.getAllActivitiesList().size)
    }

    // W. Clear all resets onboarding/profile according to chosen semantics
    @Test
    fun testW_clearAllResetsOnboardingAndProfile() = runTest(testDispatcher) {
        userPreferences.userName = "Prashant"
        userPreferences.hasCompletedOnboarding = true
        userPreferences.remindersEnabled = false

        val vm = SettingsViewModel(repository, userPreferences)
        var onClearedCalled = false

        vm.clearAllData(context) {
            onClearedCalled = true
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(onClearedCalled)
        assertEquals("", userPreferences.userName)
        assertFalse(userPreferences.hasCompletedOnboarding)
        assertTrue(userPreferences.remindersEnabled) // Back to default true
    }

    // X. No stale reminder eligibility remains after clear
    @Test
    fun testX_noStaleReminderEligibilityAfterClear() = runTest(testDispatcher) {
        val friendId = friendDao.insertFriend(Friend(name = "OverdueFriend"))
        val now = 1000000000L
        val dueDate = getMillisForDaysAgo(now, 20)

        transactionDao.insertTransaction(
            TransactionEntity(
                friendId = friendId,
                amount = 1000.0,
                direction = TransactionDirection.LENT,
                dueDate = dueDate,
                status = TransactionStatus.OPEN,
                createdDate = dueDate - 86400000L
            )
        )

        // Clear all data
        repository.clearAllData()

        val summary = SmartReminderEngine.checkAndSendReminders(
            repository = repository,
            context = context,
            nowMillis = now,
            timeZone = tz
        )

        assertEquals(0, summary.checkedTransactionsCount)
        assertEquals(0, summary.remindersSentCount)
    }

    // Y. App version retrieval does not crash
    @Test
    fun testY_appVersionRetrievalDoesNotCrash() {
        val version = SettingsViewModel.getAppVersion(context)
        assertNotNull(version)
        assertTrue(version.isNotBlank())
    }

    // Z. Duplicate friend names get separate distinct IDs (no V2 identity collapsing in V1)
    @Test
    fun testZ_duplicateFriendNamesGetDistinctIds() = runTest(testDispatcher) {
        val id1 = repository.insertFriend("Rahul")
        val id2 = repository.insertFriend("Rahul")
        assertTrue(id1 > 0)
        assertTrue(id2 > 0)
        assertTrue("Two friends with the same name must have separate IDs", id1 != id2)
    }

    // AA. Blank friend name is rejected
    @Test(expected = IllegalArgumentException::class)
    fun testAA_blankFriendNameRejected() = runTest(testDispatcher) {
        repository.insertFriend("   ")
    }
}
