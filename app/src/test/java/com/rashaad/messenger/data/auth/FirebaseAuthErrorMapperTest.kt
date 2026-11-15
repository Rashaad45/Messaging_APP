package com.rashaad.messenger.data.auth

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.rashaad.messenger.domain.model.AuthError
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Robolectric: Firebase exception constructors call Android framework code (TextUtils). */
@RunWith(AndroidJUnit4::class)
class FirebaseAuthErrorMapperTest {

    @Test
    fun `weak password wins over its parent invalid-credentials type`() {
        assertEquals(
            AuthError.WEAK_PASSWORD,
            FirebaseAuthErrorMapper.map(FirebaseAuthWeakPasswordException("ERROR_WEAK_PASSWORD", "weak", "too short")),
        )
    }

    @Test
    fun `invalid credential codes`() {
        assertEquals(
            AuthError.INVALID_EMAIL,
            FirebaseAuthErrorMapper.map(FirebaseAuthInvalidCredentialsException("ERROR_INVALID_EMAIL", "bad")),
        )
        assertEquals(
            AuthError.INVALID_CREDENTIALS,
            FirebaseAuthErrorMapper.map(FirebaseAuthInvalidCredentialsException("ERROR_INVALID_CREDENTIAL", "bad")),
        )
    }

    @Test
    fun `user and collision codes`() {
        assertEquals(
            AuthError.USER_DISABLED,
            FirebaseAuthErrorMapper.map(FirebaseAuthInvalidUserException("ERROR_USER_DISABLED", "off")),
        )
        assertEquals(
            AuthError.EMAIL_ALREADY_IN_USE,
            FirebaseAuthErrorMapper.map(FirebaseAuthUserCollisionException("ERROR_EMAIL_ALREADY_IN_USE", "dup")),
        )
        assertEquals(
            AuthError.ACCOUNT_EXISTS_WITH_DIFFERENT_METHOD,
            FirebaseAuthErrorMapper.map(
                FirebaseAuthUserCollisionException("ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL", "dup"),
            ),
        )
    }

    @Test
    fun `network and unknown`() {
        assertEquals(AuthError.NETWORK, FirebaseAuthErrorMapper.map(FirebaseNetworkException("offline")))
        assertEquals(AuthError.UNKNOWN, FirebaseAuthErrorMapper.map(IllegalStateException()))
    }
}
