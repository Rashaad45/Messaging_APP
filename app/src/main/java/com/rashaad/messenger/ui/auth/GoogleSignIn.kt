package com.rashaad.messenger.ui.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.rashaad.messenger.R

/**
 * Shows the Google account picker via Credential Manager and returns an ID token.
 *
 * This lives in the UI layer because Credential Manager needs an *Activity* context to show
 * its bottom sheet, and ViewModels must not hold Activities (they outlive them). The token is
 * then passed to the ViewModel, which hands it to the repository.
 */
object GoogleSignIn {

    sealed interface Result {
        data class Token(val idToken: String) : Result
        data object Cancelled : Result
        data object Unavailable : Result
    }

    suspend fun requestIdToken(activityContext: Context): Result {
        // "Sign in with Google" button flow: always shows the account chooser (vs. the
        // GetGoogleIdOption bottom sheet meant for automatic one-tap sign-in).
        val option = GetSignInWithGoogleOption.Builder(
            serverClientId = activityContext.getString(R.string.default_web_client_id),
        ).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()

        return try {
            val credential = CredentialManager.create(activityContext)
                .getCredential(activityContext, request)
                .credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                Result.Token(GoogleIdTokenCredential.createFrom(credential.data).idToken)
            } else {
                Result.Unavailable
            }
        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "Google sign-in cancelled", e)
            Result.Cancelled
        } catch (e: NoCredentialException) {
            // No Google account on the device (or none usable for this app).
            Log.w(TAG, "No Google account available", e)
            Result.Unavailable
        } catch (e: GetCredentialException) {
            Log.w(TAG, "Google sign-in failed", e)
            Result.Unavailable
        } catch (e: GoogleIdTokenParsingException) {
            Log.w(TAG, "Invalid Google ID token", e)
            Result.Unavailable
        }
    }

    private const val TAG = "GoogleSignIn"
}
