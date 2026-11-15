package com.rashaad.messenger.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rashaad.messenger.core.Outcome
import com.rashaad.messenger.data.auth.AuthRepository
import com.rashaad.messenger.data.user.UserRepository
import com.rashaad.messenger.domain.model.ProfileError
import com.rashaad.messenger.domain.validation.DisplayNameValidation
import com.rashaad.messenger.domain.validation.DisplayNameValidator
import com.rashaad.messenger.domain.validation.UsernameValidation
import com.rashaad.messenger.domain.validation.UsernameValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileSetupUiState(
    val username: String = "",
    val displayName: String = "",
    /** Null when valid or not yet worth showing. */
    val usernameError: UsernameValidation? = null,
    val displayNameError: DisplayNameValidation? = null,
    val isSaving: Boolean = false,
    val error: ProfileError? = null,
)

@HiltViewModel
class ProfileSetupViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ProfileSetupUiState(displayName = authRepository.currentUser()?.suggestedDisplayName.orEmpty()),
    )
    val uiState: StateFlow<ProfileSetupUiState> = _uiState.asStateFlow()

    private var submitAttempted = false

    fun onUsernameChange(value: String) {
        // Usernames are lowercase; normalise while typing so the user sees what will be saved.
        val username = value.lowercase().filterNot(Char::isWhitespace)
        _uiState.update { it.copy(username = username, usernameError = usernameErrorFor(username), error = null) }
    }

    fun onDisplayNameChange(value: String) = _uiState.update {
        it.copy(
            displayName = value,
            displayNameError = if (submitAttempted) displayNameErrorFor(value) else null,
        )
    }

    fun onSubmit() {
        val state = _uiState.value
        if (state.isSaving) return
        submitAttempted = true

        val username = UsernameValidator.validate(state.username)
        val displayName = DisplayNameValidator.validate(state.displayName)
        val uid = authRepository.currentUser()?.uid

        when {
            username !is UsernameValidation.Valid || displayName !is DisplayNameValidation.Valid ->
                _uiState.update {
                    it.copy(
                        usernameError = usernameErrorFor(it.username),
                        displayNameError = displayNameErrorFor(it.displayName),
                    )
                }
            // Signed out in the meantime; the root will show sign-in again.
            uid == null -> _uiState.update { it.copy(error = ProfileError.UNKNOWN) }
            else -> save(uid, username.username, displayName.displayName)
        }
    }

    private fun save(uid: String, username: String, displayName: String) {
        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            val result = userRepository.createProfile(uid, username, displayName)
            // Success needs no navigation: the profile listener fires and the root shows Home.
            if (result is Outcome.Failure) {
                _uiState.update { it.copy(isSaving = false, error = result.error) }
            }
        }
    }

    fun onErrorShown() = _uiState.update { it.copy(error = null) }

    /**
     * Show "too long"/"invalid characters" immediately while typing; only show "too short"
     * after a submit attempt, so the field isn't red after the first keystroke.
     */
    private fun usernameErrorFor(username: String): UsernameValidation? =
        when (val result = UsernameValidator.validate(username)) {
            is UsernameValidation.Valid -> null
            UsernameValidation.TooShort -> if (submitAttempted) result else null
            else -> result
        }

    private fun displayNameErrorFor(name: String): DisplayNameValidation? =
        DisplayNameValidator.validate(name).takeUnless { it is DisplayNameValidation.Valid }
}
