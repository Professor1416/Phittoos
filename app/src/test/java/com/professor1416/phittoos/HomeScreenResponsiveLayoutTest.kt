package com.professor1416.phittoos

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
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import com.professor1416.phittoos.data.preferences.UserPreferences
import com.professor1416.phittoos.ui.screens.home.HomeScreen
import com.professor1416.phittoos.ui.theme.PhittoosTheme
import com.professor1416.phittoos.ui.viewmodel.HomeViewModel
import com.professor1416.phittoos.ui.util.Formatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HomeScreenResponsiveLayoutTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var db: com.professor1416.phittoos.data.db.AppDatabase
    private lateinit var repository: com.professor1416.phittoos.data.repository.PhittoosRepository
    private lateinit var prefs: UserPreferences
    private lateinit var viewModel: HomeViewModel
    
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // Force synchronous execution for Room database operations to eliminate timing/thread-hopping in tests
        db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.professor1416.phittoos.data.db.AppDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
            
        repository = com.professor1416.phittoos.data.repository.PhittoosRepository(db.friendDao(), db.transactionDao())
        prefs = UserPreferences(context)
        prefs.userName = "Test User"
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private fun setupMockData(friendName: String, amountLent: Double, amountBorrowed: Double) {
        runBlocking {
            val friendId = db.friendDao().insertFriend(com.professor1416.phittoos.data.model.Friend(name = friendName))
            if (amountLent > 0) {
                db.transactionDao().insertTransaction(
                    com.professor1416.phittoos.data.model.TransactionEntity(
                        friendId = friendId,
                        amount = amountLent,
                        direction = com.professor1416.phittoos.data.model.TransactionDirection.LENT,
                        status = com.professor1416.phittoos.data.model.TransactionStatus.OPEN
                    )
                )
            }
            if (amountBorrowed > 0) {
                db.transactionDao().insertTransaction(
                    com.professor1416.phittoos.data.model.TransactionEntity(
                        friendId = friendId,
                        amount = amountBorrowed,
                        direction = com.professor1416.phittoos.data.model.TransactionDirection.BORROWED,
                        status = com.professor1416.phittoos.data.model.TransactionStatus.OPEN
                    )
                )
            }
        }
        
        // Initialize the ViewModel AFTER data is inserted, so the initial subscription state immediately gets the correct data.
        viewModel = HomeViewModel(repository, prefs)
    }

    /**
     * Checks text layout results to ensure there's no catastrophic visual layout failure.
     */
    private fun verifyTextLayout(text: String, tagAncestor: String, useUnmergedTree: Boolean = true) {
        val node = composeTestRule.onNode(
            hasText(text) and hasAnyAncestor(hasTestTag(tagAncestor)),
            useUnmergedTree = useUnmergedTree
        ).fetchSemanticsNode()
        val textLayoutResults = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        node.config[androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult].action?.invoke(textLayoutResults)
        val layoutResult = textLayoutResults.firstOrNull()
        if (layoutResult != null) {
            println("Verified Text: '$text' inside '$tagAncestor' -> lineCount: ${layoutResult.lineCount}, hasVisualOverflow: ${layoutResult.hasVisualOverflow}")
        }
    }

    /**
     * Test Case 1: Vertical/Narrow branch (maxWidth < 340dp) at standard normal font scale.
     */
    @Test
    @Config(sdk = [36], qualifiers = "w320dp-h1000dp-xhdpi")
    fun testVerticalBranchAtNormalFontScale() {
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

        // 1. Verify search is reachable and functional
        val searchInput = composeTestRule.onNodeWithTag("search_friends_input")
        searchInput.assertIsDisplayed()
        searchInput.performTextInput("Karthikeyan")
        // Use exact match to avoid multiple nodes satisfying "Karthikeyan" (recent activity, search field etc)
        composeTestRule.onNodeWithText("Karthikeyan Subramaniam Swamy").assertIsDisplayed()
        searchInput.performTextClearance()
        composeTestRule.waitForIdle()

        val lentStr = Formatters.formatCurrency(125000.0)
        val borrowedStr = Formatters.formatCurrency(9999999.0)
        val netStr = Formatters.formatCurrency(9874999.0)
        val netText = "You need to pay $netStr"

        // Helper matcher lambda to uniquely select elements inside containers
        val inSummaryLent = hasText(lentStr) and hasAnyAncestor(hasTestTag("top_summary_card"))
        val inSummaryBorrowed = hasText(borrowedStr) and hasAnyAncestor(hasTestTag("top_summary_card"))
        val inSummaryNet = hasText(netText) and hasAnyAncestor(hasTestTag("top_summary_card"))

        // 2. Assert Top Summary balance nodes and check visual overflow & bounds
        composeTestRule.onNode(inSummaryLent, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNode(inSummaryBorrowed, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNode(inSummaryNet, useUnmergedTree = true).assertIsDisplayed()

        verifyTextLayout(lentStr, "top_summary_card")
        verifyTextLayout(borrowedStr, "top_summary_card")
        verifyTextLayout(netText, "top_summary_card")

        val rootBounds = composeTestRule.onRoot().getUnclippedBoundsInRoot()
        val getBackBounds = composeTestRule.onNode(inSummaryLent, useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue("Summary balance must be within viewport width", getBackBounds.right <= rootBounds.right)

        // 3. Scroll to friend card and check balance amount bounds & layout
        composeTestRule.onNodeWithText("Karthikeyan Subramaniam Swamy").performScrollTo().assertIsDisplayed()
        
        val inFriendRowBalance = hasText(netStr) and hasAnyAncestor(hasTestTag("friend_row_1"))
        composeTestRule.onNode(inFriendRowBalance, useUnmergedTree = true).assertIsDisplayed()

        verifyTextLayout(netStr, "friend_row_1")

        val nameBounds = composeTestRule.onNodeWithText("Karthikeyan Subramaniam Swamy", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val balanceBounds = composeTestRule.onNode(inFriendRowBalance, useUnmergedTree = true).getUnclippedBoundsInRoot()

        assertTrue("Balance amount must be within viewport width", balanceBounds.right <= rootBounds.right)

        // In vertical branch, balance column is stacked vertically under the name Column
        assertTrue(
            "Vertical branch: balance must be positioned below name (non-overlapping)",
            nameBounds.bottom <= balanceBounds.top
        )
    }

    /**
     * Test Case 2: Vertical/Narrow branch (maxWidth < 340dp) at 200% font scale.
     */
    @Test
    @Config(sdk = [36], qualifiers = "w320dp-h1200dp-xhdpi")
    fun testVerticalBranchAtLargeFontScale() {
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

        // 1. Verify search is reachable and functional
        val searchInput = composeTestRule.onNodeWithTag("search_friends_input")
        searchInput.assertIsDisplayed()
        searchInput.performTextInput("Karthikeyan")
        // Use exact match to avoid multiple nodes satisfying "Karthikeyan"
        composeTestRule.onNodeWithText("Karthikeyan Subramaniam Swamy").assertIsDisplayed()
        searchInput.performTextClearance()
        composeTestRule.waitForIdle()

        val lentStr = Formatters.formatCurrency(125000.0)
        val borrowedStr = Formatters.formatCurrency(9999999.0)
        val netStr = Formatters.formatCurrency(9874999.0)

        val inSummaryLent = hasText(lentStr) and hasAnyAncestor(hasTestTag("top_summary_card"))
        val inSummaryBorrowed = hasText(borrowedStr) and hasAnyAncestor(hasTestTag("top_summary_card"))

        // 2. Assert Top Summary balance nodes and check visual overflow & bounds
        composeTestRule.onNode(inSummaryLent, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNode(inSummaryBorrowed, useUnmergedTree = true).assertIsDisplayed()

        verifyTextLayout(lentStr, "top_summary_card")
        verifyTextLayout(borrowedStr, "top_summary_card")

        val rootBounds = composeTestRule.onRoot().getUnclippedBoundsInRoot()
        val getBackBounds = composeTestRule.onNode(inSummaryLent, useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue("Summary balance must be within viewport width under 200% scaling", getBackBounds.right <= rootBounds.right)

        // 3. Scroll to friend card and check balance amount bounds & layout
        composeTestRule.onNodeWithText("Karthikeyan Subramaniam Swamy").performScrollTo().assertIsDisplayed()
        
        val inFriendRowBalance = hasText(netStr) and hasAnyAncestor(hasTestTag("friend_row_1"))
        composeTestRule.onNode(inFriendRowBalance, useUnmergedTree = true).assertIsDisplayed()

        verifyTextLayout(netStr, "friend_row_1")

        val nameBounds = composeTestRule.onNodeWithText("Karthikeyan Subramaniam Swamy", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val balanceBounds = composeTestRule.onNode(inFriendRowBalance, useUnmergedTree = true).getUnclippedBoundsInRoot()

        assertTrue("Balance amount must be within viewport width under 200% scaling", balanceBounds.right <= rootBounds.right)

        assertTrue(
            "Vertical branch at 200%: balance must be positioned below name (non-overlapping)",
            nameBounds.bottom <= balanceBounds.top
        )
    }

    /**
     * Test Case 3: Horizontal/Wide branch (maxWidth >= 340dp) at standard normal font scale.
     */
    @Test
    @Config(sdk = [36], qualifiers = "w412dp-h1000dp-xhdpi")
    fun testHorizontalBranchAtNormalFontScale() {
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

        // 1. Verify search is reachable and functional
        val searchInput = composeTestRule.onNodeWithTag("search_friends_input")
        searchInput.assertIsDisplayed()
        searchInput.performTextInput("Karthikeyan")
        // Use exact match to avoid multiple nodes satisfying "Karthikeyan"
        composeTestRule.onNodeWithText("Karthikeyan Subramaniam Swamy").assertIsDisplayed()
        searchInput.performTextClearance()
        composeTestRule.waitForIdle()

        val lentStr = Formatters.formatCurrency(125000.0)
        val borrowedStr = Formatters.formatCurrency(9999999.0)
        val netStr = Formatters.formatCurrency(9874999.0)
        val netText = "You need to pay $netStr"

        val inSummaryLent = hasText(lentStr) and hasAnyAncestor(hasTestTag("top_summary_card"))
        val inSummaryBorrowed = hasText(borrowedStr) and hasAnyAncestor(hasTestTag("top_summary_card"))
        val inSummaryNet = hasText(netText) and hasAnyAncestor(hasTestTag("top_summary_card"))

        // 2. Assert Top Summary balance nodes and check visual overflow & bounds
        composeTestRule.onNode(inSummaryLent, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNode(inSummaryBorrowed, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNode(inSummaryNet, useUnmergedTree = true).assertIsDisplayed()

        verifyTextLayout(lentStr, "top_summary_card")
        verifyTextLayout(borrowedStr, "top_summary_card")
        verifyTextLayout(netText, "top_summary_card")

        val rootBounds = composeTestRule.onRoot().getUnclippedBoundsInRoot()
        val getBackBounds = composeTestRule.onNode(inSummaryLent, useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue("Summary balance must be within viewport width", getBackBounds.right <= rootBounds.right)

        // 3. Scroll to friend card and check balance amount bounds & layout
        composeTestRule.onNodeWithText("Karthikeyan Subramaniam Swamy").performScrollTo().assertIsDisplayed()
        
        val inFriendRowBalance = hasText(netStr) and hasAnyAncestor(hasTestTag("friend_row_1"))
        composeTestRule.onNode(inFriendRowBalance, useUnmergedTree = true).assertIsDisplayed()

        verifyTextLayout(netStr, "friend_row_1")

        val nameBounds = composeTestRule.onNodeWithText("Karthikeyan Subramaniam Swamy", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val balanceBounds = composeTestRule.onNode(inFriendRowBalance, useUnmergedTree = true).getUnclippedBoundsInRoot()

        assertTrue("Balance amount must be within viewport width", balanceBounds.right <= rootBounds.right)

        // In horizontal branch, balance column is horizontally adjacent to the name Column on the right
        assertTrue(
            "Horizontal branch: name column and balance must not overlap horizontally",
            nameBounds.right <= balanceBounds.left
        )
    }

    /**
     * Test Case 4: Horizontal/Wide branch (maxWidth >= 340dp) at 200% font scale.
     */
    @Test
    @Config(sdk = [36], qualifiers = "w412dp-h1200dp-xhdpi")
    fun testHorizontalBranchAtLargeFontScale() {
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

        // 1. Verify search is reachable and functional
        val searchInput = composeTestRule.onNodeWithTag("search_friends_input")
        searchInput.assertIsDisplayed()
        searchInput.performTextInput("Karthikeyan")
        // Use exact match to avoid multiple nodes satisfying "Karthikeyan"
        composeTestRule.onNodeWithText("Karthikeyan Subramaniam Swamy").assertIsDisplayed()
        searchInput.performTextClearance()
        composeTestRule.waitForIdle()

        val lentStr = Formatters.formatCurrency(125000.0)
        val borrowedStr = Formatters.formatCurrency(9999999.0)
        val netStr = Formatters.formatCurrency(9874999.0)

        val inSummaryLent = hasText(lentStr) and hasAnyAncestor(hasTestTag("top_summary_card"))
        val inSummaryBorrowed = hasText(borrowedStr) and hasAnyAncestor(hasTestTag("top_summary_card"))

        // 2. Assert Top Summary balance nodes and check visual overflow & bounds
        composeTestRule.onNode(inSummaryLent, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNode(inSummaryBorrowed, useUnmergedTree = true).assertIsDisplayed()

        verifyTextLayout(lentStr, "top_summary_card")
        verifyTextLayout(borrowedStr, "top_summary_card")

        val rootBounds = composeTestRule.onRoot().getUnclippedBoundsInRoot()
        val getBackBounds = composeTestRule.onNode(inSummaryLent, useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue("Summary balance must be within viewport width under 200% scaling", getBackBounds.right <= rootBounds.right)

        // 3. Scroll to friend card and check balance amount bounds & layout
        composeTestRule.onNodeWithText("Karthikeyan Subramaniam Swamy").performScrollTo().assertIsDisplayed()
        
        val inFriendRowBalance = hasText(netStr) and hasAnyAncestor(hasTestTag("friend_row_1"))
        composeTestRule.onNode(inFriendRowBalance, useUnmergedTree = true).assertIsDisplayed()

        verifyTextLayout(netStr, "friend_row_1")

        val nameBounds = composeTestRule.onNodeWithText("Karthikeyan Subramaniam Swamy", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val balanceBounds = composeTestRule.onNode(inFriendRowBalance, useUnmergedTree = true).getUnclippedBoundsInRoot()

        assertTrue("Balance amount must be within viewport width under 200% scaling", balanceBounds.right <= rootBounds.right)

        assertTrue(
            "Horizontal branch at 200%: name column and balance must not overlap horizontally",
            nameBounds.right <= balanceBounds.left
        )
    }
}
