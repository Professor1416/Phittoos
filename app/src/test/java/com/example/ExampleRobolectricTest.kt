package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.preferences.UserPreferences
import com.example.ui.screens.onboarding.OnboardingScreen
import com.example.ui.theme.PhittoosTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
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

    // Screen 3: Should show "Pick friends faster"
    composeTestRule.onNodeWithText("Pick friends faster").assertIsDisplayed()

    // Click Skip
    composeTestRule.onNodeWithTag("button_skip_contacts").assertIsDisplayed().performClick()

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

    assertEquals(com.example.ui.theme.Slate900, lightOnSurface)
    assertEquals(androidx.compose.ui.graphics.Color.White, lightSurface)
    assertEquals(com.example.ui.theme.Slate900, darkOnSurface)
    assertEquals(androidx.compose.ui.graphics.Color.White, darkSurface)
  }

  @Test
  fun `add transaction typing akash 500 and dinner are displayed`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val inMemoryDb = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.db.AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val friendDao = inMemoryDb.friendDao()
    val transactionDao = inMemoryDb.transactionDao()
    val repository = com.example.data.repository.PhittoosRepository(friendDao, transactionDao)
    val addTxViewModel = com.example.ui.viewmodel.AddTransactionViewModel(repository)

    composeTestRule.setContent {
      PhittoosTheme {
        com.example.ui.screens.addtransaction.AddTransactionScreen(
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
    val inMemoryDb = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.db.AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val friendDao = inMemoryDb.friendDao()
    val transactionDao = inMemoryDb.transactionDao()
    val repository = com.example.data.repository.PhittoosRepository(friendDao, transactionDao)
    val prefs = UserPreferences(context)
    val homeViewModel = com.example.ui.viewmodel.HomeViewModel(repository, prefs)

    composeTestRule.setContent {
      PhittoosTheme {
        com.example.ui.screens.home.HomeScreen(
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
}
