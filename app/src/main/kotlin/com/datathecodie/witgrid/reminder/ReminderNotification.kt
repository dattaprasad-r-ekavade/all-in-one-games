package com.datathecodie.witgrid.reminder

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.datathecodie.witgrid.MainActivity
import com.datathecodie.witgrid.R

internal object ReminderNotification {
    fun show(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notificationManager = context.getSystemService(NotificationManager::class.java) ?: return
        if (!notificationManager.areNotificationsEnabled()) return
        createNotificationChannel(context, notificationManager)

        val contentIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_REQUEST_CODE,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = notificationBuilder(context)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.reminder_notification_title))
            .setContentText(context.getString(R.string.reminder_notification_body))
            .setCategory(Notification.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel(
        context: Context,
        notificationManager: NotificationManager,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.reminder_notification_channel_description)
        }
        notificationManager.createNotificationChannel(channel)
    }

    @Suppress("DEPRECATION")
    private fun notificationBuilder(context: Context): Notification.Builder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, CHANNEL_ID)
        } else {
            Notification.Builder(context)
        }

    private const val CHANNEL_ID = "daily_reminder"
    private const val NOTIFICATION_ID = 4107
    private const val NOTIFICATION_REQUEST_CODE = 4108
}
