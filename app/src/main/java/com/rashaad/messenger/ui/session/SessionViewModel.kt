package com.rashaad.messenger.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rashaad.messenger.data.auth.AuthRepository
import com.rashaad.messenger.domain.session.ObserveSessionUseCase
import com.rashaad.messenger.domain.session.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SessionViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    private val authRepository: AuthRepository,
) : ViewModel() {

    // WhileSubscribed(5s): keeps the Firestore listener alive across configuration changes
    // (rotation takes < 5s) but stops it when the app has been in the background a while.
    val state: StateFlow<SessionState> = observeSession()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SessionState.Loading)

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
