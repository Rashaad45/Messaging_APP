package com.rashaad.messenger.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Test

class DisplayNameValidatorTest {

    @Test
    fun `whitespace is trimmed and collapsed`() {
        assertEquals(DisplayNameValidation.Valid("Ada Lovelace"), DisplayNameValidator.validate("  Ada \n  Lovelace "))
    }

    @Test
    fun `blank is empty`() {
        assertEquals(DisplayNameValidation.Empty, DisplayNameValidator.validate("   "))
    }

    @Test
    fun `50 characters is allowed, 51 is too long`() {
        assertEquals(DisplayNameValidation.Valid("a".repeat(50)), DisplayNameValidator.validate("a".repeat(50)))
        assertEquals(DisplayNameValidation.TooLong, DisplayNameValidator.validate("a".repeat(51)))
    }

    @Test
    fun `emoji count as one character each`() {
        val fiftyEmoji = "😀".repeat(50) // 100 UTF-16 chars, 50 code points
        assertEquals(DisplayNameValidation.Valid(fiftyEmoji), DisplayNameValidator.validate(fiftyEmoji))
    }
}
