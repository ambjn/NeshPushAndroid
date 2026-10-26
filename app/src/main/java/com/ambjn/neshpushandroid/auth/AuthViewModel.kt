package com.ambjn.neshpushandroid.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ambjn.neshpushandroid.R
import com.ambjn.neshpushandroid.push.DeviceRegistrar
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()

    private val _user = MutableStateFlow(auth.currentUser)
    val user: StateFlow<FirebaseUser?> = _user

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val listener = FirebaseAuth.AuthStateListener { _user.value = it.currentUser }

    init {
        auth.addAuthStateListener(listener)
    }

    /** [context] must be an Activity: Credential Manager shows a system sheet. */
    fun signInWithGoogle(context: Context) {
        if (_loading.value) return
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val option = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(context.getString(R.string.default_web_client_id))
                    .build()
                val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
                val result = CredentialManager.create(context).getCredential(context, request)
                val idToken = GoogleIdTokenCredential.createFrom(result.credential.data).idToken
                auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
            } catch (_: GetCredentialCancellationException) {
                // user dismissed the sheet
            } catch (_: NoCredentialException) {
                _error.value = "Couldn't reach Google. Check your connection and that a Google account is on this device."
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Sign-in failed."
            } finally {
                _loading.value = false
            }
        }
    }

    fun signOut(context: Context) {
        viewModelScope.launch {
            // Revoke this installation while the ID token is still obtainable.
            DeviceRegistrar.revoke(context.applicationContext)
            auth.signOut()
            // Forget the chosen account so the next sign-in shows the picker again.
            runCatching { CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest()) }
        }
    }

    override fun onCleared() = auth.removeAuthStateListener(listener)
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
    addOnCanceledListener { cont.cancel() }
}
