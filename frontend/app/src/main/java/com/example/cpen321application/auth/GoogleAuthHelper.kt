package com.example.cpen321application.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class GoogleUserProfile(
    val firstName: String,
    val lastName: String,
    val displayName: String,
)

suspend fun signInWithGoogle(
    context: Context,
    serverClientId: String,
): GoogleUserProfile? = withContext(Dispatchers.IO) {
    if (serverClientId.isBlank()) {
        return@withContext null
    }

    try {
        val credentialManager = CredentialManager.create(context)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = credentialManager.getCredential(context, request)
        val credential = result.credential

        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)

            val firstName = googleIdTokenCredential.givenName ?: "Google"
            val lastName = googleIdTokenCredential.familyName ?: "User"
            val displayName = googleIdTokenCredential.displayName ?: "$firstName $lastName"

            return@withContext GoogleUserProfile(
                firstName = firstName,
                lastName = lastName,
                displayName = displayName,
            )
        }
    } catch (_: Exception) {
        return@withContext null
    }

    return@withContext null
}
