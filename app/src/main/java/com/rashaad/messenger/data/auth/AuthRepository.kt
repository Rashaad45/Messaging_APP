package com.rashaad.messenger.data.auth

import com.rashaad.messenger.core.Outcome
import com.rashaad.messenger.domain.model.AuthError
import com.rashaad.messenger.domain.model.AuthUser
import kotlinx.coroutines.flow.Flow

/**
 * Authentication boundary. ViewModels depend on this interface; production uses
 * [FirebaseAuthRepository] and tests use a fake.
 */
interface AuthRepository {
    /** Emits the signed-in user, or null when signed out. Emits immediately on collection. */
    val authUser: Flow<AuthUser?>

    /** Synchronous snapshot of the signed-in user, or null. */
    fun currentUser(): AuthUser?

    suspend fun signInWithEmail(email: String, password: String): Outcome<Unit, AuthError>

    suspend fun signUpWithEmail(email: String, password: String): Outcome<Unit, AuthError>

    /** Exchanges a Google ID token (from Credential Manager) for a Firebase session. */
    suspend fun signInWithGoogle(idToken: String): Outcome<Unit, AuthError>

    suspend fun signOut()
}
