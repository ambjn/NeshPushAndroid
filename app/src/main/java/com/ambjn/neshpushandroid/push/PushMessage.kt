package com.ambjn.neshpushandroid.push

import android.os.Bundle

/**
 * Slim push payload model over the FCM data map / tap-intent extras (same shape as the reference
 * VtFirebaseMessagingService → PushMessage → notification routing). Keys match nerd-push-web lib/push/payload.ts.
 */
class PushMessage(data: Map<String, String?>) {
    val title: String = data[KEY_TITLE].orEmpty()
    val message: String = data[KEY_MESSAGE].orEmpty()
    val eventSource: String? = data[KEY_EVENT_SOURCE]
    val eventId: String? = data[KEY_EVENT_ID]
    val notificationType: String? = data[KEY_NOTIFICATION_TYPE]
    val route: Route? = Route.fromKey(data[KEY_ROUTE])
    val rawRoute: String? = data[KEY_ROUTE]
    val targetUserId: String? = data[KEY_TARGET_USER_ID]
    val webUrl: String? = data[KEY_WEB_URL]?.takeIf { it.startsWith("https://") }

    /** A NerdPush event we are willing to act on: our event_source and an allowlisted route. */
    val isNerdPush: Boolean get() = eventSource == EVENT_SOURCE_NERDPUSH && route != null
    val isMarketing: Boolean get() = notificationType == TYPE_MARKETING

    fun toBundle(): Bundle = Bundle().apply {
        putString(KEY_TITLE, title); putString(KEY_MESSAGE, message)
        eventSource?.let { putString(KEY_EVENT_SOURCE, it) }; eventId?.let { putString(KEY_EVENT_ID, it) }
        notificationType?.let { putString(KEY_NOTIFICATION_TYPE, it) }; rawRoute?.let { putString(KEY_ROUTE, it) }
        targetUserId?.let { putString(KEY_TARGET_USER_ID, it) }; webUrl?.let { putString(KEY_WEB_URL, it) }
    }

    companion object {
        const val KEY_TITLE = "title"
        const val KEY_MESSAGE = "message"
        const val KEY_EVENT_SOURCE = "event_source"
        const val KEY_EVENT_ID = "event_id"
        const val KEY_NOTIFICATION_TYPE = "notification_type"
        const val KEY_ROUTE = "route"
        const val KEY_TARGET_USER_ID = "target_user_id"
        const val KEY_WEB_URL = "web_url"
        const val EVENT_SOURCE_NERDPUSH = "NerdPush"
        const val TYPE_MARKETING = "nerdpush_marketing"
        val KEYS = listOf(KEY_TITLE, KEY_MESSAGE, KEY_EVENT_SOURCE, KEY_EVENT_ID, KEY_NOTIFICATION_TYPE, KEY_ROUTE, KEY_TARGET_USER_ID, KEY_WEB_URL)

        fun fromBundle(bundle: Bundle?): PushMessage? {
            if (bundle == null) return null
            val map = KEYS.associateWith { bundle.getString(it) }
            return if (map[KEY_EVENT_SOURCE] == null && map[KEY_ROUTE] == null) null else PushMessage(map)
        }
    }
}
