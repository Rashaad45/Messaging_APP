package com.rashaad.messenger.data.remote

/** Single place for Firestore document paths (must match firestore.rules and docs/DATA_MODEL.md). */
object FirestorePaths {
    fun user(uid: String) = "users/$uid"
    fun username(username: String) = "usernames/$username"
}
