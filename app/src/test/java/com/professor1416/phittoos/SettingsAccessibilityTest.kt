package com.professor1416.phittoos

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.professor1416.phittoos.data.db.AppDatabase
import com.professor1416.phittoos.data.preferences.UserPreferences
import com.professor1416.phittoos.data.repository.PhittoosRepository
import com.professor1416.phittoos.ui.screens.settings.SettingsScreen
import com.professor1416.phittoos.ui.theme.PhittoosTheme
import com.professor1416.phittoos.ui.viewmodel.SettingsViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w320dp-h480dp") // Simulate compact screen size
class SettingsAccessibilityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var db: AppDatabase
    private lateinit var repository: PhittoosRepository
    private lateinit var userPreferences: UserPreferences
    private lateinit var viewModel: SettingsViewModel
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = PhittoosRepository(db.friendDao(), db.transactionDao(), db.activityDao())
        userPreferences = UserPreferences(context)
        userPreferences.clearAll()

        viewModel = SettingsViewModel(repository, userPreferences)
    }

    @After
    fun tearDown() {
        userPreferences.clearAll()
        db.close()
    }

    @Test
    fun testReminderRowAccessibilitySemanticsAndToggleBehavior() {
        // Force reminders to be enabled initially
        userPreferences.remindersEnabled = true
        viewModel.refreshState(context)

        composeTestRule.setContent {
            PhittoosTheme {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = {},
                    onDataCleared = {}
                )
            }
        }

        // 1. Verify that the Reminder Row has Role.Switch and is displayed
        val reminderRow = composeTestRule.onNodeWithTag("row_repayment_reminders")
        reminderRow.performScrollTo().assertIsDisplayed()

        // Verify minimum 48dp touch target
        reminderRow.assertWidthIsAtLeast(48.dp)
        reminderRow.assertHeightIsAtLeast(48.dp)

        // Verify the semantic properties of the merged Row (Role.Switch) and ToggleableState
        reminderRow.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
        reminderRow.assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On))

        // Verify there is only one actionable switch node for this preference in the accessibility tree
        composeTestRule.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            .assertCountEquals(1)

        // 2. Click the Row at different coordinate regions and verify preference toggles exactly once per tap.
        assertTrue(userPreferences.remindersEnabled)
        assertTrue(viewModel.uiState.value.remindersEnabled)

        // Tap near the label on the left side of the row using touch input coordinates
        reminderRow.performTouchInput {
            click(position = androidx.compose.ui.geometry.Offset(width * 0.25f, height / 2f))
        }

        assertFalse(userPreferences.remindersEnabled)
        assertFalse(viewModel.uiState.value.remindersEnabled)
        reminderRow.assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.Off))

        // Tap near the switch on the right side of the row using touch input coordinates
        reminderRow.performTouchInput {
            click(position = androidx.compose.ui.geometry.Offset(width * 0.85f, height / 2f))
        }

        assertTrue(userPreferences.remindersEnabled)
        assertTrue(viewModel.uiState.value.remindersEnabled)
        reminderRow.assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On))
    }

    @Test
    fun testLongProfileNameWrapsAndEditActionIsInteractive() {
        // Set a long supported profile name
        userPreferences.userName = "Karthikeyan Venkatachalam Swamy"
        viewModel.refreshState(context)

        composeTestRule.setContent {
            val density = LocalDensity.current
            val customDensity = Density(
                density = density.density,
                fontScale = 2.0f // Explicitly apply 200% font scale at 320dp x 480dp
            )
            CompositionLocalProvider(LocalDensity provides customDensity) {
                PhittoosTheme {
                    SettingsScreen(
                        viewModel = viewModel,
                        onNavigateBack = {},
                        onDataCleared = {}
                    )
                }
            }
        }

        // Verify profile name is displayed and can be read
        val nameNode = composeTestRule.onNodeWithText("Karthikeyan Venkatachalam Swamy")
        nameNode.assertIsDisplayed()

        // Verify the Edit button is displayed and interactive
        val editBtn = composeTestRule.onNodeWithTag("btn_edit_profile_name")
        editBtn.assertIsDisplayed()

        // Verify actual text layout bounds:
        // Since Name text and Edit button are aligned horizontally within a Row using weight(1f),
        // let's verify they do not overlap.
        val nameBounds = nameNode.getUnclippedBoundsInRoot()
        val editBtnBounds = editBtn.getUnclippedBoundsInRoot()

        assertTrue(
            "Name text right bound (${nameBounds.right}) must be less than or equal to Edit button left bound (${editBtnBounds.left})",
            nameBounds.right <= editBtnBounds.left
        )

        // Perform click to open dialog
        editBtn.performClick()

        // Verify Edit dialog opens successfully
        composeTestRule.onNodeWithTag("dialog_edit_name_input").assertIsDisplayed()
        
        // Verify Cancel dismisses the dialog without saving or deleting data
        composeTestRule.onNodeWithTag("dialog_edit_name_cancel").performClick()
        
        // Verify dialog is dismissed (input field no longer exists/displayed)
        composeTestRule.onNodeWithTag("dialog_edit_name_input").assertDoesNotExist()
        
        // Verify username is unchanged
        assertEquals("Karthikeyan Venkatachalam Swamy", userPreferences.userName)
    }

    @Test
    fun testDialogsAreReachableAndScrollableOnCompactViewports() {
        // Force username to a known state
        userPreferences.userName = "Original Name"
        viewModel.refreshState(context)

        composeTestRule.setContent {
            val density = LocalDensity.current
            val customDensity = Density(
                density = density.density,
                fontScale = 2.0f // Explicitly apply 200% font scale at 320dp x 480dp
            )
            CompositionLocalProvider(LocalDensity provides customDensity) {
                PhittoosTheme {
                    SettingsScreen(
                        viewModel = viewModel,
                        onNavigateBack = {},
                        onDataCleared = {}
                    )
                }
            }
        }

        // 1. Open and test Edit Name Dialog
        composeTestRule.onNodeWithTag("btn_edit_profile_name").performClick()
        
        // Verify input can be reached and has minimum accessibility touch bounds
        val inputNode = composeTestRule.onNodeWithTag("dialog_edit_name_input")
        inputNode.assertIsDisplayed()
        inputNode.assertWidthIsAtLeast(48.dp)
        inputNode.assertHeightIsAtLeast(48.dp)
        
        // Verify action buttons are reachable
        val cancelBtn = composeTestRule.onNodeWithTag("dialog_edit_name_cancel")
        val saveBtn = composeTestRule.onNodeWithTag("dialog_edit_name_save")
        cancelBtn.assertIsDisplayed()
        saveBtn.assertIsDisplayed()

        // Verify Cancel dismisses the dialog without saving
        cancelBtn.performClick()
        inputNode.assertDoesNotExist()
        assertEquals("Original Name", userPreferences.userName)

        // 2. Open and test Clear Data confirmation dialog
        composeTestRule.onNodeWithTag("btn_clear_all_data").performScrollTo().performClick()
        
        val confirmDialog = composeTestRule.onNodeWithTag("dialog_clear_data_confirm")
        confirmDialog.assertIsDisplayed()
        
        val cancelClearBtn = composeTestRule.onNodeWithTag("dialog_clear_data_cancel")
        cancelClearBtn.assertIsDisplayed()

        // Verify Cancel dismisses the dialog without deleting data
        cancelClearBtn.performClick()
        confirmDialog.assertDoesNotExist()
        // Verify that data/preferences were NOT cleared and userName still exists
        assertEquals("Original Name", userPreferences.userName)
    }
}
