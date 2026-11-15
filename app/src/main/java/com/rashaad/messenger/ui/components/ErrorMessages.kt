package com.rashaad.messenger.ui.components

import androidx.annotation.StringRes
import com.rashaad.messenger.R
import com.rashaad.messenger.domain.model.AuthError
import com.rashaad.messenger.domain.model.ProfileError

/** Maps domain errors to user-facing strings. Kept in the UI layer so ViewModels stay Android-free. */
@StringRes
fun AuthError.messageRes(): Int = when (this) {
    AuthError.INVALID_CREDENTIALS -> R.string.auth_error_invalid_credentials
    AuthError.EMAIL_ALREADY_IN_USE -> R.string.auth_error_email_in_use
    AuthError.WEAK_PASSWORD -> R.string.auth_error_weak_password
    AuthError.INVALID_EMAIL -> R.string.auth_error_invalid_email
    AuthError.USER_DISABLED -> R.string.auth_error_user_disabled
    AuthError.ACCOUNT_EXISTS_WITH_DIFFERENT_METHOD -> R.string.auth_error_different_method
    AuthError.TOO_MANY_REQUESTS -> R.string.auth_error_too_many_requests
    AuthError.NETWORK -> R.string.auth_error_network
    AuthError.GOOGLE_UNAVAILABLE -> R.string.auth_error_google_unavailable
    AuthError.UNKNOWN -> R.string.error_unknown
}

@StringRes
fun ProfileError.messageRes(): Int = when (this) {
    ProfileError.USERNAME_TAKEN -> R.string.profile_error_username_taken
    ProfileError.NETWORK -> R.string.profile_error_network
    ProfileError.PERMISSION_DENIED -> R.string.profile_error_permission
    ProfileError.UNKNOWN -> R.string.error_unknown
}
