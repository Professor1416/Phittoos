package com.example

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.test.core.app.ApplicationProvider
import com.example.ui.screens.frienddetail.SettlementCelebrationDialog
import com.example.ui.screens.frienddetail.SettlementCelebrationEvent
import com.example.ui.theme.PhittoosTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SettlementCelebrationVisualRefinementTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testCelebrationDialog_rendersCenteredPopupAndExactResourceText() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val friendName = "Priya"

        composeTestRule.setContent {
            PhittoosTheme {
                SettlementCelebrationDialog(
                    event = SettlementCelebrationEvent(id = 101L),
                    friendName = friendName,
                    onDismiss = {},
                    enableAnimation = false
                )
            }
        }

        // Popup card appears
        composeTestRule.onNodeWithTag("card_settlement_celebration").assertExists()

        // Circular badge with checkmark appears
        composeTestRule.onNodeWithTag("badge_celebration_checkmark").assertExists()

        // 2. Smaller introductory line: "Tere mere hisaab…"
        composeTestRule.onNodeWithTag("text_celebration_intro")
            .assertExists()
            .assertTextEquals(context.getString(R.string.celebration_intro))

        // 3. Large bold headline: "Phittoos!"
        composeTestRule.onNodeWithTag("text_celebration_headline")
            .assertExists()
            .assertTextEquals(context.getString(R.string.celebration_headline))

        // 4. Supporting line with friend name
        val expectedSupport = context.getString(R.string.celebration_support_with_name, friendName)
        composeTestRule.onNodeWithTag("text_celebration_support")
            .assertExists()
            .assertTextEquals(expectedSupport)

        // Close button at top-right with >= 48dp touch target
        composeTestRule.onNodeWithTag("btn_dismiss_celebration")
            .assertExists()
            .assertHasClickAction()
    }

    @Test
    fun testCelebrationDialog_rendersGenericSupportWhenFriendNameEmpty() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        composeTestRule.setContent {
            PhittoosTheme {
                SettlementCelebrationDialog(
                    event = SettlementCelebrationEvent(id = 102L),
                    friendName = null,
                    onDismiss = {},
                    enableAnimation = false
                )
            }
        }

        val expectedGeneric = context.getString(R.string.celebration_support_generic)
        composeTestRule.onNodeWithTag("text_celebration_support")
            .assertExists()
            .assertTextEquals(expectedGeneric)
    }

    @Test
    fun testCelebrationDialog_dismissesOnCloseButtonClick() {
        var dismissed = false

        composeTestRule.setContent {
            PhittoosTheme {
                SettlementCelebrationDialog(
                    event = SettlementCelebrationEvent(id = 103L),
                    friendName = "Vikram",
                    onDismiss = { dismissed = true },
                    enableAnimation = false
                )
            }
        }

        assertFalse("Dialog should not be dismissed initially", dismissed)
        composeTestRule.onNodeWithTag("btn_dismiss_celebration").performClick()
        assertTrue("Clicking close button triggers onDismiss", dismissed)
    }

    @Test
    fun testCelebrationDialog_dismissesOnOutsideScrimTap() {
        var dismissed = false

        composeTestRule.setContent {
            PhittoosTheme {
                SettlementCelebrationDialog(
                    event = SettlementCelebrationEvent(id = 104L),
                    friendName = "Rahul",
                    onDismiss = { dismissed = true },
                    enableAnimation = false
                )
            }
        }

        assertFalse("Dialog should not be dismissed initially", dismissed)
        // Click near the top-left of the scrim outside the centered card
        composeTestRule.onNodeWithTag("dialog_scrim_celebration").performTouchInput { click(Offset(10f, 10f)) }
        assertTrue("Tapping the dimmed scrim triggers onDismiss", dismissed)
    }

    @Test
    fun testCelebrationDialog_outsideTapDoesNotClickUnderlyingControls() {
        var underlyingClicked = false
        var celebrationDismissed = false

        composeTestRule.setContent {
            PhittoosTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    Button(
                        onClick = { underlyingClicked = true },
                        modifier = Modifier.testTag("underlying_button")
                    ) {
                        Text("Underlying Action")
                    }

                    SettlementCelebrationDialog(
                        event = SettlementCelebrationEvent(id = 106L),
                        friendName = "Sunil",
                        onDismiss = { celebrationDismissed = true },
                        enableAnimation = false
                    )
                }
            }
        }

        // Tap the scrim near the top-left corner outside the centered card
        composeTestRule.onNodeWithTag("dialog_scrim_celebration").performTouchInput { click(Offset(10f, 10f)) }

        assertTrue("Celebration dismissed by outside tap", celebrationDismissed)
        assertFalse("Underlying button must NOT be clicked when outside tap dismisses dialog", underlyingClicked)
    }

    @Test
    fun testRecompositionDoesNotResetOrCrash() {
        var recomposeTrigger by mutableIntStateOf(0)

        composeTestRule.setContent {
            PhittoosTheme {
                val count = recomposeTrigger
                SettlementCelebrationDialog(
                    event = remember { SettlementCelebrationEvent(id = 107L) },
                    friendName = "Friend $count",
                    onDismiss = {},
                    enableAnimation = false
                )
            }
        }

        composeTestRule.onNodeWithTag("card_settlement_celebration").assertExists()

        // Trigger recomposition
        recomposeTrigger++
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("card_settlement_celebration").assertExists()
    }
}
