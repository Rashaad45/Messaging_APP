package com.rashaad.messenger.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Test

class UsernameValidatorTest {

    @Test
    fun `valid username is accepted and normalised`() {
        assertEquals(UsernameValidation.Valid("rashaad_45"), UsernameValidator.validate("  Rashaad_45 "))
    }

    @Test
    fun `boundary lengths 3 and 20 are accepted`() {
        assertEquals(UsernameValidation.Valid("abc"), UsernameValidator.validate("abc"))
        val twenty = "a".repeat(20)
        assertEquals(UsernameValidation.Valid(twenty), UsernameValidator.validate(twenty))
    }

    @Test
    fun `fewer than 3 characters is too short`() {
        assertEquals(UsernameValidation.TooShort, UsernameValidator.validate("ab"))
        assertEquals(UsernameValidation.TooShort, UsernameValidator.validate("   "))
    }

    @Test
    fun `more than 20 characters is too long`() {
        assertEquals(UsernameValidation.TooLong, UsernameValidator.validate("a".repeat(21)))
    }

    @Test
    fun `characters outside a-z 0-9 underscore are rejected`() {
        listOf("bad name", "dash-name", "dot.name", "émile", "emoji😀x").forEach {
            assertEquals(it, UsernameValidation.InvalidCharacters, UsernameValidator.validate(it))
        }
    }
}
