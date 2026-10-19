package com.professor1416.phittoos.prototype

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h1000dp-xhdpi")
class SharedDebtPrototypeScreenTest {
    @get:Rule val compose = createAndroidComposeRule<SharedDebtPrototypeActivity>()

    private fun click(tag: String) {
        compose.onNodeWithTag(tag).performScrollTo().performClick()
    }

    private fun propose() { click("mode_SHARED"); click("propose_debt") }
    private fun acceptAsB() { click("actor_B"); click("accept_debt") }

    @Test fun modesAndPrivateDataAreExplicitlySeparate() {
        compose.onNodeWithTag("mode_PRIVATE").assertIsSelected()
        compose.onNodeWithText("Private amount to get back: ₹300").assertExists()
        click("actor_B")
        compose.onNodeWithText("No private demo record for Participant B.").assertExists()
        compose.onNodeWithText("Private amount to get back: ₹300").assertDoesNotExist()
        click("mode_SHARED")
        compose.onNodeWithTag("mode_SHARED").assertIsSelected()
        compose.onNodeWithTag("private_card").assertDoesNotExist()
        compose.onNodeWithText("Agreed outstanding: ₹0").assertExists()
    }

    @Test fun realUiKeepsProposalPendingUntilCounterpartyAccepts() {
        propose()
        compose.onNodeWithText("Status: PROPOSED").assertExists()
        compose.onNodeWithText("Agreed outstanding: ₹0").assertExists()
        compose.onNodeWithText("All settled", substring = true, ignoreCase = true).assertDoesNotExist()
        compose.onNodeWithTag("accept_debt").assertIsNotEnabled()
        acceptAsB()
        compose.onNodeWithText("Status: ACCEPTED").assertExists()
        compose.onNodeWithText("Agreed outstanding: ₹500").assertExists()
        compose.onNodeWithText("You owe: ₹500").assertExists()
        compose.onNodeWithTag("accept_debt").assertDoesNotExist()
        compose.onNodeWithTag("event_demo-debt-1-proposed").assertExists()
        compose.onNodeWithTag("event_demo-debt-1-accepted").assertExists()
        click("actor_A")
        compose.onNodeWithText("You’ll get back: ₹500").assertExists()
        compose.onNodeWithText("You owe: ₹500").assertDoesNotExist()
    }

    @Test fun resetAndScrollingKeepDisclosureVisible() {
        propose()
        acceptAsB()
        compose.onNodeWithTag("reset_demo").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("demo_banner").assertIsDisplayed()
        compose.onNodeWithTag("reset_demo").performClick()
        compose.onNodeWithTag("mode_PRIVATE").assertIsSelected()
        compose.onNodeWithText("Private amount to get back: ₹300").assertExists()
        click("mode_SHARED")
        compose.onNodeWithText("Status: DRAFT").assertExists()
        compose.onNodeWithText("No shared events yet.").assertExists()
    }

    @Test fun rotationRetainsAgreementInActualActivityViewModel() {
        propose()
        acceptAsB()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Status: ACCEPTED").assertExists()
        compose.onNodeWithText("You owe: ₹500").assertExists()
        compose.onAllNodesWithTag("event_demo-debt-1-accepted").assertCountEquals(1)
    }

    @Test @Config(qualifiers = "w411dp-h1000dp-night-xhdpi")
    fun darkThemeShowsControlsAndFinancialDirection() {
        propose()
        acceptAsB()
        compose.onNodeWithText("You owe: ₹500").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("demo_banner").assertIsDisplayed()
    }

    @Test @Config(qualifiers = "w360dp-h1000dp-xhdpi", fontScale = 2f)
    fun largeTextRemainsScrollableWithLabelledTouchTargets() {
        click("mode_SHARED")
        compose.onNodeWithTag("propose_debt").performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(androidx.compose.ui.unit.Dp(48f))
        compose.onNodeWithTag("demo_banner").assertIsDisplayed()
        compose.onNodeWithTag("propose_debt").performClick()
        click("actor_B")
        compose.onNodeWithTag("accept_debt").performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(androidx.compose.ui.unit.Dp(48f))
    }

    @Test fun installedVariantMetadataAndRuntimeHaveNoV1StartupOrStorage() {
        val context = compose.activity
        assertEquals("com.professor1416.phittoos.v2prototype", context.packageName)
        assertEquals(Application::class.java, context.application.javaClass)
        assertTrue(context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0)
        val pm = context.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(context.packageName)
        @Suppress("DEPRECATION")
        val activities = pm.queryIntentActivities(launcher, 0)
        assertEquals(listOf(SharedDebtPrototypeActivity::class.java.name), activities.map { it.activityInfo.name })
        @Suppress("DEPRECATION")
        assertThrows(PackageManager.NameNotFoundException::class.java) {
            pm.getActivityInfo(ComponentName(context.packageName, "com.professor1416.phittoos.MainActivity"), 0)
        }
        @Suppress("DEPRECATION")
        val packageInfo = pm.getPackageInfo(context.packageName, PackageManager.GET_PROVIDERS)
        assertFalse(packageInfo.providers.orEmpty().any { it.name == "androidx.startup.InitializationProvider" })
        // Exercise both mock paths before checking the application sandbox.
        propose()
        acceptAsB()
        click("reset_demo")
        assertTrue("Prototype must not open any Room/WorkManager database", context.databaseList().isEmpty())
        val preferenceFiles = File(context.applicationInfo.dataDir, "shared_prefs").listFiles().orEmpty()
        assertFalse(preferenceFiles.any { it.name.startsWith("phittoos_prefs") })
    }
}
