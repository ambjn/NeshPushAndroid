package com.ambjn.neshpushandroid.push

import android.content.Context
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject
import java.time.Instant
import java.util.UUID

/**
 * Reports one authenticated tap per notification open. The event UUID is minted on the device
 * so the server can dedupe retries; the send must belong to the signed-in UID server-side.
 */
object TapReporter {
    data class Tap(val sendId: String, val routeKey: String, val eventId: String = UUID.randomUUID().toString())

    /** Extracts and clears the tap from a launch/new intent so a config change never re-reports it. */
    fun consume(intent: Intent?): Tap? {
        val sendId = intent?.getStringExtra(PushService.EXTRA_SEND_ID) ?: return null
        val route = Routes.sanitize(intent.getStringExtra(PushService.EXTRA_ROUTE_KEY))
        intent.removeExtra(PushService.EXTRA_SEND_ID)
        intent.removeExtra(PushService.EXTRA_ROUTE_KEY)
        return Tap(sendId, route)
    }

    suspend fun report(context: Context, tap: Tap) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        runCatching {
            val idToken = with(DeviceRegistrar) { user.getIdToken(false).await() }.token ?: return
            DeviceRegistrar.post(
                "/api/push-opened", idToken,
                JSONObject().put("sendId", tap.sendId).put("eventId", tap.eventId).put("openedAt", Instant.now().toString()),
            )
        }
    }
}
