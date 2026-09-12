package com.example

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import com.example.data.preferences.UserPreferences
import com.example.ui.screens.home.HomeScreen
import com.example.ui.theme.PhittoosTheme
import com.example.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class HomeScreenResponsiveLayoutTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var db: com.example.data.db.AppDatabase
    private lateinit var repository: com.example.data.repository.PhittoosRepository
    private lateinit var prefs: UserPreferences
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.db.AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = com.example.data.repository.PhittoosRepository(db.friendDao(), db.transactionDao())
        prefs = UserPreferences(context)
        viewModel = HomeViewModel(repository, prefs)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun setupMockData(friendName: String, amountLent: Double, amountBorrowed: Double) {
        runBlocking {
            val friendId = db.friendDao().insertFriend(com.example.data.model.Friend(name = friendName))
            if (amountLent > 0) {
                db.transactionDao().insertTransaction(
                    com.example.data.model.TransactionEntity(
                        friendId = friendId,
                        amount = amountLent,
                        direction = com.example.data.model.TransactionDirection.LENT,
                        status = com.example.data.model.TransactionStatus.OPEN
                    )
                )
            }
            if (amountBorrowed > 0) {
                db.transactionDao().insertTransaction(
                    com.example.data.model.TransactionEntity(
                        friendId = friendId,
                        amount = amountBorrowed,
                        direction = com.example.data.model.TransactionDirection.BORROWED,
                        status = com.example.data.model.TransactionStatus.OPEN
                    )
                )
            }
        }
    }

    @Test
    @Config(sdk = [36], qualifiers = "w320dp-h1200dp-xhdpi")
    fun testLayoutAt320dpWidthWith200PercentFontScaleAndLargeAmounts() {
        // Setup Karthikeyan with ₹1,25,000 lent and ₹99,99,999 borrowed
        setupMockData("Karthikeyan Subramaniam Swamy", 125000.0, 9999999.0)

        composeTestRule.setContent {
            val originalDensity = LocalDensity.current
            val customDensity = Density(
                density = originalDensity.density,
                fontScale = 2.0f // 200% font scale
            )
            CompositionLocalProvider(LocalDensity provides customDensity) {
                PhittoosTheme {
                    HomeScreen(
                        viewModel = viewModel,
                        onNavigateToAddTransaction = {},
                        onNavigateToFriendDetail = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        // Verify elements exist and are displayed after scrolling to them
        composeTestRule.onNodeWithText("Karthikeyan Subramaniam Swamy").performScrollTo().assertIsDisplayed()
        
        // Let's check text bounds to ensure there's no clipping/truncation
        val rootBounds = composeTestRule.onRoot().getUnclippedBoundsInRoot()
        val friendNameBounds = composeTestRule.onNodeWithText("Karthikeyan Subramaniam Swamy").getUnclippedBoundsInRoot()
        
        // Assert name stays within viewport bounds
        assertTrue("Name must be within viewport width", friendNameBounds.right <= rootBounds.right)
    }

    @Test
    @Config(sdk = [36], qualifiers = "w360dp-h640dp-xhdpi")
    fun testLayoutAt360dpWidthWithNormalFontScaleAndLargeAmounts() {
        setupMockData("Karthikeyan Subramaniam Swamy", 125000.0, 9999999.0)

        composeTestRule.setContent {
            PhittoosTheme {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToAddTransaction = {},
                    onNavigateToFriendDetail = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Karthikeyan Subramaniam Swamy").performScrollTo().assertIsDisplayed()
    }

    @Test
    @Config(sdk = [36], qualifiers = "w360dp-h1200dp-xhdpi")
    fun testLayoutAt360dpWidthWith200PercentFontScaleAndLargeAmounts() {
        setupMockData("Karthikeyan Subramaniam Swamy", 125000.0, 9999999.0)

        composeTestRule.setContent {
            val originalDensity = LocalDensity.current
            val customDensity = Density(
                density = originalDensity.density,
                fontScale = 2.0f // 200% font scale
            )
            CompositionLocalProvider(LocalDensity provides customDensity) {
                PhittoosTheme {
                    HomeScreen(
                        viewModel = viewModel,
                        onNavigateToAddTransaction = {},
                        onNavigateToFriendDetail = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Karthikeyan Subramaniam Swamy").performScrollTo().assertIsDisplayed()
    }
}
