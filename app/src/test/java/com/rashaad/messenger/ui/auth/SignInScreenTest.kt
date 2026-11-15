package com.rashaad.messenger.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rashaad.messenger.testing.FakeAuthRepository
import com.rashaad.messenger.ui.theme.MessengerTheme
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** UI test on the JVM (Robolectric): real ViewModel + fake repository + real Compose screen. */
@RunWith(AndroidJUnit4::class)
class SignInScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val auth = FakeAuthRepository()

    private fun setContent(viewModel: SignInViewModel = SignInViewModel(auth)) {
        composeRule.setContent {
            MessengerTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                SignInScreen(
                    uiState = state,
                    onEmailChange = viewModel::onEmailChange,
                    onPasswordChange = viewModel::onPasswordChange,
                    onSubmit = viewModel::onSubmit,
                    onToggleMode = viewModel::onToggleMode,
                    onGoogleClick = {},
                    onErrorShown = viewModel::onErrorShown,
                )
            }
        }
    }

    @Test
    fun submittingEmptyFormShowsInlineErrors() {
        setContent()
        composeRule.onNodeWithText("Sign in").performClick()

        composeRule.onNodeWithText("Enter a valid email address").assertExists()
        composeRule.onNodeWithText("Enter your password").assertExists()
    }

    @Test
    fun switchingToSignUpChangesTitleAndButton() {
        setContent()
        composeRule.onNodeWithText("New here? Create an account").performClick()

        composeRule.onNodeWithText("Create your account").assertExists()
        composeRule.onNodeWithText("Create account").assertExists()
    }

    @Test
    fun passwordVisibilityToggleHasAccessibleLabel() {
        setContent()
        composeRule.onNodeWithContentDescription("Show password").performClick()
        composeRule.onNodeWithContentDescription("Hide password").assertExists()
    }

    @Test
    fun validSubmitCallsBackendAndDisablesFormWhileLoading() {
        auth.gate = CompletableDeferred()
        setContent()

        composeRule.onNodeWithText("Email").performTextInput("ada@example.com")
        composeRule.onNodeWithText("Password").performTextInput("password123")
        composeRule.onNodeWithText("Sign in").performClick()

        composeRule.onNodeWithText("Continue with Google").assertIsNotEnabled()
        assertEquals(listOf("signIn:ada@example.com"), auth.calls)
    }

    @Test
    fun backendErrorAppearsInSnackbar() {
        auth.nextError = com.rashaad.messenger.domain.model.AuthError.NETWORK
        setContent()

        composeRule.onNodeWithText("Email").performTextInput("ada@example.com")
        composeRule.onNodeWithText("Password").performTextInput("password123")
        composeRule.onNodeWithText("Sign in").performClick()

        composeRule.onNodeWithText("No internet connection. Check your connection and try again.").assertExists()
    }
}
