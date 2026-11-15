package com.rashaad.messenger.data.auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.rashaad.messenger.domain.model.AuthError

/** Translates Firebase Auth exceptions into the app's own [AuthError] values. */
internal object FirebaseAuthErrorMapper {

    fun map(e: Throwable): AuthError = when (e) {
        // Order matters: WeakPassword is a subclass of InvalidCredentials.
        is FirebaseAuthWeakPasswordException -> AuthError.WEAK_PASSWORD
        is FirebaseAuthInvalidCredentialsException ->
            if (e.errorCode == "ERROR_INVALID_EMAIL") AuthError.INVALID_EMAIL else AuthError.INVALID_CREDENTIALS
        is FirebaseAuthInvalidUserException ->
            if (e.errorCode == "ERROR_USER_DISABLED") AuthError.USER_DISABLED else AuthError.INVALID_CREDENTIALS
        is FirebaseAuthUserCollisionException ->
            if (e.errorCode == "ERROR_EMAIL_ALREADY_IN_USE") {
                AuthError.EMAIL_ALREADY_IN_USE
            } else {
                AuthError.ACCOUNT_EXISTS_WITH_DIFFERENT_METHOD
            }
        is FirebaseTooManyRequestsException -> AuthError.TOO_MANY_REQUESTS
        is FirebaseNetworkException -> AuthError.NETWORK
        is FirebaseAuthException -> AuthError.UNKNOWN
        else -> AuthError.UNKNOWN
    }
}
