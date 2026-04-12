package jamgmilk.fuwagit

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SshCloneInstrumentedTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Test
    fun sshCloneCloneButtonBecomesEnabled() {
        val testUrl = "git@github.com:JamGmilk/FuwaGit.git"

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Add Repository")
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Repository URL")
            .performTextInput(testUrl)

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Credential")
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("SSH")
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Mirurin")
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Select")
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Destination")
            .performClick()

        Thread.sleep(2000)

        val documentsFolder = device.findObject(UiSelector().text("Documents"))
        documentsFolder.click()

        Thread.sleep(2000)

        val fuwaGitFolder = device.findObject(UiSelector().text("FuwaGit喵"))
        fuwaGitFolder.click()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Select")
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Clone Repository")
            .assertIsEnabled()
    }
}