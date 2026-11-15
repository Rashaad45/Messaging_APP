package com.rashaad.messenger.data.user

import com.rashaad.messenger.core.Outcome
import com.rashaad.messenger.domain.model.ProfileError
import com.rashaad.messenger.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    /**
     * Emits the user's profile, or null once the server has confirmed it doesn't exist yet.
     * Never emits null based only on an empty offline cache.
     */
    fun observeProfile(uid: String): Flow<UserProfile?>

    /**
     * Atomically claims [username] and creates the profile. Inputs must already be validated
     * and normalised (see UsernameValidator / DisplayNameValidator).
     */
    suspend fun createProfile(uid: String, username: String, displayName: String): Outcome<Unit, ProfileError>
}
