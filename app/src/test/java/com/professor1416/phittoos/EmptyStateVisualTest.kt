package com.professor1416.phittoos

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.professor1416.phittoos.ui.screens.home.EmptyFriendsState
import com.professor1416.phittoos.ui.theme.PhittoosTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w400dp-h1000dp-xhdpi")
class EmptyStateVisualTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun zeroState_displaysIllustrationTextGuideStepsAndCTAs() {
        var addTransactionClicked = false
        var addFriendClicked = false

        composeTestRule.setContent {
            PhittoosTheme {
                EmptyFriendsState(
                    isSearching = false,
                    onAddFriendClick = { addFriendClicked = true },
                    onAddTransactionClick = { addTransactionClicked = true }
                )
            }
        }

        // 1. Verify Empty State container is visible
        composeTestRule.onNodeWithTag("empty_friends_state").assertIsDisplayed()

        // 2. Verify welcoming copy and guidance steps
        composeTestRule.onNodeWithText("Ready to start tracking?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Add Friends").assertIsDisplayed()
        composeTestRule.onNodeWithText("Log Amounts").assertIsDisplayed()
        composeTestRule.onNodeWithText("Settle & Celebrate").assertIsDisplayed()

        // 3. Verify primary action CTA (Record First Entry)
        composeTestRule.onNodeWithTag("button_empty_add_transaction").assertIsDisplayed()
        composeTestRule.onNodeWithTag("button_empty_add_transaction").performClick()
        assertTrue(addTransactionClicked)

        // 4. Verify secondary action CTA (Quick Add Friend)
        composeTestRule.onNodeWithTag("button_empty_add_friend").assertIsDisplayed()
        composeTestRule.onNodeWithTag("button_empty_add_friend").performClick()
        assertTrue(addFriendClicked)
    }

    @Test
    fun searchEmptyState_displaysSearchOffIllustrationAndClearButton() {
        var clearSearchClicked = false
        var addSearchedFriendClicked = false

        composeTestRule.setContent {
            PhittoosTheme {
                EmptyFriendsState(
                    isSearching = true,
                    searchQuery = "Karthik",
                    onAddFriendClick = {},
                    onAddTransactionClick = {},
                    onClearSearch = { clearSearchClicked = true },
                    onAddSearchedFriend = { addSearchedFriendClicked = true }
                )
            }
        }

        // Verify Search Empty State
        composeTestRule.onNodeWithTag("empty_friends_state").assertIsDisplayed()
        composeTestRule.onNodeWithText("No friends found").assertIsDisplayed()
        composeTestRule.onNodeWithText("No friends matched \"Karthik\". Check spelling or add them below.").assertIsDisplayed()

        // Click Add Searched Friend
        composeTestRule.onNodeWithTag("button_empty_add_friend").performClick()
        assertTrue(addSearchedFriendClicked)

        // Click clear
        composeTestRule.onNodeWithTag("button_empty_clear_search").performClick()
        assertTrue(clearSearchClicked)
    }
}
