package com.rashaad.messenger.ui.auth

import app.cash.turbine.test
import com.rashaad.messenger.domain.model.AuthError
import com.rashaad.messenger.testing.FakeAuthRepository
import com.rashaad.messenger.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SignInViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private val viewModel = SignInViewModel(auth)

    @Test
    fun `invalid email and empty password show field errors without calling the backend`() {
        viewModel.onEmailChange("not-an-email")
        viewModel.onSubmit()

        val state = viewModel.uiState.value
        assertEquals(EmailFieldError.INVALID, state.emailError)
        assertEquals(PasswordFieldError.EMPTY, state.passwordError)
        assertTrue(auth.calls.isEmpty())
    }

    @Test
    fun `short password is rejected only when signing up`() {
        viewModel.onEmailChange("ada@example.com")
        viewModel.onPasswordChange("short")

        viewModel.onToggleMode() // -> SIGN_UP
        viewModel.onSubmit()
        assertEquals(PasswordFieldError.TOO_SHORT, viewModel.uiState.value.passwordError)

        viewModel.onToggleMode() // -> SIGN_IN: existing accounts may have older, shorter passwords
        viewModel.onSubmit()
        assertEquals(listOf("signIn:ada@example.com"), auth.calls)
    }

    @Test
    fun `typing clears that field's error`() {
        viewModel.onSubmit()
        viewModel.onEmailChange("a")
        assertNull(viewModel.uiState.value.emailError)
        assertEquals(PasswordFieldError.EMPTY, viewModel.uiState.value.passwordError)
    }

    @Test
    fun `successful sign up shows loading then resets the form`() = runTest {
        val gate = CompletableDeferred<Unit>().also { auth.gate = it }
        viewModel.onToggleMode()
        viewModel.onEmailChange("ada@example.com")
        viewModel.onPasswordChange("correct horse")

        viewModel.uiState.test {
            assertFalse(awaitItem().isLoading)
            viewModel.onSubmit()
            assertTrue(awaitItem().isLoading)
            gate.complete(Unit)
            val done = awaitItem()
            assertFalse(done.isLoading)
            assertEquals("", done.password) // never keep the password around after use
            assertEquals(AuthMode.SIGN_UP, done.mode)
        }
        assertEquals(listOf("signUp:ada@example.com"), auth.calls)
    }

    @Test
    fun `backend error is exposed once and can be dismissed`() {
        auth.nextError = AuthError.INVALID_CREDENTIALS
        viewModel.onEmailChange("ada@example.com")
        viewModel.onPasswordChange("wrong-password")

        viewModel.onSubmit()

        assertEquals(AuthError.INVALID_CREDENTIALS, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("wrong-password", viewModel.uiState.value.password) // kept so the user can fix a typo
        viewModel.onErrorShown()
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `submitting twice while loading only calls the backend once`() {
        auth.gate = CompletableDeferred()
        viewModel.onEmailChange("ada@example.com")
        viewModel.onPasswordChange("password123")

        viewModel.onSubmit()
        viewModel.onSubmit()

        assertEquals(1, auth.calls.size)
    }

    @Test
    fun `google token is exchanged for a session`() {
        viewModel.onGoogleIdToken("token-abc")
        assertEquals(listOf("google:token-abc"), auth.calls)
    }

    @Test
    fun `google unavailable shows an error`() {
        viewModel.onGoogleSignInUnavailable()
        assertEquals(AuthError.GOOGLE_UNAVAILABLE, viewModel.uiState.value.error)
    }
}
