package com.rashaad.messenger.ui.onboarding

import com.rashaad.messenger.domain.model.AuthUser
import com.rashaad.messenger.domain.model.ProfileError
import com.rashaad.messenger.domain.model.UserProfile
import com.rashaad.messenger.domain.validation.DisplayNameValidation
import com.rashaad.messenger.domain.validation.UsernameValidation
import com.rashaad.messenger.testing.FakeAuthRepository
import com.rashaad.messenger.testing.FakeUserRepository
import com.rashaad.messenger.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class ProfileSetupViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val auth = FakeAuthRepository(AuthUser("me", "me@example.com", suggestedDisplayName = "Ada Lovelace"))
    private val users = FakeUserRepository()
    private val viewModel = ProfileSetupViewModel(auth, users)

    @Test
    fun `display name is prefilled from the Google account`() {
        assertEquals("Ada Lovelace", viewModel.uiState.value.displayName)
    }

    @Test
    fun `username is lowercased and stripped of spaces while typing`() {
        viewModel.onUsernameChange("Ada Love")
        assertEquals("adalove", viewModel.uiState.value.username)
    }

    @Test
    fun `too short is only shown after submit, invalid characters immediately`() {
        viewModel.onUsernameChange("a")
        assertNull(viewModel.uiState.value.usernameError)

        viewModel.onUsernameChange("a-b")
        assertEquals(UsernameValidation.InvalidCharacters, viewModel.uiState.value.usernameError)

        viewModel.onUsernameChange("ab")
        viewModel.onSubmit()
        assertEquals(UsernameValidation.TooShort, viewModel.uiState.value.usernameError)
    }

    @Test
    fun `blank display name is rejected`() {
        viewModel.onUsernameChange("ada")
        viewModel.onDisplayNameChange("   ")
        viewModel.onSubmit()
        assertEquals(DisplayNameValidation.Empty, viewModel.uiState.value.displayNameError)
        assertNull(users.profile("me"))
    }

    @Test
    fun `valid input creates a normalised profile`() {
        viewModel.onUsernameChange("ada_99")
        viewModel.onDisplayNameChange("  Ada   Lovelace ")
        viewModel.onSubmit()
        assertEquals(UserProfile("me", "ada_99", "Ada Lovelace"), users.profile("me"))
    }

    @Test
    fun `taken username shows an error and allows retry`() {
        users.addProfile(UserProfile("someone-else", "ada", "Other Ada"))
        viewModel.onUsernameChange("ada")
        viewModel.onSubmit()

        assertEquals(ProfileError.USERNAME_TAKEN, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSaving)

        viewModel.onUsernameChange("ada2")
        assertNull(viewModel.uiState.value.error) // editing clears the stale error
        viewModel.onSubmit()
        assertEquals("ada2", users.profile("me")?.username)
    }

    @Test
    fun `offline claim reports a network error`() {
        users.nextError = ProfileError.NETWORK
        viewModel.onUsernameChange("ada")
        viewModel.onSubmit()
        assertEquals(ProfileError.NETWORK, viewModel.uiState.value.error)
    }
}
