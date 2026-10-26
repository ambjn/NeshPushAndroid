package com.ambjn.neshpushandroid.push

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
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
import java.util.TimeZone
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Registers this installation's FCM token with the app-owned backend using the
 * signed-in user's Firebase ID token. The server derives the UID from that token.
 * ponytail: plain HttpURLConnection + org.json; add OkHttp only if more endpoints appear.
 */
object DeviceRegistrar {
    enum class State { Pending, Registered, Failed }

    private val _state = MutableStateFlow(State.Pending)
    val state: StateFlow<State> = _state

    /** No-op when signed out: registration is deferred until a user exists. */
    suspend fun register(context: Context) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        _state.value = State.Pending
        _state.value = runCatching {
            val idToken = user.getIdToken(false).await().token ?: error("no id token")
            val fcmToken = FirebaseMessaging.getInstance().token.await()
            val body = JSONObject()
                .put("installationId", installationId(context))
                .put("token", fcmToken)
                .put("platform", "android")
                .put("notificationsEnabled", notificationsEnabled(context))
                .put("displayName", user.displayName)
                .put("timezone", TimeZone.getDefault().id)
            post("/api/app/push-devices/register", idToken, body)
            State.Registered
        }.getOrElse { State.Failed }
    }

    /** Call while still signed in; failures are ignored because sign-out must not block. */
    suspend fun revoke(context: Context) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        runCatching {
            val idToken = user.getIdToken(false).await().token ?: return
            post("/api/app/push-devices/revoke", idToken, JSONObject().put("installationId", installationId(context)))
        }
        _state.value = State.Pending
    }

    fun installationId(context: Context): String {
        val prefs = context.getSharedPreferences("push", Context.MODE_PRIVATE)
        return prefs.getString("installation_id", null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString("installation_id", it).apply()
        }
    }

    fun notificationsEnabled(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private suspend fun post(path: String, idToken: String, body: JSONObject) = withContext(Dispatchers.IO) {
        val base = BuildConfig.API_BASE_URL.ifEmpty { error("API_BASE_URL not configured") }
        val conn = URL(base + path).openConnection() as HttpURLConnection
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

    internal suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resumeWithException(it) }
        addOnCanceledListener { cont.cancel() }
    }
}
