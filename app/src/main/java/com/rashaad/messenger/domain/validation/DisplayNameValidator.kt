package com.rashaad.messenger.domain.validation

/** Display names are free text (any language, emoji), 1–50 characters after trimming. */
object DisplayNameValidator {
    const val MAX_LENGTH = 50

    fun normalize(input: String): String = input.trim().replace(Regex("\\s+"), " ")

    fun validate(input: String): DisplayNameValidation {
        val name = normalize(input)
        return when {
            name.isEmpty() -> DisplayNameValidation.Empty
            // codePointCount so an emoji counts as one character, matching the server rule's intent.
            name.codePointCount(0, name.length) > MAX_LENGTH -> DisplayNameValidation.TooLong
            else -> DisplayNameValidation.Valid(name)
        }
    }
}

sealed interface DisplayNameValidation {
    data class Valid(val displayName: String) : DisplayNameValidation
    data object Empty : DisplayNameValidation
    data object TooLong : DisplayNameValidation
}
