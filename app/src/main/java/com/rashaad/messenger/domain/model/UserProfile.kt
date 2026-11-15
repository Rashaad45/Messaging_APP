package com.rashaad.messenger.domain.model

/** A user's public profile, as stored in `users/{uid}`. */
data class UserProfile(val uid: String, val username: String, val displayName: String)
