package com.example

import android.content.Context
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.Friend
import com.example.data.repository.PhittoosRepository
import com.example.ui.screens.addtransaction.AddTransactionScreen
import com.example.ui.theme.CoralOrangeDark
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.PhittoosTheme
import com.example.ui.viewmodel.AddTransactionViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w400dp-h1000dp")
class AddTransactionAccessibilityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var db: AppDatabase
    private lateinit var repository: PhittoosRepository
    private lateinit var viewModel: AddTransactionViewModel
    private var testFriendId: Long = 0L

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = PhittoosRepository(db.friendDao(), db.transactionDao())

        runBlocking {
            testFriendId = repository.insertFriend("Aman")
        }

        viewModel = AddTransactionViewModel(repository, initialFriendId = testFriendId)
        viewModel.selectFriend(Friend(id = testFriendId, name = "Aman"))
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testQuickAmountChipsHaveAtLeast48dpTouchTargets() {
        composeTestRule.setContent {
            PhittoosTheme {
                AddTransactionScreen(
                    viewModel = viewModel,
                    onNavigateBack = {}
                )
            }
        }

        val quickAmounts = listOf(100, 200, 500, 1000, 2000, 5000)
        for (amt in quickAmounts) {
            val chip = composeTestRule.onNodeWithTag("chip_amount_$amt")
            chip.assertExists()
            chip.assertWidthIsAtLeast(48.dp)
            chip.assertHeightIsAtLeast(48.dp)
        }
    }

    @Test
    fun testQuickAmountChipsIncrementAmountCorrectly() {
        val job = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            viewModel.uiState.collect()
        }

        composeTestRule.setContent {
            PhittoosTheme {
                AddTransactionScreen(
                    viewModel = viewModel,
                    onNavigateBack = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // Click +100 chip once -> amount is 100
        composeTestRule.onNodeWithTag("chip_amount_100").performClick()
        composeTestRule.waitForIdle()
        assertEquals("100", viewModel.uiState.value.amount)

        // Click +100 chip again -> increments from 100 to 200
        composeTestRule.onNodeWithTag("chip_amount_100").performClick()
        composeTestRule.waitForIdle()
        assertEquals("200", viewModel.uiState.value.amount)

        job.cancel()
    }

    @Test
    fun testDirectionSelectionExposedAsMutuallyExclusiveRadioGroup() {
        composeTestRule.setContent {
            PhittoosTheme {
                AddTransactionScreen(
                    viewModel = viewModel,
                    onNavigateBack = {}
                )
            }
        }

        val lentButton = composeTestRule.onNodeWithTag("toggle_lent")
        val borrowedButton = composeTestRule.onNodeWithTag("toggle_borrowed")

        // Both options have Role.RadioButton
        lentButton.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
        borrowedButton.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))

        // Initially LENT is selected, BORROWED is not selected
        lentButton.assertIsSelected()
        borrowedButton.assertIsNotSelected()

        // Clicking BORROWED selects it and deselects LENT
        borrowedButton.performClick()
        borrowedButton.assertIsSelected()
        lentButton.assertIsNotSelected()

        // Clicking LENT selects it and deselects BORROWED
        lentButton.performClick()
        lentButton.assertIsSelected()
        borrowedButton.assertIsNotSelected()
    }

    @Test
    fun testAmountFieldHasAccessibleRupeesLabelAndErrorSemantics() {
        val friend = Friend(id = testFriendId, name = "Aman")
        viewModel.selectFriend(friend)
        viewModel.setAmount("0")

        composeTestRule.setContent {
            PhittoosTheme {
                AddTransactionScreen(
                    viewModel = viewModel,
                    onNavigateBack = {}
                )
            }
        }

        // Accessible label exists
        composeTestRule.onNodeWithText("Amount in rupees").assertExists()

        // Trigger error by attempting save with invalid amount ("0")
        viewModel.saveTransaction {}
        composeTestRule.waitForIdle()

        // Amount error is displayed under the amount field accessibly
        composeTestRule.onNodeWithText("Please enter a valid amount").assertExists()
    }

    @Test
    fun testSelectedDirectionColorsMeetWcagContrast() {
        // WCAG AA contrast verification for selected states
        fun relLum(color: androidx.compose.ui.graphics.Color): Double {
            fun ch(c: Float): Double {
                return if (c <= 0.04045f) (c / 12.92).toDouble() else Math.pow(((c + 0.055) / 1.055).toDouble(), 2.4)
            }
            return 0.2126 * ch(color.red) + 0.7152 * ch(color.green) + 0.0722 * ch(color.blue)
        }

        fun contrast(c1: androidx.compose.ui.graphics.Color, c2: androidx.compose.ui.graphics.Color): Double {
            val l1 = relLum(c1)
            val l2 = relLum(c2)
            val lighter = maxOf(l1, l2)
            val darker = minOf(l1, l2)
            return (lighter + 0.05) / (darker + 0.05)
        }

        val white = androidx.compose.ui.graphics.Color.White
        val lentContrast = contrast(white, EmeraldGreenDark)
        val borrowedContrast = contrast(white, CoralOrangeDark)

        // WCAG AA requires >= 4.5:1 for normal text
        assertTrue("EmeraldGreenDark contrast $lentContrast must be >= 4.5", lentContrast >= 4.5)
        assertTrue("CoralOrangeDark contrast $borrowedContrast must be >= 4.5", borrowedContrast >= 4.5)
    }
}
