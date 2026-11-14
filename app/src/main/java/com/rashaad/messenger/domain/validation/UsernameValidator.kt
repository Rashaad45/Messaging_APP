package com.rashaad.messenger.domain.validation

/**
 * Validation rules for usernames, shared by the onboarding screen and the repository.
 * The same rules are enforced server-side in firestore.rules so a modified client
 * cannot bypass them.
 */
object UsernameValidator {
    const val MIN_LENGTH = 3
    const val MAX_LENGTH = 20
    private val ALLOWED = Regex("^[a-z0-9_]+$")

    /** Usernames are case-insensitive: we store and compare them lowercase and trimmed. */
    fun normalize(input: String): String = input.trim().lowercase()

    fun validate(input: String): UsernameValidation {
        val username = normalize(input)
        return when {
            username.length < MIN_LENGTH -> UsernameValidation.TooShort
            username.length > MAX_LENGTH -> UsernameValidation.TooLong
            !ALLOWED.matches(username) -> UsernameValidation.InvalidCharacters
            else -> UsernameValidation.Valid(username)
        }
    }
}

sealed interface UsernameValidation {
    data class Valid(val username: String) : UsernameValidation
    data object TooShort : UsernameValidation
    data object TooLong : UsernameValidation
    data object InvalidCharacters : UsernameValidation
}
