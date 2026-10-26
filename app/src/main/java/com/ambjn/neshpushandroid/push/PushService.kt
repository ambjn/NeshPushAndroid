package com.ambjn.neshpushandroid.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.ambjn.neshpushandroid.MainActivity
import com.ambjn.neshpushandroid.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PushService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Token rotated: re-register if signed in. DeviceRegistrar reads the current token itself. */
    override fun onNewToken(token: String) {
        scope.launch { DeviceRegistrar.register(applicationContext) }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    /**
     * The server sends data-only messages, so this runs in every app state and exactly one
     * banner is shown. send_id and route_key ride along for tap reporting and routing.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.data["title"] ?: message.notification?.title ?: getString(R.string.app_name)
        val body = message.data["body"] ?: message.notification?.body ?: return
        showNotification(this, title, body, message.data["send_id"], message.data["route_key"])
    }

    companion object {
        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(
                    context.getString(R.string.push_channel_id),
                    context.getString(R.string.push_channel_name),
                    NotificationManager.IMPORTANCE_HIGH,
                )
            )
        }

        const val EXTRA_SEND_ID = "send_id"
        const val EXTRA_ROUTE_KEY = "route_key"

        fun showNotification(context: Context, title: String, body: String, sendId: String?, routeKey: String?) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) return
            ensureChannel(context)
            val requestCode = sendId?.hashCode() ?: System.currentTimeMillis().toInt()
            val tap = Intent(context, MainActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(EXTRA_SEND_ID, sendId)
                .putExtra(EXTRA_ROUTE_KEY, Routes.sanitize(routeKey))
            val pending = PendingIntent.getActivity(context, requestCode, tap, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val notification = NotificationCompat.Builder(context, context.getString(R.string.push_channel_id))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pending)
                .setAutoCancel(true)
                .build()
            context.getSystemService(NotificationManager::class.java).notify(requestCode, notification)
        }
    }
}
