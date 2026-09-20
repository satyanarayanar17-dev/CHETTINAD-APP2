package com.example.util

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

object GoogleSignInHelper {
    private const val TAG = "GoogleSignInHelper"

    // Default Web Client ID configured for Chettinad Care Google Sign In
    const val DEFAULT_WEB_CLIENT_ID = "16979430375-sample.apps.googleusercontent.com"

    suspend fun launchGoogleSignIn(
        context: Context,
        serverClientId: String = DEFAULT_WEB_CLIENT_ID
    ): Result<String> {
        return try {
            val credentialManager = CredentialManager.create(context)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response: GetCredentialResponse = credentialManager.getCredential(
                request = request,
                context = context
            )
            handleSignInResult(response)
        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "Google Sign-In was cancelled by user")
            Result.failure(e)
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Google Sign-In failed", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during Google Sign-In", e)
            Result.failure(e)
        }
    }

    private fun handleSignInResult(response: GetCredentialResponse): Result<String> {
        val credential = response.credential
        return when {
            credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                try {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    Result.success(googleIdTokenCredential.idToken)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse Google ID Token", e)
                    Result.failure(e)
                }
            }
            else -> {
                Result.failure(IllegalStateException("Unsupported credential type: ${credential.type}"))
            }
        }
    }
}
