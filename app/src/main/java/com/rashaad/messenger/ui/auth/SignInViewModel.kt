package com.rashaad.messenger.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rashaad.messenger.core.Outcome
import com.rashaad.messenger.data.auth.AuthRepository
import com.rashaad.messenger.domain.model.AuthError
import com.rashaad.messenger.domain.validation.CredentialsValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthMode { SIGN_IN, SIGN_UP }

enum class EmailFieldError { INVALID }

enum class PasswordFieldError { EMPTY, TOO_SHORT }

/** Everything the sign-in screen needs to draw itself. Immutable: every change is a copy. */
data class SignInUiState(
    val mode: AuthMode = AuthMode.SIGN_IN,
    val email: String = "",
    val password: String = "",
    val emailError: EmailFieldError? = null,
    val passwordError: PasswordFieldError? = null,
    val isLoading: Boolean = false,
    /** A one-off error to show in a snackbar; cleared via [SignInViewModel.onErrorShown]. */
    val error: AuthError? = null,
)

@HiltViewModel
class SignInViewModel @Inject constructor(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, emailError = null) }

    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, passwordError = null) }

    fun onToggleMode() = _uiState.update {
        it.copy(
            mode = if (it.mode == AuthMode.SIGN_IN) AuthMode.SIGN_UP else AuthMode.SIGN_IN,
            emailError = null,
            passwordError = null,
            error = null,
        )
    }

    fun onSubmit() {
        val state = _uiState.value
        if (state.isLoading) return

        val emailError = if (CredentialsValidator.isEmailValid(state.email)) null else EmailFieldError.INVALID
        val passwordError = when {
            state.password.isEmpty() -> PasswordFieldError.EMPTY
            state.mode == AuthMode.SIGN_UP && !CredentialsValidator.isPasswordStrongEnough(state.password) ->
                PasswordFieldError.TOO_SHORT
            else -> null
        }
        if (emailError != null || passwordError != null) {
            _uiState.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }

        runAuth {
            when (state.mode) {
                AuthMode.SIGN_IN -> authRepository.signInWithEmail(state.email, state.password)
                AuthMode.SIGN_UP -> authRepository.signUpWithEmail(state.email, state.password)
            }
        }
    }

    /** Called by the screen after Credential Manager returned a Google ID token. */
    fun onGoogleIdToken(idToken: String) = runAuth { authRepository.signInWithGoogle(idToken) }

    /** Called by the screen when Credential Manager failed (not when the user cancelled). */
    fun onGoogleSignInUnavailable() = _uiState.update { it.copy(error = AuthError.GOOGLE_UNAVAILABLE) }

    fun onErrorShown() = _uiState.update { it.copy(error = null) }

    private fun runAuth(call: suspend () -> Outcome<Unit, AuthError>) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = call()
            // On success there's nothing to navigate: the session state changes and the
            // app root swaps this screen out. We still reset so the form is clean after sign-out.
            _uiState.update {
                when (result) {
                    is Outcome.Success -> SignInUiState(mode = it.mode)
                    is Outcome.Failure -> it.copy(isLoading = false, error = result.error)
                }
            }
        }
    }
}
