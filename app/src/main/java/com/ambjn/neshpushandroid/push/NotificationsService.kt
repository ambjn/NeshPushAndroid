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

/** Renders a local notification for a foreground push; the tap intent carries the payload to MainActivity. */
object NotificationsService {
    const val CHANNEL_DEFAULT = "push"
    const val CHANNEL_OFFERS = "offers_and_tips"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL_DEFAULT, context.getString(R.string.push_channel_name), NotificationManager.IMPORTANCE_HIGH))
        nm.createNotificationChannel(NotificationChannel(CHANNEL_OFFERS, context.getString(R.string.offers_channel_name), NotificationManager.IMPORTANCE_DEFAULT))
    }

    fun show(context: Context, push: PushMessage) {
        if (push.message.isBlank()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        ensureChannels(context)
        // One notification per event: the id is keyed by event_id so a retry replaces, never duplicates.
        val id = push.eventId?.hashCode() ?: push.message.hashCode()
        val tap = Intent(context, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtras(push.toBundle())
        val pending = PendingIntent.getActivity(context, id, tap, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, if (push.isMarketing) CHANNEL_OFFERS else CHANNEL_DEFAULT)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(push.title.ifBlank { context.getString(R.string.app_name) })
            .setContentText(push.message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(id, notification)
    }
}
