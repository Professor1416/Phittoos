package com.professor1416.phittoos

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.professor1416.phittoos.data.db.AppDatabase
import com.professor1416.phittoos.data.preferences.UserPreferences
import com.professor1416.phittoos.data.model.TransactionDirection
import com.professor1416.phittoos.data.repository.PhittoosRepository
import com.professor1416.phittoos.ui.screens.settings.SettingsScreen
import com.professor1416.phittoos.ui.theme.PhittoosTheme
import com.professor1416.phittoos.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowContentResolver
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream

// Test-only provider behavior: the stock shadow falls back to framework access for null suppliers.
@Implements(ContentResolver::class)
class ExportOutputResolverShadow : ShadowContentResolver() {
    @Implementation
    public override fun openOutputStream(uri: Uri): OutputStream? = openStream(uri)

    companion object {
        var openStream: (Uri) -> OutputStream? = { error("No synthetic provider configured") }
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w400dp-h900dp", shadows = [ExportOutputResolverShadow::class])
class ExportOutputReliabilityTest {
    @get:Rule val composeTestRule = createComposeRule()
    private lateinit var db: AppDatabase
    private lateinit var repository: PhittoosRepository
    private lateinit var preferences: UserPreferences
    private lateinit var viewModel: SettingsViewModel
    private lateinit var context: Context
    private val store = ViewModelStore()
    private val registry = DocumentRegistry()
    private val originalBackupTime = 1234L
    private var preparedAt = 0L
    private var launchedAt = 0L
    private lateinit var expectedPayload: String
    private var beforeLedger: List<Any> = emptyList()
    private val events = mutableListOf<String>()

    private enum class Format(val tag: String, val mime: String, val success: String, val failure: String) {
        JSON("btn_create_backup", "application/json", "Backup created successfully", "Backup creation failed:"),
        CSV("btn_export_data", "text/csv", "Export completed successfully", "Export failed:")
    }
    private enum class Failure { NULL, OPEN, WRITE, FLUSH, CLOSE }

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repository = PhittoosRepository(db.friendDao(), db.transactionDao(), db.activityDao(), db)
        preferences = UserPreferences(context)
        preferences.clearAll()
        preferences.userName = "Synthetic owner"
        preferences.hasCompletedOnboarding = true
        preferences.remindersEnabled = false
        preferences.lastBackupTime = originalBackupTime
        runBlocking {
            val friend = repository.insertFriend("Synthetic friend")
            val id = repository.addTransaction(friend, 50.25, TransactionDirection.LENT, "Synthetic ₹ note", createdDate = 1_700_000_000_000L)
            repository.recordRepayment(id, 10.0, settledAt = 1_700_000_000_001L)
        }
        viewModel = SettingsViewModel(repository, preferences)
        store.put("settings", viewModel)
        val owner = object : ActivityResultRegistryOwner {
            override val activityResultRegistry: ActivityResultRegistry = registry
        }
        composeTestRule.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                PhittoosTheme { SettingsScreen(viewModel, onNavigateBack = {}, onDataCleared = {}) }
            }
        }
    }

    @After
    fun tearDown() {
        composeTestRule.runOnIdle { store.clear() }
        preferences.clearAll()
        db.close()
        ExportOutputResolverShadow.openStream = { error("No synthetic provider configured") }
    }

    @Test fun jsonNullStreamFails() = assertFailure(Format.JSON, Failure.NULL)
    @Test fun csvNullStreamFails() = assertFailure(Format.CSV, Failure.NULL)
    @Test fun jsonOpeningExceptionFails() = assertFailure(Format.JSON, Failure.OPEN)
    @Test fun csvOpeningExceptionFails() = assertFailure(Format.CSV, Failure.OPEN)
    @Test fun jsonWriteExceptionFails() = assertFailure(Format.JSON, Failure.WRITE)
    @Test fun csvWriteExceptionFails() = assertFailure(Format.CSV, Failure.WRITE)
    @Test fun jsonFlushExceptionFails() = assertFailure(Format.JSON, Failure.FLUSH)
    @Test fun csvFlushExceptionFails() = assertFailure(Format.CSV, Failure.FLUSH)
    @Test fun jsonCloseExceptionFails() = assertFailure(Format.JSON, Failure.CLOSE)
    @Test fun csvCloseExceptionFails() = assertFailure(Format.CSV, Failure.CLOSE)
    @Test fun jsonSuccessfulOutputUpdatesTimestampAfterClose() = assertSuccess(Format.JSON)
    @Test fun csvSuccessfulOutputPreservesTimestamp() = assertSuccess(Format.CSV)
    @Test fun jsonCancellationIsSilent() = assertCancellation(Format.JSON)
    @Test fun csvCancellationIsSilent() = assertCancellation(Format.CSV)
    @Test fun jsonFailureThenSuccessUsesFreshPayload() = assertRetry(Format.JSON, cancelFirst = false)
    @Test fun csvFailureThenSuccessUsesFreshPayload() = assertRetry(Format.CSV, cancelFirst = false)
    @Test fun jsonCancellationThenSuccessUsesFreshPayload() = assertRetry(Format.JSON, cancelFirst = true)
    @Test fun csvCancellationThenSuccessUsesFreshPayload() = assertRetry(Format.CSV, cancelFirst = true)

    private fun ledger() = runBlocking {
        listOf(db.friendDao().getAllFriendsList(), db.transactionDao().getAllTransactionsOrdered(), db.activityDao().getAllActivitiesList())
    }

    private fun begin(format: Format) {
        beforeLedger = ledger()
        preparedAt = System.currentTimeMillis()
        expectedPayload = runBlocking {
            (if (format == Format.JSON) viewModel.generateBackupJson() else viewModel.generateCsvExport()).first!!
        }
        val count = registry.launches
        composeTestRule.onNodeWithTag(format.tag).performScrollTo().performClick()
        composeTestRule.waitUntil(5_000) {
            composeTestRule.waitForIdle()
            registry.launches == count + 1
        }
        launchedAt = System.currentTimeMillis()
        assertEquals(Intent.ACTION_CREATE_DOCUMENT, registry.intent.action)
        assertEquals(format.mime, registry.intent.type)
        assertEquals(originalBackupTime, preferences.lastBackupTime)
    }

    private fun deliver(uri: Uri?) {
        composeTestRule.runOnUiThread {
            registry.dispatchResult(registry.requestCode,
                if (uri == null) Activity.RESULT_CANCELED else Activity.RESULT_OK,
                uri?.let { Intent().setData(it) })
        }
        composeTestRule.waitForIdle()
        assertEquals(beforeLedger, ledger())
    }

    private fun provider(failure: Failure? = null): RecordingStream {
        val stream = RecordingStream(failure)
        ExportOutputResolverShadow.openStream = {
            events += "open"
            assertEquals(originalBackupTime, preferences.lastBackupTime)
            when (failure) {
                Failure.NULL -> null
                Failure.OPEN -> throw IOException("Synthetic open failure")
                else -> stream
            }
        }
        return stream
    }

    private fun assertFailure(format: Format, failure: Failure) {
        begin(format)
        provider(failure)
        deliver(Uri.parse("content://synthetic-export/failure"))
        assertEquals(originalBackupTime, preferences.lastBackupTime)
        assertEquals(originalBackupTime, viewModel.uiState.value.lastBackupTime)
        composeTestRule.onAllNodesWithText(format.success).assertCountEquals(0)
        awaitFeedback(format.failure, substring = true)
        val expectedEvents = when (failure) {
            Failure.NULL, Failure.OPEN -> listOf("open")
            Failure.WRITE -> listOf("open", "write", "close")
            Failure.FLUSH, Failure.CLOSE -> listOf("open", "write", "flush", "close")
        }
        assertEquals(expectedEvents, events)
    }

    private fun assertSuccess(format: Format) {
        begin(format)
        val stream = provider()
        val beforeResult = System.currentTimeMillis()
        deliver(Uri.parse("content://synthetic-export/success"))
        assertEquals(listOf("open", "write", "flush", "close"), events)
        assertBytes(format, stream.bytes.toByteArray())
        if (format == Format.JSON) {
            assertTrue(preferences.lastBackupTime in beforeResult..System.currentTimeMillis())
            assertEquals(preferences.lastBackupTime, viewModel.uiState.value.lastBackupTime)
        } else {
            assertEquals(originalBackupTime, preferences.lastBackupTime)
        }
        awaitFeedback(format.success)
        composeTestRule.onAllNodesWithText(format.failure, substring = true).assertCountEquals(0)
    }

    private fun assertBytes(format: Format, actual: ByteArray) {
        val expected = if (format == Format.JSON) {
            // JSON's generated timestamp varies; verify its bounds separately, then compare every byte.
            val generatedAt = JSONObject(actual.toString(Charsets.UTF_8)).getJSONObject("metadata").getLong("createdAt")
            assertTrue(generatedAt in preparedAt..launchedAt)
            JSONObject(expectedPayload).apply { getJSONObject("metadata").put("createdAt", generatedAt) }.toString(2)
        } else expectedPayload
        assertArrayEquals(expected.toByteArray(Charsets.UTF_8), actual)
    }

    private fun assertCancellation(format: Format) {
        begin(format)
        provider()
        deliver(null)
        assertTrue(events.isEmpty())
        assertEquals(originalBackupTime, preferences.lastBackupTime)
        composeTestRule.onAllNodesWithText(format.success).assertCountEquals(0)
        composeTestRule.onAllNodesWithText(format.failure, substring = true).assertCountEquals(0)
    }

    private fun assertRetry(format: Format, cancelFirst: Boolean) {
        if (cancelFirst) assertCancellation(format) else {
            assertFailure(format, Failure.WRITE)
            composeTestRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss))
                .performSemanticsAction(SemanticsActions.Dismiss) { it() }
        }
        val oldPayload = expectedPayload
        runBlocking {
            val friend = repository.insertFriend("Synthetic retry friend")
            repository.addTransaction(friend, 12.34, TransactionDirection.BORROWED, "Fresh retry payload", createdDate = 1_700_000_000_002L)
        }
        events.clear()
        assertSuccess(format)
        assertTrue(expectedPayload != oldPayload)
    }

    private fun awaitFeedback(text: String, substring: Boolean = false) {
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onAllNodesWithText(text, substring = substring).assertCountEquals(1)
    }

    private inner class RecordingStream(private val failure: Failure?) : OutputStream() {
        val bytes = ByteArrayOutputStream()
        private fun stage(name: String, throwsAt: Failure) {
            events += name
            // The timestamp must still be old throughout opening, writing, flushing AND closing.
            assertEquals(originalBackupTime, preferences.lastBackupTime)
            assertEquals(originalBackupTime, viewModel.uiState.value.lastBackupTime)
            if (failure == throwsAt) throw IOException("Synthetic $name failure")
        }
        override fun write(value: Int) { stage("write", Failure.WRITE); bytes.write(value) }
        override fun write(buffer: ByteArray, offset: Int, length: Int) {
            if (failure == Failure.WRITE) bytes.write(buffer, offset, length / 2)
            stage("write", Failure.WRITE)
            bytes.write(buffer, offset, length)
        }
        override fun flush() = stage("flush", Failure.FLUSH)
        override fun close() = stage("close", Failure.CLOSE)
    }

    private inner class DocumentRegistry : ActivityResultRegistry() {
        var launches = 0
        var requestCode = 0
        lateinit var intent: Intent
        override fun <I, O> onLaunch(requestCode: Int, contract: ActivityResultContract<I, O>, input: I, options: ActivityOptionsCompat?) {
            this.requestCode = requestCode
            intent = contract.createIntent(context, input)
            launches++
        }
    }
}
