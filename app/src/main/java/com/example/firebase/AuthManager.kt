package com.example.firebase

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Autenticación opcional con Google.
 *
 * Firebase es un extra en esta app (la escritura es 100 % local), así que todo
 * el flujo falla de forma silenciosa y controlada si la compilación no trae
 * `google-services.json`. Antes, `Firebase.auth` se resolvía en el constructor
 * y la app no arrancaba sin configuración de Firebase.
 */
class AuthManager(private val context: Context) {

    private val auth: FirebaseAuth? = runCatching { Firebase.auth }.getOrNull()

    private val credentialManager: CredentialManager? = runCatching {
        CredentialManager.create(context)
    }.getOrNull()

    val isConfigured: Boolean get() = auth != null

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth?.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    init {
        auth?.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
        }
    }

    fun attemptAutoSignIn(scope: CoroutineScope) {
        val auth = this.auth ?: return
        val credentialManager = this.credentialManager ?: return
        if (auth.currentUser != null) {
            _currentUser.value = auth.currentUser
            return
        }

        val clientId = webClientId() ?: return

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    auth.signInWithCredential(authCredential).await()
                    _currentUser.value = auth.currentUser
                }
            } catch (e: Exception) {
                Log.d("AuthManager", "Auto sign-in silent check: ${e.message}")
            }
        }
    }

    fun signInWithGoogle(activityContext: Context, scope: CoroutineScope, onSuccess: () -> Unit = {}) {
        _authError.value = null

        val auth = this.auth
        val credentialManager = this.credentialManager
        if (auth == null || credentialManager == null) {
            _authError.value = "Firebase no está configurado en esta compilación. La app funciona sin conexión."
            return
        }

        val clientId = webClientId()
        if (clientId == null) {
            _authError.value = "Falta la configuración de Google Sign-In (google-services.json)."
            return
        }

        _isLoading.value = true

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(activityContext as Activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    auth.signInWithCredential(authCredential).await()
                    _currentUser.value = auth.currentUser
                    _isLoading.value = false
                    onSuccess()
                } else {
                    _authError.value = "Tipo de credencial no esperado"
                    _isLoading.value = false
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w("AuthManager", "Google Sign-In cancelado por el usuario: ${e.message}")
                _isLoading.value = false
            } catch (e: Exception) {
                Log.e("AuthManager", "Fallo al iniciar sesión con Google", e)
                _authError.value = e.localizedMessage ?: "Fallo al iniciar sesión"
                _isLoading.value = false
            }
        }
    }

    fun signOut(scope: CoroutineScope) {
        auth?.signOut()
        _currentUser.value = null
        scope.launch {
            try {
                credentialManager?.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.e("AuthManager", "Error al limpiar estado de credenciales", e)
            }
        }
    }

    fun clearError() {
        _authError.value = null
    }

    /**
     * `default_web_client_id` la genera el plugin de Google Services a partir de
     * `google-services.json`. Si no existe, devolvemos null en vez de romper la
     * compilación.
     */
    private fun webClientId(): String? {
        return try {
            val resId = context.resources.getIdentifier(
                "default_web_client_id",
                "string",
                context.packageName
            )
            if (resId != 0) context.getString(resId) else null
        } catch (_: Exception) {
            null
        }
    }
}
