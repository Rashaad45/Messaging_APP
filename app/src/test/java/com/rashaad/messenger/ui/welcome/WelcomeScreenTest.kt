package com.rashaad.messenger.ui.welcome

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rashaad.messenger.ui.theme.MessengerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs on the JVM via Robolectric, so it executes in CI without an emulator.
 */
@RunWith(AndroidJUnit4::class)
class WelcomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsAppNameAsHeading() {
        composeRule.setContent { MessengerTheme { WelcomeScreen() } }

        composeRule.onNodeWithText("Messenger")
            .assertIsDisplayed()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }
}
