package com.professor1416.phittoos

import android.content.Context
import android.os.Looper
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.professor1416.phittoos.data.dao.ActivityDao
import com.professor1416.phittoos.data.dao.FriendDao
import com.professor1416.phittoos.data.dao.TransactionDao
import com.professor1416.phittoos.data.db.AppDatabase
import com.professor1416.phittoos.data.model.TransactionDirection
import com.professor1416.phittoos.data.preferences.AppThemeMode
import com.professor1416.phittoos.data.preferences.UserPreferences
import com.professor1416.phittoos.data.repository.PhittoosRepository
import com.professor1416.phittoos.ui.screens.addtransaction.AddTransactionScreen
import com.professor1416.phittoos.ui.screens.home.HomeScreen
import com.professor1416.phittoos.ui.theme.PhittoosTheme
import com.professor1416.phittoos.ui.viewmodel.AddTransactionViewModel
import com.professor1416.phittoos.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w360dp-h740dp")
class LayoutAndSystemInsetsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val viewModelStore = ViewModelStore()
    private val viewModelJobs = mutableListOf<Job>()
    private val showContent = mutableStateOf(true)
    private var contentWasSet = false

    private fun <T : ViewModel> own(viewModel: T): T {
        viewModelStore.put("test-${viewModelJobs.size}", viewModel)
        viewModelJobs += viewModel.viewModelScope.coroutineContext[Job]!!
        return viewModel
    }

    private fun setTestContent(content: @Composable () -> Unit) {
        composeTestRule.setContent { if (showContent.value) content() }
        contentWasSet = true
    }

    private lateinit var db: AppDatabase
    private lateinit var friendDao: FriendDao
    private lateinit var transactionDao: TransactionDao
    private lateinit var activityDao: ActivityDao
    private lateinit var repository: PhittoosRepository
    private lateinit var userPreferences: UserPreferences
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        friendDao = db.friendDao()
        transactionDao = db.transactionDao()
        activityDao = db.activityDao()
        repository = PhittoosRepository(friendDao, transactionDao, activityDao, db)
        userPreferences = UserPreferences(context)
        userPreferences.clearAll()
    }

    @After
    fun tearDown() {
        try {
            // Dispose screen collectors before ending the ViewModels' ownership.
            if (contentWasSet) {
                composeTestRule.runOnIdle { showContent.value = false }
                composeTestRule.waitForIdle()
            }
        } finally {
            // These manually constructed ViewModels are not owned by the Compose activity.
            composeTestRule.runOnUiThread { viewModelStore.clear() }
            composeTestRule.waitUntil(timeoutMillis = 5_000) {
                // Without mounted content, Compose does not drain pending Main callbacks.
                shadowOf(Looper.getMainLooper()).idle()
                viewModelJobs.all { it.isCancelled && it.isCompleted }
            }
            // A cancellation timeout must fail before closing a database still in use.
            db.close()
        }
    }

    @Test
    fun homeScreen_rendersFabAndListContent() {
        val friendId = runBlocking {
            val id = repository.insertFriend("Vikram")
            val transactionId = repository.addTransaction(id, 450.0, TransactionDirection.LENT, "Lunch")
            assertEquals("Vikram", friendDao.getFriendByIdOnce(id)?.name)
            assertEquals(id, transactionDao.getTransactionById(transactionId)!!.friendId)
            id
        }

        val homeViewModel = own(HomeViewModel(repository, userPreferences))

        setTestContent {
            PhittoosTheme(themeMode = AppThemeMode.LIGHT) {
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToAddTransaction = {},
                    onNavigateToFriendDetail = {}
                )
            }
        }

        // Compose idle does not guarantee that Room/combine has delivered the friend list.
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            // Query semantics on every poll to drain pending Robolectric UI work as well.
            val rowExists = composeTestRule.onAllNodesWithTag("friend_row_$friendId")
                .fetchSemanticsNodes().isNotEmpty()
            val state = homeViewModel.uiState.value
            rowExists && !state.isLoading &&
                state.friends.any { it.friend.id == friendId && it.friend.name == "Vikram" }
        }
        val loadedFriend = homeViewModel.uiState.value.friends.single { it.friend.id == friendId }
        assertEquals(450.0, loadedFriend.netBalance, 0.0)
        assertEquals(1, loadedFriend.openTransactionsCount)
        composeTestRule.onAllNodesWithText("Loading your ledger…").assertCountEquals(0)
        composeTestRule.onNodeWithTag("fab_add_transaction").assertIsDisplayed()
        composeTestRule.onNodeWithText("Vikram").assertIsDisplayed()
    }

    @Test
    fun addTransaction_allQuickAmountChipsAreReachable() {
        var friendId: Long = 0L
        runBlocking {
            friendId = repository.insertFriend("Rohan")
        }

        val viewModel = own(AddTransactionViewModel(repository, initialFriendId = friendId))

        setTestContent {
            PhittoosTheme(themeMode = AppThemeMode.LIGHT) {
                AddTransactionScreen(
                    viewModel = viewModel,
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        // Verify chips +100, +200, +500, +1000, +2000, +5000 exist and can be clicked
        val quickAmounts = listOf(100, 200, 500, 1000, 2000, 5000)
        for (amt in quickAmounts) {
            composeTestRule.onNodeWithTag("chip_amount_$amt").assertExists()
        }

        // Tap +500 and verify amount state updates
        composeTestRule.onNodeWithTag("chip_amount_500").performClick()
        composeTestRule.waitForIdle()
        assertEquals("500", viewModel.uiState.value.amount)

        // Tap +100 and verify cumulative addition: 500 + 100 = 600
        composeTestRule.onNodeWithTag("chip_amount_100").performClick()
        composeTestRule.waitForIdle()
        assertEquals("600", viewModel.uiState.value.amount)
    }

    @Test
    fun addTransaction_directionTogglesAndSaveButtonUsable() {
        var friendId: Long = 0L
        runBlocking {
            friendId = repository.insertFriend("Pooja")
        }

        val viewModel = own(AddTransactionViewModel(repository, initialFriendId = friendId))

        setTestContent {
            PhittoosTheme(themeMode = AppThemeMode.DARK) {
                AddTransactionScreen(
                    viewModel = viewModel,
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        // Toggle borrowed
        composeTestRule.onNodeWithTag("toggle_borrowed").performClick()
        composeTestRule.waitForIdle()
        assertEquals(TransactionDirection.BORROWED, viewModel.uiState.value.direction)

        // Toggle lent
        composeTestRule.onNodeWithTag("toggle_lent").performClick()
        composeTestRule.waitForIdle()
        assertEquals(TransactionDirection.LENT, viewModel.uiState.value.direction)

        // Save button exists
        composeTestRule.onNodeWithTag("button_save_transaction").assertExists()
    }

    @Test
    fun theme_rendersInBothLightAndDarkModesWithoutCrashing() {
        val homeViewModel = own(HomeViewModel(repository, userPreferences))

        setTestContent {
            PhittoosTheme(themeMode = AppThemeMode.DARK) {
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToAddTransaction = {},
                    onNavigateToFriendDetail = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("fab_add_transaction").assertIsDisplayed()
    }
}
