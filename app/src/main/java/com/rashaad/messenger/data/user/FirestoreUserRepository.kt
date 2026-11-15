package com.rashaad.messenger.data.user

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreException.Code
import com.rashaad.messenger.core.Outcome
import com.rashaad.messenger.data.remote.FirestorePaths
import com.rashaad.messenger.domain.model.ProfileError
import com.rashaad.messenger.domain.model.UserProfile
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.tasks.await

@Singleton
class FirestoreUserRepository @Inject constructor(private val firestore: FirebaseFirestore) : UserRepository {

    override fun observeProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        val registration = firestore.document(FirestorePaths.user(uid)).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error) // handled by retryWhen below
                return@addSnapshotListener
            }
            if (snapshot == null) return@addSnapshotListener
            when {
                snapshot.exists() -> trySend(snapshot.toUserProfile())
                // A missing document from the offline cache only means "not cached yet",
                // not "no profile". Emitting null here would wrongly send an existing user
                // to onboarding when they open the app offline.
                snapshot.metadata.isFromCache -> Unit
                else -> trySend(null)
            }
        }
        awaitClose { registration.remove() }
    }
        .distinctUntilChanged()
        // Re-subscribe after transient listener errors with exponential backoff (1s, 2s, 4s … 30s).
        .retryWhen { cause, attempt ->
            if (cause is CancellationException) return@retryWhen false
            delay(min(MAX_BACKOFF_MS, INITIAL_BACKOFF_MS shl attempt.toInt().coerceAtMost(MAX_SHIFT)))
            true
        }

    override suspend fun createProfile(
        uid: String,
        username: String,
        displayName: String,
    ): Outcome<Unit, ProfileError> = try {
        val usernameRef = firestore.document(FirestorePaths.username(username))
        val userRef = firestore.document(FirestorePaths.user(uid))

        // A transaction (not a plain batch) so we can *read* the username first and report
        // "taken" precisely. Security rules additionally require both documents to be
        // written together, so the claim is atomic even if a client skips this check.
        val claimed = firestore.runTransaction { tx ->
            if (tx.get(usernameRef).exists()) {
                false
            } else {
                tx.set(
                    userRef,
                    mapOf(
                        "username" to username,
                        "displayName" to displayName,
                        "displayNameLower" to displayName.lowercase(),
                        "createdAt" to FieldValue.serverTimestamp(),
                    ),
                )
                tx.set(usernameRef, mapOf("uid" to uid))
                true
            }
        }.await()

        if (claimed) Outcome.Success(Unit) else Outcome.Failure(ProfileError.USERNAME_TAKEN)
    } catch (e: CancellationException) {
        throw e
    } catch (e: FirebaseFirestoreException) {
        Outcome.Failure(
            when (e.code) {
                // Transactions need the server: they fail fast when offline.
                Code.UNAVAILABLE, Code.DEADLINE_EXCEEDED -> ProfileError.NETWORK
                Code.PERMISSION_DENIED -> ProfileError.PERMISSION_DENIED
                else -> ProfileError.UNKNOWN
            },
        )
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toUserProfile() = UserProfile(
        uid = id,
        username = getString("username").orEmpty(),
        displayName = getString("displayName").orEmpty(),
    )

    private companion object {
        const val INITIAL_BACKOFF_MS = 1_000L
        const val MAX_BACKOFF_MS = 30_000L
        const val MAX_SHIFT = 5
    }
}
