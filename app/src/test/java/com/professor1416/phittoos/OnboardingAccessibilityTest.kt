package com.professor1416.phittoos

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.professor1416.phittoos.data.db.AppDatabase
import com.professor1416.phittoos.data.preferences.UserPreferences
import com.professor1416.phittoos.ui.screens.onboarding.OnboardingScreen
import com.professor1416.phittoos.ui.theme.PhittoosTheme
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
@Config(sdk = [36], qualifiers = "w320dp-h480dp") // Simulate compact phone screen
class OnboardingAccessibilityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var db: AppDatabase
    private lateinit var userPreferences: UserPreferences
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        userPreferences = UserPreferences(context)
        userPreferences.clearAll()
    }

    @After
    fun tearDown() {
        userPreferences.clearAll()
        db.close()
    }

    @Test
    fun testGetStartedAndIntroClickBehavior() {
        var completed = false
        composeTestRule.setContent {
            PhittoosTheme {
                OnboardingScreen(
                    userPreferences = userPreferences,
                    onCompleteOnboarding = { completed = true }
                )
            }
        }

        // 1. Verify we start on Step 1 of 2
        val expectedProgressText1 = context.getString(
            R.string.onboarding_progress_announcement,
            1,
            2
        )
        // Verify Row is mergeDescendants=true with step progress content description
        composeTestRule.onNodeWithContentDescription(expectedProgressText1)
            .assertIsDisplayed()

        // 2. Verify "Get Started" button touch targets (48dp minimum check)
        val getStartedBtn = composeTestRule.onNodeWithTag("button_intro_get_started")
        getStartedBtn.assertIsDisplayed()
        getStartedBtn.assertWidthIsAtLeast(48.dp)
        getStartedBtn.assertHeightIsAtLeast(48.dp)

        // 3. Tapping the intro background or title text does NOT advance the step
        composeTestRule.onNodeWithText("Phittoos").performClick()
        // Still on step 1
        composeTestRule.onNodeWithContentDescription(expectedProgressText1)
            .assertIsDisplayed()

        // 4. Click the actual Get Started button to advance
        getStartedBtn.performClick()

        // 5. Verify we are now on Step 2 of 2
        val expectedProgressText2 = context.getString(
            R.string.onboarding_progress_announcement,
            2,
            2
        )
        composeTestRule.onNodeWithContentDescription(expectedProgressText2)
            .assertIsDisplayed()
        assertFalse(completed)
    }

    @Test
    fun testOnboardingStep2AccessibilityAndValidation() {
        var completed = false
        composeTestRule.setContent {
            PhittoosTheme {
                OnboardingScreen(
                    userPreferences = userPreferences,
                    onCompleteOnboarding = { completed = true }
                )
            }
        }

        // Move to Step 2
        composeTestRule.onNodeWithTag("button_intro_get_started").performClick()

        // Verify Step Row announcement is updated
        val expectedProgressText2 = context.getString(
            R.string.onboarding_progress_announcement,
            2,
            2
        )
        composeTestRule.onNodeWithContentDescription(expectedProgressText2)
            .assertIsDisplayed()

        // Verify Name field is displayed
        val nameField = composeTestRule.onNodeWithTag("input_user_name")
        nameField.assertIsDisplayed()

        // Verify Continue button exists and is disabled initially (name input empty)
        val continueBtn = composeTestRule.onNodeWithTag("button_name_continue")
        continueBtn.assertIsDisplayed()
        continueBtn.assertIsNotEnabled()

        // Touch target sizing check (Continue button must be at least 48dp)
        continueBtn.assertWidthIsAtLeast(48.dp)
        continueBtn.assertHeightIsAtLeast(48.dp)

        // Type a valid name
        nameField.performTextInput("Rahul")
        continueBtn.assertIsEnabled()

        // Clear name, Continue button must be disabled again
        nameField.performTextReplacement("")
        continueBtn.assertIsNotEnabled()
    }

    @Test
    fun testNameLabelRetainedAfterTyping() {
        composeTestRule.setContent {
            PhittoosTheme {
                OnboardingScreen(
                    userPreferences = userPreferences,
                    onCompleteOnboarding = {}
                )
            }
        }

        // Navigate to Step 2
        composeTestRule.onNodeWithTag("button_intro_get_started").performClick()

        // Before Typing: Label "Your name" should be displayed
        val labelText = context.getString(R.string.onboarding_name_label)
        composeTestRule.onNodeWithText(labelText).assertIsDisplayed()

        // Type username
        composeTestRule.onNodeWithTag("input_user_name").performTextInput("Swamy")

        // After Typing: Label "Your name" should STILL be displayed (as floating label)
        composeTestRule.onNodeWithText(labelText).assertIsDisplayed()
    }

    @Test
    fun testKeyboardDoneActionValidation() {
        var completed = false
        composeTestRule.setContent {
            PhittoosTheme {
                OnboardingScreen(
                    userPreferences = userPreferences,
                    onCompleteOnboarding = { completed = true }
                )
            }
        }

        // Navigate to Step 2
        composeTestRule.onNodeWithTag("button_intro_get_started").performClick()

        val nameField = composeTestRule.onNodeWithTag("input_user_name")

        // 1. With empty name, performing ImeAction.Done does NOT complete onboarding and displays error
        nameField.performImeAction()
        assertFalse(completed)

        // Verify the error semantics and supporting text displays "Please enter your name"
        composeTestRule.onNodeWithText("Please enter your name").assertIsDisplayed()
        nameField.assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, "Please enter your name"))

        // 2. Type a valid name and perform ImeAction.Done
        nameField.performTextInput("John Doe")
        nameField.performImeAction()

        // Verify onboarding is completed and preferences updated
        assertTrue(completed)
        assertEquals("John Doe", userPreferences.userName)
        assertTrue(userPreferences.hasCompletedOnboarding)
    }

    @Test
    fun testCompactViewportLayoutAt200PercentFontScale() {
        composeTestRule.setContent {
            val density = LocalDensity.current
            val customDensity = Density(
                density = density.density,
                fontScale = 2.0f // Explicitly apply 200% font scale at 320dp x 480dp
            )
            CompositionLocalProvider(LocalDensity provides customDensity) {
                PhittoosTheme {
                    OnboardingScreen(
                        userPreferences = userPreferences,
                        onCompleteOnboarding = {}
                    )
                }
            }
        }

        // Verify Step 1 is displayed
        val titleNode = composeTestRule.onNodeWithText("Phittoos")
        titleNode.assertIsDisplayed()

        val getStartedBtn = composeTestRule.onNodeWithTag("button_intro_get_started")
        getStartedBtn.assertIsDisplayed()

        // Verify they do not overlap
        val titleBounds = titleNode.getUnclippedBoundsInRoot()
        val btnBounds = getStartedBtn.getUnclippedBoundsInRoot()

        assertTrue(
            "Title bottom bound (${titleBounds.bottom}) must be less than or equal to Get Started button top bound (${btnBounds.top})",
            titleBounds.bottom <= btnBounds.top
        )
    }

    @Test
    fun testSkipFromStep1_completesOnboardingWithDefaultName() {
        var completed = false
        composeTestRule.setContent {
            PhittoosTheme {
                OnboardingScreen(
                    userPreferences = userPreferences,
                    onCompleteOnboarding = { completed = true }
                )
            }
        }

        // Verify Skip button on step 1 is present and clickable
        val skipBtn = composeTestRule.onNodeWithTag("button_intro_skip")
        skipBtn.assertIsDisplayed()
        skipBtn.performClick()

        assertTrue(completed)
        assertTrue(userPreferences.hasCompletedOnboarding)
        assertEquals("You", userPreferences.userName)
    }

    @Test
    fun testSkipFromStep2_completesOnboardingWithDefaultName() {
        var completed = false
        composeTestRule.setContent {
            PhittoosTheme {
                OnboardingScreen(
                    userPreferences = userPreferences,
                    onCompleteOnboarding = { completed = true }
                )
            }
        }

        // Advance to step 2
        composeTestRule.onNodeWithTag("button_intro_get_started").performClick()

        // Verify Skip button on step 2 is present and clickable
        val skipBtn = composeTestRule.onNodeWithTag("button_name_skip")
        skipBtn.assertIsDisplayed()
        skipBtn.performClick()

        assertTrue(completed)
        assertTrue(userPreferences.hasCompletedOnboarding)
        assertEquals("You", userPreferences.userName)
    }
}
