package com.ambjn.neshpushandroid.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PushService : FirebaseMessagingService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        if (token.isEmpty()) return
        scope.launch { DeviceRegistrar.register() }
    }

    /**
     * Foreground only for notification+data messages: in the background FCM shows the banner itself and the
     * tap launches MainActivity with the data keys as extras. So exactly one banner is ever shown.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data.toMutableMap<String, String?>()
        if (data[PushMessage.KEY_MESSAGE].isNullOrEmpty()) data[PushMessage.KEY_MESSAGE] = remoteMessage.notification?.body
        if (data[PushMessage.KEY_TITLE].isNullOrEmpty()) data[PushMessage.KEY_TITLE] = remoteMessage.notification?.title
        NotificationsService.show(this, PushMessage(data))
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
