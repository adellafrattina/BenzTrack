package it.uninsubria.benztrack

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHandler {

    public const val DATE_CHANNEL = "date_channel"
    public const val CO2_CHANNEL = "co2_channel"

    public fun init(context: Context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            (context as Activity).requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)

        manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createChannel(DATE_CHANNEL)
        createChannel(CO2_CHANNEL)
    }

    public fun notify(context: Context, channel: String, title: String, text: String) {

        val notification: Notification= NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
    }

    private fun createChannel(channel: String, name: String = channel, desc: String = "", importance: Int = NotificationManager.IMPORTANCE_HIGH) {

        val c = NotificationChannel(channel, name, importance)
        c.description = desc
        manager.createNotificationChannel(c)
    }

    private lateinit var manager: NotificationManager
}
