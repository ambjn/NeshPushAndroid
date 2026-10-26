package com.ambjn.neshpushandroid.push

import com.ambjn.neshpushandroid.BuildConfig
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Sends this device's FCM token to the dashboard backend, authenticated with the user's Firebase ID token. */
object DeviceRegistrar {
    enum class State { Pending, Registered, Failed }

    private val _state = MutableStateFlow(State.Pending)
    val state: StateFlow<State> = _state

    /** No-op when signed out. */
    suspend fun register() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        _state.value = runCatching {
            val idToken = user.getIdToken(false).await().token ?: error("no id token")
            val fcmToken = FirebaseMessaging.getInstance().token.await()
            post(idToken, JSONObject().put("token", fcmToken).put("displayName", user.displayName))
            State.Registered
        }.getOrElse { State.Failed }
    }

    private suspend fun post(idToken: String, body: JSONObject) = withContext(Dispatchers.IO) {
        val base = BuildConfig.API_BASE_URL.ifEmpty { error("API_BASE_URL not configured") }
        val conn = URL("$base/api/devices/register").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $idToken")
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode}")
        } finally {
            conn.disconnect()
        }
    }

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resumeWithException(it) }
        addOnCanceledListener { cont.cancel() }
    }
}
