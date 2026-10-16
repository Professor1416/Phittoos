package com.professor1416.phittoos

import android.content.Context
import android.os.Looper
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.professor1416.phittoos.data.db.AppDatabase
import com.professor1416.phittoos.data.model.Friend
import com.professor1416.phittoos.data.repository.PhittoosRepository
import com.professor1416.phittoos.ui.screens.addtransaction.AddTransactionScreen
import com.professor1416.phittoos.ui.theme.CoralOrangeDark
import com.professor1416.phittoos.ui.theme.EmeraldGreenDark
import com.professor1416.phittoos.ui.theme.PhittoosTheme
import com.professor1416.phittoos.ui.viewmodel.AddTransactionViewModel
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
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w400dp-h1000dp")
class AddTransactionAccessibilityTest {

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

        viewModel = own(AddTransactionViewModel(repository, initialFriendId = testFriendId))
        viewModel.selectFriend(Friend(id = testFriendId, name = "Aman"))
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
    fun testQuickAmountChipsHaveAtLeast48dpTouchTargets() {
        setTestContent {
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
        val job = viewModel.viewModelScope.launch {
            viewModel.uiState.collect()
        }

        setTestContent {
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
        setTestContent {
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

        setTestContent {
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
