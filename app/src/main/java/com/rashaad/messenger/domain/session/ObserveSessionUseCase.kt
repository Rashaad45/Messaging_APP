package com.rashaad.messenger.domain.session

import com.rashaad.messenger.data.auth.AuthRepository
import com.rashaad.messenger.data.user.UserRepository
import com.rashaad.messenger.domain.model.AuthUser
import com.rashaad.messenger.domain.model.UserProfile
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/** Where the user is in the sign-in journey. The root UI shows one screen per state. */
sealed interface SessionState {
    data object Loading : SessionState
    data object SignedOut : SessionState

    /** Signed in to Firebase Auth but hasn't picked a username yet. */
    data class NeedsProfile(val user: AuthUser) : SessionState
    data class SignedIn(val profile: UserProfile) : SessionState
}

/**
 * Combines two repositories (auth + profile) into one session state. This is a use case
 * because the logic spans repositories and is needed by the app's root, not one screen.
 */
class ObserveSessionUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<SessionState> = authRepository.authUser
        // flatMapLatest: when the user signs out or switches account, the previous
        // profile listener is cancelled before the new one starts.
        .flatMapLatest { user ->
            if (user == null) {
                flowOf(SessionState.SignedOut)
            } else {
                userRepository.observeProfile(user.uid)
                    .map { profile ->
                        if (profile == null) SessionState.NeedsProfile(user) else SessionState.SignedIn(profile)
                    }
                    .onStart { emit(SessionState.Loading) }
            }
        }
        .distinctUntilChanged()
}
