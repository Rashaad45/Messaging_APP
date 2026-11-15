package com.rashaad.messenger.data.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.ClearCredentialException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.rashaad.messenger.core.Outcome
import com.rashaad.messenger.domain.model.AuthError
import com.rashaad.messenger.domain.model.AuthUser
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await

@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    @param:ApplicationContext private val context: Context,
) : AuthRepository {

    // callbackFlow adapts Firebase's listener API to a Flow; awaitClose removes the
    // listener when the collector stops, so nothing leaks.
    override val authUser: Flow<AuthUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.toAuthUser()) }
        auth.addAuthStateListener(listener) // also fires once immediately with the current state
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    override fun currentUser(): AuthUser? = auth.currentUser?.toAuthUser()

    override suspend fun signInWithEmail(email: String, password: String) = runAuth {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
    }

    override suspend fun signUpWithEmail(email: String, password: String) = runAuth {
        auth.createUserWithEmailAndPassword(email.trim(), password).await()
    }

    override suspend fun signInWithGoogle(idToken: String) = runAuth {
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
    }

    override suspend fun signOut() {
        auth.signOut()
        // Forget the chosen Google account so the account picker appears on next sign-in.
        try {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        } catch (e: ClearCredentialException) {
            // Not fatal: the Firebase session is already gone.
            android.util.Log.w(TAG, "Couldn't clear credential state", e)
        }
    }

    // Firebase reports failures through several unrelated exception types, so we catch
    // broadly here and let FirebaseAuthErrorMapper sort them into AuthError values.
    @Suppress("TooGenericExceptionCaught")
    private suspend fun runAuth(block: suspend () -> Unit): Outcome<Unit, AuthError> = try {
        block()
        Outcome.Success(Unit)
    } catch (e: CancellationException) {
        throw e // never swallow coroutine cancellation
    } catch (e: Exception) {
        Outcome.Failure(FirebaseAuthErrorMapper.map(e))
    }

    private fun FirebaseUser.toAuthUser() = AuthUser(
        uid = uid,
        email = email,
        suggestedDisplayName = displayName?.takeIf { it.isNotBlank() },
    )

    private companion object {
        const val TAG = "FirebaseAuthRepository"
    }
}
