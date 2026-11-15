package com.rashaad.messenger.testing

import com.rashaad.messenger.core.Outcome
import com.rashaad.messenger.data.user.UserRepository
import com.rashaad.messenger.domain.model.ProfileError
import com.rashaad.messenger.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

/** In-memory UserRepository that enforces username uniqueness like the real backend. */
class FakeUserRepository : UserRepository {
    private val profiles = MutableStateFlow<Map<String, UserProfile>>(emptyMap())

    /** uids the "server" has answered for; until then observeProfile emits nothing (like a cold cache). */
    private val loaded = MutableStateFlow<Set<String>>(emptySet())

    /** Force the next createProfile call to fail with this error (e.g. NETWORK). */
    var nextError: ProfileError? = null

    override fun observeProfile(uid: String): Flow<UserProfile?> = combine(profiles, loaded) { all, loadedUids ->
        // Wrap so "loaded, no profile" (Some(null)) differs from "not loaded yet" (null).
        if (uid in loadedUids) Some(all[uid]) else null
    }
        .filterNotNull()
        .map { it.value }
        .distinctUntilChanged()

    /** Simulates the server answering for [uid] (so null means "confirmed: no profile"). */
    fun markLoaded(uid: String) {
        loaded.value += uid
    }

    fun addProfile(profile: UserProfile) {
        profiles.value += (profile.uid to profile)
        loaded.value += profile.uid
    }

    override suspend fun createProfile(
        uid: String,
        username: String,
        displayName: String,
    ): Outcome<Unit, ProfileError> {
        val error = nextError ?: ProfileError.USERNAME_TAKEN.takeIf {
            profiles.value.values.any { it.username == username }
        }
        if (error == null) addProfile(UserProfile(uid, username, displayName))
        return if (error == null) Outcome.Success(Unit) else Outcome.Failure(error)
    }

    fun profile(uid: String): UserProfile? = profiles.value[uid]

    private data class Some<T>(val value: T)
}
