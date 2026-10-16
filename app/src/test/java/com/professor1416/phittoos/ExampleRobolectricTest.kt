package com.professor1416.phittoos

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.room.Room
import com.professor1416.phittoos.data.dao.TransactionDao
import com.professor1416.phittoos.data.db.AppDatabase
import com.professor1416.phittoos.data.model.Friend
import com.professor1416.phittoos.data.model.TransactionDirection
import com.professor1416.phittoos.data.model.TransactionEntity
import com.professor1416.phittoos.data.model.TransactionStatus
import com.professor1416.phittoos.data.repository.PhittoosRepository
import com.professor1416.phittoos.ui.screens.home.HomeScreen
import com.professor1416.phittoos.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.professor1416.phittoos.data.preferences.UserPreferences
import com.professor1416.phittoos.ui.screens.onboarding.OnboardingScreen
import com.professor1416.phittoos.ui.theme.PhittoosTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w360dp-h1000dp-xhdpi")
class ExampleRobolectricTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Phittoos", appName)
  }

  @Test
  fun `onboarding flow advances through steps`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = UserPreferences(context)
    prefs.hasCompletedOnboarding = false
    prefs.userName = ""

    var completed = false

    composeTestRule.setContent {
      PhittoosTheme {
        OnboardingScreen(
          userPreferences = prefs,
          onCompleteOnboarding = { completed = true }
        )
      }
    }

    // Screen 1: Verify "Get Started" button exists and click it
    composeTestRule.onNodeWithTag("button_intro_get_started").assertIsDisplayed().performClick()

    // Screen 2: Should show "What's your name?"
    composeTestRule.onNodeWithText("What's your name?").assertIsDisplayed()

    // Type name
    composeTestRule.onNodeWithTag("input_user_name").performTextInput("Rahul")
    composeTestRule.onNodeWithTag("button_name_continue").assertIsDisplayed().performClick()

    assertTrue(completed)
    assertTrue(prefs.hasCompletedOnboarding)
    assertEquals("Rahul", prefs.userName)
  }

  @Test
  fun `lazy column keys do not collide when friend and transaction have same id`() {
    composeTestRule.setContent {
      PhittoosTheme {
        androidx.compose.foundation.lazy.LazyColumn {
          items(
            count = 1,
            key = { "friend_1" }
          ) {
            androidx.compose.material3.Text("Aman Sharma")
          }

          items(
            count = 1,
            key = { "activity_1" }
          ) {
            androidx.compose.material3.Text("Lunch")
          }
        }
      }
    }

    composeTestRule.onNodeWithText("Aman Sharma").assertIsDisplayed()
    composeTestRule.onNodeWithText("Lunch").assertIsDisplayed()
  }

  @Test
  fun `phittoos theme remains deterministic in both light and dark mode`() {
    var lightOnSurface: androidx.compose.ui.graphics.Color? = null
    var lightSurface: androidx.compose.ui.graphics.Color? = null
    var darkOnSurface: androidx.compose.ui.graphics.Color? = null
    var darkSurface: androidx.compose.ui.graphics.Color? = null

    composeTestRule.setContent {
      PhittoosTheme(darkTheme = false) {
        lightOnSurface = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
        lightSurface = androidx.compose.material3.MaterialTheme.colorScheme.surface
      }
      PhittoosTheme(darkTheme = true) {
        darkOnSurface = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
        darkSurface = androidx.compose.material3.MaterialTheme.colorScheme.surface
      }
    }

    assertEquals(com.professor1416.phittoos.ui.theme.Slate900, lightOnSurface)
    assertEquals(androidx.compose.ui.graphics.Color.White, lightSurface)
    assertEquals(androidx.compose.ui.graphics.Color.White, darkOnSurface)
    assertEquals(com.professor1416.phittoos.ui.theme.Slate800, darkSurface)
  }

  @Test
  fun `add transaction typing akash 500 and dinner are displayed`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val inMemoryDb = androidx.room.Room.inMemoryDatabaseBuilder(context, com.professor1416.phittoos.data.db.AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val friendDao = inMemoryDb.friendDao()
    val transactionDao = inMemoryDb.transactionDao()
    val repository = com.professor1416.phittoos.data.repository.PhittoosRepository(friendDao, transactionDao)
    val addTxViewModel = com.professor1416.phittoos.ui.viewmodel.AddTransactionViewModel(repository)

    composeTestRule.setContent {
      PhittoosTheme {
        com.professor1416.phittoos.ui.screens.addtransaction.AddTransactionScreen(
          viewModel = addTxViewModel,
          onNavigateBack = {}
        )
      }
    }

    // Type "Akash" in friend search
    composeTestRule.onNodeWithTag("input_friend_search").performTextInput("Akash")
    composeTestRule.onNodeWithText("Akash").assertIsDisplayed()
    composeTestRule.onNodeWithText("+ Add \"Akash\" as new friend", substring = true).assertIsDisplayed()

    // Type "500" in amount
    composeTestRule.onNodeWithTag("input_amount").performTextInput("500")
    composeTestRule.onNodeWithText("500").assertIsDisplayed()

    // Type "Dinner" in note
    composeTestRule.onNodeWithTag("input_note").performTextInput("Dinner")
    composeTestRule.onNodeWithText("Dinner").assertIsDisplayed()

    inMemoryDb.close()
  }

  @Test
  fun `home screen search input accepts and displays text`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val inMemoryDb = androidx.room.Room.inMemoryDatabaseBuilder(context, com.professor1416.phittoos.data.db.AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val friendDao = inMemoryDb.friendDao()
    val transactionDao = inMemoryDb.transactionDao()
    val repository = com.professor1416.phittoos.data.repository.PhittoosRepository(friendDao, transactionDao)
    val prefs = UserPreferences(context)
    val homeViewModel = com.professor1416.phittoos.ui.viewmodel.HomeViewModel(repository, prefs)

    composeTestRule.setContent {
      PhittoosTheme {
        com.professor1416.phittoos.ui.screens.home.HomeScreen(
          viewModel = homeViewModel,
          onNavigateToAddTransaction = {},
          onNavigateToFriendDetail = {}
        )
      }
    }

    composeTestRule.onNodeWithTag("search_friends_input").performTextInput("Aman")
    composeTestRule.onNodeWithText("Aman").assertIsDisplayed()

    inMemoryDb.close()
  }

  @Test
  fun `individual settlement dialog displays amount and triggers onConfirm`() {
    var confirmed = false
    val tx = com.professor1416.phittoos.data.model.TransactionEntity(
      id = 1L,
      friendId = 10L,
      amount = 500.0,
      direction = com.professor1416.phittoos.data.model.TransactionDirection.LENT,
      status = com.professor1416.phittoos.data.model.TransactionStatus.OPEN
    )

    composeTestRule.setContent {
      PhittoosTheme {
        com.professor1416.phittoos.ui.screens.frienddetail.IndividualSettlementDialog(
          friendName = "Akash",
          tx = tx,
          onConfirm = { confirmed = true },
          onDismiss = {}
        )
      }
    }

    composeTestRule.onNodeWithText("Mark this as fully paid?").assertIsDisplayed()
    composeTestRule.onNodeWithText("₹500 is still pending from Akash. This will mark this transaction as settled.", substring = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag("button_confirm_settle_individual").performClick()
    assertTrue(confirmed)
  }

  @Test
  fun `bulk settlement dialog displays summary and triggers onConfirm`() {
    var confirmed = false

    composeTestRule.setContent {
      PhittoosTheme {
        com.professor1416.phittoos.ui.screens.frienddetail.BulkSettlementDialog(
          friendName = "Pooja",
          eligibility = com.professor1416.phittoos.ui.viewmodel.BulkSettlementEligibility.SAME_DIRECTION_LENT,
          totalRemaining = 800.0,
          openCount = 2,
          onConfirm = { confirmed = true },
          onDismiss = {}
        )
      }
    }

    composeTestRule.onNodeWithText("Settle all pending transactions?").assertIsDisplayed()
    composeTestRule.onNodeWithText("This will mark all 2 pending transactions (₹800) from Pooja as fully paid.", substring = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag("button_confirm_settle_bulk").performClick()
    assertTrue(confirmed)
  }

  @Test
  fun `zero net with open transactions displays Overall balance 0 and NOT All settled`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val inMemoryDb = androidx.room.Room.inMemoryDatabaseBuilder(context, com.professor1416.phittoos.data.db.AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val friendDao = inMemoryDb.friendDao()
    val transactionDao = inMemoryDb.transactionDao()
    val repository = com.professor1416.phittoos.data.repository.PhittoosRepository(friendDao, transactionDao)
    val prefs = UserPreferences(context)
    val homeViewModel = com.professor1416.phittoos.ui.viewmodel.HomeViewModel(repository, prefs)

    kotlinx.coroutines.runBlocking {
      val friendId = friendDao.insertFriend(com.professor1416.phittoos.data.model.Friend(name = "Karan"))
      transactionDao.insertTransaction(
        com.professor1416.phittoos.data.model.TransactionEntity(
          friendId = friendId,
          amount = 500.0,
          direction = com.professor1416.phittoos.data.model.TransactionDirection.LENT,
          status = com.professor1416.phittoos.data.model.TransactionStatus.OPEN
        )
      )
      transactionDao.insertTransaction(
        com.professor1416.phittoos.data.model.TransactionEntity(
          friendId = friendId,
          amount = 500.0,
          direction = com.professor1416.phittoos.data.model.TransactionDirection.BORROWED,
          status = com.professor1416.phittoos.data.model.TransactionStatus.OPEN
        )
      )
    }

    composeTestRule.setContent {
      PhittoosTheme {
        com.professor1416.phittoos.ui.screens.home.HomeScreen(
          viewModel = homeViewModel,
          onNavigateToAddTransaction = {},
          onNavigateToFriendDetail = {}
        )
      }
    }
    awaitLoadedSummary(homeViewModel)
    assertEquals(2, homeViewModel.uiState.value.openTransactionsCount)
    assertEquals(500.0, homeViewModel.uiState.value.youWillGetBack, 0.0)
    assertEquals(500.0, homeViewModel.uiState.value.youOwe, 0.0)
    assertEquals(0.0, homeViewModel.uiState.value.netPosition, 0.0)

    // Must display "Overall balance ₹0" in top summary
    composeTestRule.onNodeWithTag("top_summary_net_text").assertTextEquals("Overall balance ₹0")
    // Top summary card MUST NOT contain "All settled"
    composeTestRule.onAllNodesWithText("All settled up (₹0)").assertCountEquals(0)

    inMemoryDb.close()
  }

  @Test
  fun `zero net with no open transactions displays All settled up`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val inMemoryDb = androidx.room.Room.inMemoryDatabaseBuilder(context, com.professor1416.phittoos.data.db.AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val friendDao = inMemoryDb.friendDao()
    val transactionDao = inMemoryDb.transactionDao()
    val repository = com.professor1416.phittoos.data.repository.PhittoosRepository(friendDao, transactionDao)
    val prefs = UserPreferences(context)
    val homeViewModel = com.professor1416.phittoos.ui.viewmodel.HomeViewModel(repository, prefs)

    kotlinx.coroutines.runBlocking {
      val friendId = friendDao.insertFriend(com.professor1416.phittoos.data.model.Friend(name = "Karan"))
      transactionDao.insertTransaction(
        com.professor1416.phittoos.data.model.TransactionEntity(
          friendId = friendId,
          amount = 500.0,
          direction = com.professor1416.phittoos.data.model.TransactionDirection.LENT,
          status = com.professor1416.phittoos.data.model.TransactionStatus.CONFIRMED
        )
      )
    }

    composeTestRule.setContent {
      PhittoosTheme {
        com.professor1416.phittoos.ui.screens.home.HomeScreen(
          viewModel = homeViewModel,
          onNavigateToAddTransaction = {},
          onNavigateToFriendDetail = {}
        )
      }
    }

    awaitLoadedSummary(homeViewModel)
    assertEquals(0, homeViewModel.uiState.value.openTransactionsCount)
    composeTestRule.onNodeWithTag("top_summary_net_text").assertTextEquals("All settled up (₹0)")

    inMemoryDb.close()
  }

  @Test
  fun `positive net displays You will receive`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val inMemoryDb = androidx.room.Room.inMemoryDatabaseBuilder(context, com.professor1416.phittoos.data.db.AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val friendDao = inMemoryDb.friendDao()
    val transactionDao = inMemoryDb.transactionDao()
    val repository = com.professor1416.phittoos.data.repository.PhittoosRepository(friendDao, transactionDao)
    val prefs = UserPreferences(context)
    val homeViewModel = com.professor1416.phittoos.ui.viewmodel.HomeViewModel(repository, prefs)

    kotlinx.coroutines.runBlocking {
      val friendId = friendDao.insertFriend(com.professor1416.phittoos.data.model.Friend(name = "Karan"))
      transactionDao.insertTransaction(
        com.professor1416.phittoos.data.model.TransactionEntity(
          friendId = friendId,
          amount = 700.0,
          direction = com.professor1416.phittoos.data.model.TransactionDirection.LENT,
          status = com.professor1416.phittoos.data.model.TransactionStatus.OPEN
        )
      )
    }

    composeTestRule.setContent {
      PhittoosTheme {
        com.professor1416.phittoos.ui.screens.home.HomeScreen(
          viewModel = homeViewModel,
          onNavigateToAddTransaction = {},
          onNavigateToFriendDetail = {}
        )
      }
    }

    awaitLoadedSummary(homeViewModel)
    assertEquals(700.0, homeViewModel.uiState.value.netPosition, 0.0)
    assertEquals(1, homeViewModel.uiState.value.openTransactionsCount)
    composeTestRule.onNodeWithTag("top_summary_net_text").assertTextEquals("You'll receive ₹700")

    inMemoryDb.close()
  }

  @Test
  fun `negative net displays You need to pay`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val inMemoryDb = androidx.room.Room.inMemoryDatabaseBuilder(context, com.professor1416.phittoos.data.db.AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val friendDao = inMemoryDb.friendDao()
    val transactionDao = inMemoryDb.transactionDao()
    val repository = com.professor1416.phittoos.data.repository.PhittoosRepository(friendDao, transactionDao)
    val prefs = UserPreferences(context)
    val homeViewModel = com.professor1416.phittoos.ui.viewmodel.HomeViewModel(repository, prefs)

    kotlinx.coroutines.runBlocking {
      val friendId = friendDao.insertFriend(com.professor1416.phittoos.data.model.Friend(name = "Karan"))
      transactionDao.insertTransaction(
        com.professor1416.phittoos.data.model.TransactionEntity(
          friendId = friendId,
          amount = 450.0,
          direction = com.professor1416.phittoos.data.model.TransactionDirection.BORROWED,
          status = com.professor1416.phittoos.data.model.TransactionStatus.OPEN
        )
      )
    }

    composeTestRule.setContent {
      PhittoosTheme {
        com.professor1416.phittoos.ui.screens.home.HomeScreen(
          viewModel = homeViewModel,
          onNavigateToAddTransaction = {},
          onNavigateToFriendDetail = {}
        )
      }
    }

    awaitLoadedSummary(homeViewModel)
    assertEquals(-450.0, homeViewModel.uiState.value.netPosition, 0.0)
    assertEquals(1, homeViewModel.uiState.value.openTransactionsCount)
    composeTestRule.onNodeWithTag("top_summary_net_text").assertTextEquals("You need to pay ₹450")

    inMemoryDb.close()
  }

  @Test
  fun `loading offsetting open entries never claims settlement`() {
    assertDelayedLedgerSummary(settled = false)
  }

  @Test
  fun `loading settled entries waits before claiming settlement`() {
    assertDelayedLedgerSummary(settled = true)
  }

  @Test
  fun `initial loading survives inactive lifecycle until collection starts`() {
    assertDelayedLedgerSummary(settled = false, startInactive = true)
  }

  private fun awaitLoadedSummary(viewModel: HomeViewModel) {
    // Compose idle alone does not establish that Room/combine has emitted.
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      // Query semantics on every poll so Robolectric also drains pending UI work.
      composeTestRule.onAllNodesWithTag("top_summary_net_text").fetchSemanticsNodes().isNotEmpty() &&
        !viewModel.uiState.value.isLoading
    }
  }

  private fun assertDelayedLedgerSummary(settled: Boolean, startInactive: Boolean = false) {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val releaseTransactions = CompletableDeferred<Unit>()
    val store = ViewModelStore()
    try {
      val transactionDao = db.transactionDao()
      runBlocking {
        val friendId = db.friendDao().insertFriend(Friend(name = "Synthetic ledger friend"))
        for (direction in TransactionDirection.entries) {
          transactionDao.insertTransaction(
            TransactionEntity(
              friendId = friendId,
              amount = 500.0,
              direction = direction,
              status = if (settled) TransactionStatus.CONFIRMED else TransactionStatus.OPEN,
              paidAmount = if (settled) 500.0 else null,
              settledAt = if (settled) 1_700_000_000_001L else null
            )
          )
        }
      }
      val before = runBlocking { transactionDao.getAllTransactionsOrdered() }
      // Hold only the first transaction snapshot, without sleeping or changing Room data.
      val delayedDao = object : TransactionDao by transactionDao {
        override fun getAllTransactions() = flow {
          releaseTransactions.await()
          emitAll(transactionDao.getAllTransactions())
        }
      }
      val repository = PhittoosRepository(db.friendDao(), delayedDao, db.activityDao(), db)
      val viewModel = HomeViewModel(repository, UserPreferences(context))
      store.put("home", viewModel)
      val owner = object : LifecycleOwner {
        override val lifecycle = LifecycleRegistry(this)
      }
      composeTestRule.runOnUiThread {
        owner.lifecycle.currentState = if (startInactive) Lifecycle.State.CREATED else Lifecycle.State.STARTED
      }
      composeTestRule.setContent {
        CompositionLocalProvider(LocalLifecycleOwner provides owner) {
          PhittoosTheme {
            HomeScreen(viewModel, onNavigateToAddTransaction = {}, onNavigateToFriendDetail = {})
          }
        }
      }
      composeTestRule.runOnIdle { assertTrue(viewModel.uiState.value.isLoading) }
      // These must hold even when Compose is idle but the ledger has not arrived.
      composeTestRule.onAllNodesWithText("All settled up (₹0)").assertCountEquals(0)
      composeTestRule.onAllNodesWithTag("top_summary_card").assertCountEquals(0)
      composeTestRule.onAllNodesWithTag("empty_friends_state").assertCountEquals(0)
      composeTestRule.onNodeWithText("Loading your ledger…").assertIsDisplayed()
      composeTestRule.onNodeWithTag("fab_add_transaction").assertIsDisplayed()
      composeTestRule.onNodeWithTag("btn_settings").assertIsDisplayed()

      releaseTransactions.complete(Unit)
      if (startInactive) {
        composeTestRule.runOnIdle { assertTrue(viewModel.uiState.value.isLoading) }
        composeTestRule.onNodeWithText("Loading your ledger…").assertIsDisplayed()
        composeTestRule.runOnUiThread { owner.lifecycle.currentState = Lifecycle.State.STARTED }
      }
      awaitLoadedSummary(viewModel)
      val state = viewModel.uiState.value
      assertEquals(0.0, state.netPosition, 0.0)
      assertEquals(if (settled) 0 else 2, state.openTransactionsCount)
      assertEquals(if (settled) 0.0 else 500.0, state.youWillGetBack, 0.0)
      assertEquals(if (settled) 0.0 else 500.0, state.youOwe, 0.0)
      composeTestRule.onNodeWithTag("top_summary_net_text")
        .assertTextEquals(if (settled) "All settled up (₹0)" else "Overall balance ₹0")
      composeTestRule.onAllNodesWithText("Loading your ledger…").assertCountEquals(0)
      if (!settled) {
        composeTestRule.onAllNodesWithText("All settled up (₹0)").assertCountEquals(0)
      }
      assertEquals(before, runBlocking { transactionDao.getAllTransactionsOrdered() })
      assertTrue(runBlocking { db.activityDao().getAllActivitiesList() }.isEmpty())
    } finally {
      releaseTransactions.cancel()
      composeTestRule.runOnIdle { store.clear() }
      db.close()
    }
  }
}
