package com.rashaad.messenger.domain.model

/**
 * Expected authentication failures. The data layer maps Firebase exceptions to these,
 * so the UI never depends on Firebase types and can show a specific message for each.
 */
enum class AuthError {
    /** Wrong email/password, or no such account (Firebase merges these to prevent account enumeration). */
    INVALID_CREDENTIALS,
    EMAIL_ALREADY_IN_USE,
    WEAK_PASSWORD,
    INVALID_EMAIL,
    USER_DISABLED,

    /** Email already registered with another sign-in method (e.g. password vs Google). */
    ACCOUNT_EXISTS_WITH_DIFFERENT_METHOD,
    TOO_MANY_REQUESTS,
    NETWORK,

    /** Google sign-in couldn't run: no Google account on the device, or Play services problem. */
    GOOGLE_UNAVAILABLE,
    UNKNOWN,
}

/** Expected failures when creating the user's profile and claiming a username. */
enum class ProfileError {
    USERNAME_TAKEN,
    NETWORK,
    PERMISSION_DENIED,
    UNKNOWN,
}
