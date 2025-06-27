package it.uninsubria.benztrack

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil

class BackgroundService : Service() {

    companion object {

        var isRunning = false
    }

    override fun onBind(intent: Intent?): IBinder? {

        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        isRunning = true

        val notification = NotificationHandler.createNotification(this, NotificationHandler.BACKGROUND_CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle("Running in the background")
            .setContentText("Syncing messages...")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setOngoing(true)
            .build()

        startForeground(1, notification)

        context = baseContext
        scope.launch {

            today = Timestamp.now()
            while (isActive) {

                val currentDate = Timestamp.now()

                if (user != Handler.loggedUser || currentDate.seconds >= 86400 + today.seconds) {

                    user = Handler.loggedUser
                    today = currentDate

                    NotificationHandler.cancelAllNotifications()
                    dateRegister = HashMap()
                    notificationRegister = HashMap()
                }

                if (user != null) {

                    Handler.database
                        .getUserCars(user!!.username)
                        .addOnSuccessListener { cars ->

                            for (car in cars) {

                                car.maintenancedate?.let { checkDate(car, it, currentDate, "Maintenance") }
                                car.insurancedate?.let { checkDate(car, it, currentDate, "Insurance") }
                                car.taxdate?.let { checkDate(car, it, currentDate, "Tax") }
                            }
                        }
                }

                delay(secondsToWait * 1000L) // Check every ${secondsToWait} seconds
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {

        isRunning = false

        super.onDestroy()
        job.cancel()
    }

    private fun checkDate(car: Car, date: Timestamp, currentDate: Timestamp, title: String) {

        initRegisters(car.plate, date, title)

        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val formattedDate = sdf.format(date.toDate())
        val timeDifferenceInDays = abs(daysBetweenDates(currentDate, date))

        val titleStr = title + " date" + " - " + car.plate
        val textStr: String =
            if (!isSameDay(currentDate, date) && currentDate > date) {

                if (timeDifferenceInDays > 1)
                    "You are $timeDifferenceInDays days late on your car ${title.lowercase()}! (deadline was $formattedDate)"
                else
                    "You are one day late on your car ${title.lowercase()}! (deadline was $formattedDate)"
            }

            else if (isSameDay(currentDate, date)) {

                "Today is the last day to pay the car ${title.lowercase()}!"
            }

            else if (date.seconds - currentDate.seconds <= 604800) {

                if (timeDifferenceInDays > 1)
                    "You have $timeDifferenceInDays days left to pay your car ${title.lowercase()} (deadline is $formattedDate)"
                else
                    "The ${title.lowercase()} payment is due tomorrow"
            }

            else {

                ""
            }

        val showNotification = textStr.isNotEmpty() && notificationRegister[car.plate]!![title]!!

        if (showNotification) {

            notificationRegister[car.plate]!![title] = false

            val n = NotificationHandler.createNotification(context, NotificationHandler.DATE_CHANNEL)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentTitle(titleStr)
                .setContentText(textStr)
                .build()

            NotificationHandler.notify(n, hash(title + car.plate))
        }
    }

    private fun hash(input: String): Int {

        var hash = 5381
        for (c in input)
            hash = (hash * 33) + c.code

        return hash
    }

    private fun initRegisters(plate: String, date: Timestamp, title: String) {

        if (notificationRegister.containsKey(plate)) {

            if (!notificationRegister[plate]!!.containsKey(title)) {

                notificationRegister[plate]!![title] = true
            }
        }

        else {

            notificationRegister[plate] = HashMap()
            notificationRegister[plate]!![title] = true
        }
    }

    private fun isSameDay(ts1: Timestamp, ts2: Timestamp): Boolean {

        val zoneId = ZoneId.systemDefault()

        val date1 = Instant.ofEpochSecond(ts1.seconds)
            .atZone(zoneId)
            .toLocalDate()

        val date2 = Instant.ofEpochSecond(ts2.seconds)
            .atZone(zoneId)
            .toLocalDate()

        return date1 == date2
    }

    private fun daysBetweenDates(ts1: Timestamp, ts2: Timestamp): Long {

        val zoneId = ZoneId.systemDefault()

        val date1 = Instant.ofEpochSecond(ts1.seconds).atZone(zoneId).toLocalDate()
        val date2 = Instant.ofEpochSecond(ts2.seconds).atZone(zoneId).toLocalDate()

        return ChronoUnit.DAYS.between(date1, date2)
    }

    private lateinit var today: Timestamp
    private val secondsToWait = 10
    private lateinit var context: Context
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private var dateRegister = HashMap<String, HashMap<String, Timestamp>>()
    private var notificationRegister = HashMap<String, HashMap<String, Boolean>>()
    private var user: User? = null
}
