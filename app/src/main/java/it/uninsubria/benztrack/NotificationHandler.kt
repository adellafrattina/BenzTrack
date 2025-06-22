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
    public const val BACKGROUND_CHANNEL = "background_channel"

    public fun init(context: Context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            (context as Activity).requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)

        manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (manager.getNotificationChannel(DATE_CHANNEL) == null)
            createChannel(DATE_CHANNEL, importance = NotificationManager.IMPORTANCE_HIGH)
        if (manager.getNotificationChannel(CO2_CHANNEL) == null)
            createChannel(CO2_CHANNEL, importance = NotificationManager.IMPORTANCE_HIGH)
        if (manager.getNotificationChannel(BACKGROUND_CHANNEL) == null)
            createChannel(BACKGROUND_CHANNEL, importance = NotificationManager.IMPORTANCE_MIN)
    }

    public fun notify(context: Context, channel: String, title: String, text: String, priority: Int = NotificationCompat.PRIORITY_DEFAULT) {

        val notification: Notification = createNotification(context, channel)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(priority)
            .build()

        manager.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
    }

    public fun notify(context: Context, channel: String, title: String, text: String, priority: Int = NotificationCompat.PRIORITY_DEFAULT, id: Int) {

        val notification: Notification = createNotification(context, channel)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(priority)
            .build()

        manager.notify(id, notification)
    }

    public fun notify(notification: Notification) {

        manager.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
    }

    public fun notify(notification: Notification, id: Int) {

        manager.notify(id, notification)
    }

    public fun createNotification(context: Context, channel: String): NotificationCompat.Builder {

        return NotificationCompat.Builder(context, channel)
    }

    public fun cancelAllNotifications() {

        manager.cancelAll()
    }

    public fun createChannel(channel: String, name: String = channel, desc: String = "", importance: Int = NotificationManager.IMPORTANCE_DEFAULT) {

        val c = NotificationChannel(channel, name, importance)
        c.description = desc
        manager.createNotificationChannel(c)
        if (importance >= NotificationManager.IMPORTANCE_DEFAULT)
            c.enableVibration(true)
    }

    private lateinit var manager: NotificationManager
}
