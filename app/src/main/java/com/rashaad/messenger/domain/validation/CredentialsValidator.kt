package com.rashaad.messenger.domain.validation

/**
 * Quick client-side checks so obvious mistakes are shown instantly without a network round trip.
 * Firebase Auth remains the authority (e.g. it fully validates email syntax).
 */
object CredentialsValidator {
    /** Firebase's own minimum is 6; we ask for 8 on sign-up. */
    const val MIN_PASSWORD_LENGTH = 8
    private val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun isEmailValid(email: String): Boolean = EMAIL.matches(email.trim())

    fun isPasswordStrongEnough(password: String): Boolean = password.length >= MIN_PASSWORD_LENGTH
}
