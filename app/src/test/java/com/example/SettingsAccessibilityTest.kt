package com.example

import android.content.Context
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
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
import com.example.data.preferences.UserPreferences
import com.example.data.repository.PhittoosRepository
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.theme.PhittoosTheme
import com.example.ui.viewmodel.SettingsViewModel
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
        reminderRow.assertIsDisplayed()

        // Verify minimum 48dp touch target
        reminderRow.assertWidthIsAtLeast(48.dp)
        reminderRow.assertHeightIsAtLeast(48.dp)

        // Verify the semantic properties of the merged Row (Role.Switch)
        reminderRow.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))

        // 2. Click the Row and verify preference toggles exactly once
        assertTrue(userPreferences.remindersEnabled)
        assertTrue(viewModel.uiState.value.remindersEnabled)

        reminderRow.performClick()
        assertFalse(userPreferences.remindersEnabled)
        assertFalse(viewModel.uiState.value.remindersEnabled)

        reminderRow.performClick()
        assertTrue(userPreferences.remindersEnabled)
        assertTrue(viewModel.uiState.value.remindersEnabled)
    }

    @Test
    fun testLongProfileNameWrapsAndEditActionIsInteractive() {
        // Set a long supported profile name
        userPreferences.userName = "Karthikeyan Venkatachalam Swamy"
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

        // Verify profile name is displayed and can be read
        composeTestRule.onNodeWithText("Karthikeyan Venkatachalam Swamy").assertIsDisplayed()

        // Verify the Edit button is displayed and interactive
        val editBtn = composeTestRule.onNodeWithTag("btn_edit_profile_name")
        editBtn.assertIsDisplayed()
        editBtn.performClick()

        // Verify Edit dialog opens successfully
        composeTestRule.onNodeWithTag("dialog_edit_name_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dialog_edit_name_cancel").performClick()
    }

    @Test
    fun testDialogsAreReachableAndScrollableOnCompactViewports() {
        composeTestRule.setContent {
            PhittoosTheme {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = {},
                    onDataCleared = {}
                )
            }
        }

        // 1. Edit name dialog
        composeTestRule.onNodeWithTag("btn_edit_profile_name").performClick()
        composeTestRule.onNodeWithTag("dialog_edit_name_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dialog_edit_name_save").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dialog_edit_name_cancel").performClick()

        // 2. Clear data confirmation dialog
        composeTestRule.onNodeWithTag("btn_clear_all_data").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("dialog_clear_data_confirm").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dialog_clear_data_cancel").performClick()
    }
}
