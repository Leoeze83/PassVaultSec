package com.passvaultsec.app.core.auth

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.passvaultsec.app.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * Gestor de autenticación con Google mediante la API moderna CredentialManager y Firebase Auth.
 */
class GoogleAuthManager(
    private val context: Context,
    private val serverClientId: String = BuildConfig.WEB_CLIENT_ID
) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val credentialManager: CredentialManager = CredentialManager.create(context)

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
        }
    }

    /**
     * Inicia el flujo de autenticación con Google One Tap a través de CredentialManager.
     */
    suspend fun signInWithGoogle(activity: Activity): Result<FirebaseUser> {
        return try {
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
                context = activity
            )

            val credential = response.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(authCredential).await()
                val user = authResult.user ?: throw IllegalStateException("Usuario nulo tras autenticación")
                _currentUser.value = user
                Result.success(user)
            } else {
                Result.failure(IllegalStateException("Tipo de credencial no soportado"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cierra la sesión activa.
     */
    fun signOut() {
        auth.signOut()
        _currentUser.value = null
    }

    fun isUserSignedIn(): Boolean = auth.currentUser != null
    fun getCurrentUserEmail(): String = auth.currentUser?.email.orEmpty()
    fun getCurrentUserId(): String = auth.currentUser?.uid.orEmpty()
}
