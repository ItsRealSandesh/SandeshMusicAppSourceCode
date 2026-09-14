package com.sandeshmusic.app.data.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import android.util.Log
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.example.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoogleAuthManager(
    private val context: Context
) {
    private val credentialManager = CredentialManager.create(context)

    companion object {
        private const val TAG = "GoogleAuthManager"
    }

    private fun Context.findActivity(): Activity? {
        var ctx = this
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    fun getServerClientId(): String {
        val fallbackId = "882494299211-umma7eb5thaljqknpf3810cp0l2144b1.apps.googleusercontent.com"
        return try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) {
                val found = context.getString(resId)
                if (found.isNotBlank()) found else fallbackId
            } else {
                val str = context.getString(R.string.default_web_client_id)
                if (str.isNotBlank()) str else fallbackId
            }
        } catch (_: Exception) {
            fallbackId
        }
    }

    private fun extractIdToken(credential: Credential): String? {
        val bundle: Bundle = credential.data
        Log.d(TAG, "Parsing credential of type: ${credential.type}")

        // 1. Try standard GoogleIdTokenCredential parsing
        try {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(bundle)
            val token = googleIdTokenCredential.idToken
            if (!token.isNullOrBlank()) {
                Log.d(TAG, "Successfully extracted token via GoogleIdTokenCredential for user: ${googleIdTokenCredential.id}")
                return token
            }
        } catch (e: Exception) {
            Log.w(TAG, "GoogleIdTokenCredential.createFrom failed: ${e.message}")
        }

        // 2. Try known bundle keys
        val tokenKeys = listOf(
            "com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID_TOKEN",
            "id_token",
            "idToken",
            "google_id_token",
            "token",
            "com.google.android.gms.auth.api.credentials.id_token"
        )
        for (key in tokenKeys) {
            val tokenVal = bundle.getString(key)
            if (!tokenVal.isNullOrBlank()) {
                Log.d(TAG, "Found ID Token using key '$key'")
                return tokenVal
            }
        }

        // 3. Fallback: search any key containing a JWT (starts with ey and contains dots)
        try {
            for (key in bundle.keySet()) {
                val value = bundle.get(key)
                if (value is String && value.startsWith("ey") && value.count { it == '.' } >= 2) {
                    Log.d(TAG, "Extracted JWT token from dynamic bundle key: $key")
                    return value
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error scanning bundle keys: ${e.message}")
        }

        return null
    }

    suspend fun retrieveGoogleIdToken(activityContext: Context): Result<String> = withContext(Dispatchers.Main) {
        val effectiveContext = activityContext.findActivity() ?: activityContext
        val serverClientId = getServerClientId().trim()
        Log.d(TAG, "Requesting Google Sign-In with Web Client ID: $serverClientId")

        if (serverClientId.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Google Web Client ID is not configured."))
        }

        // Prepare credential options
        val signInWithGoogleOption = try {
            GetSignInWithGoogleOption.Builder(serverClientId)
                .build()
        } catch (e: Exception) {
            Log.w(TAG, "Could not build GetSignInWithGoogleOption: ${e.message}", e)
            null
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId)
            .setAutoSelectEnabled(false)
            .build()

        // 1. Primary Attempt: Use GetSignInWithGoogleOption if available, else GoogleIdOption
        val primaryRequest = GetCredentialRequest.Builder().apply {
            if (signInWithGoogleOption != null) {
                addCredentialOption(signInWithGoogleOption)
            } else {
                addCredentialOption(googleIdOption)
            }
        }.build()

        try {
            Log.d(TAG, "Calling credentialManager.getCredential...")
            val response = credentialManager.getCredential(effectiveContext, primaryRequest)
            val token = extractIdToken(response.credential)
            if (!token.isNullOrBlank()) {
                return@withContext Result.success(token)
            } else {
                Log.e(TAG, "Credential returned but no ID token could be extracted: ${response.credential.type}")
                return@withContext Result.failure(IllegalStateException("Unable to retrieve Google security token. Please try again or use email sign in."))
            }
        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "User dismissed or cancelled account selection")
            return@withContext Result.failure(CancellationException("Google Sign-In was cancelled."))
        } catch (e: NoCredentialException) {
            Log.w(TAG, "NoCredentialException during primary attempt. Trying secondary fallback...", e)
            // Secondary Attempt with GoogleIdOption
            try {
                val fallbackRequest = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                val response = credentialManager.getCredential(effectiveContext, fallbackRequest)
                val token = extractIdToken(response.credential)
                if (!token.isNullOrBlank()) {
                    return@withContext Result.success(token)
                }
            } catch (fallbackEx: Exception) {
                if (fallbackEx is GetCredentialCancellationException) {
                    return@withContext Result.failure(CancellationException("Google Sign-In was cancelled."))
                }
                Log.e(TAG, "Fallback attempt also failed: ${fallbackEx.message}", fallbackEx)
            }
            return@withContext Result.failure(Exception("No Google account was selected. Please choose a Google account or sign in with email."))
        } catch (e: GetCredentialException) {
            Log.e(TAG, "GetCredentialException during Google sign-in: ${e.type} - ${e.message}", e)
            val msg = when {
                e.message?.contains("16", ignoreCase = true) == true || e.message?.contains("cancel", ignoreCase = true) == true ->
                    "Google Sign-In was dismissed or interrupted. Please try again."
                e.message?.contains("10", ignoreCase = true) == true || e.message?.contains("developer", ignoreCase = true) == true ->
                    "Google Sign-In configuration error. Please use email & password to sign in."
                else -> e.message ?: "Google authentication could not be completed."
            }
            return@withContext Result.failure(Exception(msg, e))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during Google sign-in", e)
            return@withContext Result.failure(Exception(e.localizedMessage ?: "Google Sign-In encountered an unexpected error.", e))
        }
    }
}


