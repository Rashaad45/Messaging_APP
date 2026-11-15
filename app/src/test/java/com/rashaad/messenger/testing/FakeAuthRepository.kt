package com.rashaad.messenger.testing

import com.rashaad.messenger.core.Outcome
import com.rashaad.messenger.data.auth.AuthRepository
import com.rashaad.messenger.domain.model.AuthError
import com.rashaad.messenger.domain.model.AuthUser
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory AuthRepository. A fake (real, simple behaviour) instead of a mock (scripted calls):
 * tests assert on *outcomes* rather than on which methods were called, so they survive refactors.
 */
class FakeAuthRepository(initialUser: AuthUser? = null) : AuthRepository {
    private val user = MutableStateFlow(initialUser)
    override val authUser: Flow<AuthUser?> = user

    /** Set to make the next auth call fail with this error. */
    var nextError: AuthError? = null

    /** Set to suspend auth calls until completed, to observe loading states. */
    var gate: CompletableDeferred<Unit>? = null

    val calls = mutableListOf<String>()

    override fun currentUser(): AuthUser? = user.value

    override suspend fun signInWithEmail(email: String, password: String) = authenticate("signIn:$email")

    override suspend fun signUpWithEmail(email: String, password: String) = authenticate("signUp:$email")

    override suspend fun signInWithGoogle(idToken: String) = authenticate("google:$idToken")

    override suspend fun signOut() {
        calls += "signOut"
        user.value = null
    }

    fun setUser(value: AuthUser?) {
        user.value = value
    }

    private suspend fun authenticate(call: String): Outcome<Unit, AuthError> {
        calls += call
        gate?.await()
        val error = nextError
        return if (error != null) {
            Outcome.Failure(error)
        } else {
            user.value = AuthUser(uid = "uid-1", email = call.substringAfter(':'), suggestedDisplayName = null)
            Outcome.Success(Unit)
        }
    }
}
