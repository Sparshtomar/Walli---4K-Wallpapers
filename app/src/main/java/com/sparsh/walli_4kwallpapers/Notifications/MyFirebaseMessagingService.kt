package com.sparsh.walli_4kwallpapers.Notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.sparsh.walli_4kwallpapers.R
import com.sparsh.walli_4kwallpapers.Views.Activities.MainActivity

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        remoteMessage.notification?.let { notification ->
            // Use custom title and body or provide default fallback
            val title = notification.title ?: "🎉 New Wallpapers Alert!"
            val message = notification.body ?: "🚀 Discover fresh new wallpapers for your device now! Tap to explore the latest collections."

            // Show the notification
            showNotification(title, message)
        }
    }

    private fun showNotification(title: String, messageBody: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "default_channel_id"

        // Create a notification channel (API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Get notified when new wallpapers are available!"
                enableLights(true)
                lightColor = Color.CYAN
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Intent to open app when notification is clicked
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP) // Clear activity stack and bring existing instance to front
        }

        // Create the pending intent
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE // Use FLAG_IMMUTABLE for newer Android versions
        )

        // Create the notification with styling and actions
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.newlogo) // Custom icon
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.newlogo)) // Large icon
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent) // Set the intent for notification click

            // Add BigTextStyle for expanded notification
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageBody))

            // Customize with sound and vibration for attention
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setDefaults(NotificationCompat.DEFAULT_VIBRATE or NotificationCompat.DEFAULT_SOUND) // Default sound and vibration

        // Show the notification
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}
