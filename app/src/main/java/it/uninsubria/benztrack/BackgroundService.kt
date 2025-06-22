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

            while (isActive) {

                val currentDate = Timestamp.now()

                if (user != Handler.loggedUser) {

                    user = Handler.loggedUser

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
                        .addOnFailureListener { e ->

                            val n = NotificationHandler.createNotification(context, NotificationHandler.BACKGROUND_CHANNEL)
                                .setSmallIcon(R.drawable.ic_launcher_background)
                                .setContentTitle("An exception has occurred")
                                .setContentText(e.message?:"")
                                .setPriority(NotificationCompat.PRIORITY_MAX)
                                .build()

                            NotificationHandler.notify(n)
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

    private fun checkDate(car: Car, date: Timestamp, currentDate: Timestamp, title: String): Boolean {

        initRegisters(car.plate, date, title)

        val showLate = notificationRegister[car.plate]!![title]!![0]
        val showToday = notificationRegister[car.plate]!![title]!![1]
        val showTomorrow = notificationRegister[car.plate]!![title]!![2]
        val showThreeDays = notificationRegister[car.plate]!![title]!![3]
        val showOneWeek = notificationRegister[car.plate]!![title]!![4]

        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val formattedDate = sdf.format(date.toDate())
        val timeDifferenceInDays = abs(daysBetweenDates(currentDate, date))

        val titleStr = title + " date" + " - " + car.plate
        val textStr =
            if (timeDifferenceInDays > 1)
                "You have $timeDifferenceInDays days left to pay your car ${title.lowercase()} (deadline is $formattedDate)"
            else
                "The ${title.lowercase()} payment is due tomorrow"

        if (showLate && !isSameDay(currentDate, date) && currentDate > date) {

            notificationRegister[car.plate]!![title]!![0] = false
            notificationRegister[car.plate]!![title]!![1] = false
            notificationRegister[car.plate]!![title]!![2] = false
            notificationRegister[car.plate]!![title]!![3] = false
            notificationRegister[car.plate]!![title]!![4] = false

            val text =
                if (timeDifferenceInDays > 1)
                    "You are $timeDifferenceInDays days late on your car ${title.lowercase()}! (deadline was $formattedDate)"
                else
                    "You are one day late on your car ${title.lowercase()}! (deadline was $formattedDate)"

            val n = NotificationHandler.createNotification(context, NotificationHandler.DATE_CHANNEL)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentTitle(titleStr)
                .setContentText(text)
                .build()

            NotificationHandler.notify(n, hash(title + car.plate))

            return false
        }

        else if (showToday && isSameDay(currentDate, date)) {

            notificationRegister[car.plate]!![title]!![1] = false
            notificationRegister[car.plate]!![title]!![2] = false
            notificationRegister[car.plate]!![title]!![3] = false
            notificationRegister[car.plate]!![title]!![4] = false

            val n = NotificationHandler.createNotification(context, NotificationHandler.DATE_CHANNEL)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentTitle(titleStr)
                .setContentText("Today is the last day to pay the car ${title.lowercase()}!")
                .build()

            NotificationHandler.notify(n, hash(title + car.plate))
        }

        else if (showTomorrow && date.seconds - currentDate.seconds <= 86400)  { // Tomorrow

            notificationRegister[car.plate]!![title]!![2] = false
            notificationRegister[car.plate]!![title]!![3] = false
            notificationRegister[car.plate]!![title]!![4] = false

            val n = NotificationHandler.createNotification(context, NotificationHandler.DATE_CHANNEL)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentTitle(titleStr)
                .setContentText(textStr)
                .build()

            NotificationHandler.notify(n, hash(title + car.plate))
        }

        else if (showThreeDays && date.seconds - currentDate.seconds <= 259200)  { // Three days

            notificationRegister[car.plate]!![title]!![3] = false
            notificationRegister[car.plate]!![title]!![4] = false

            val n = NotificationHandler.createNotification(context, NotificationHandler.DATE_CHANNEL)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentTitle(titleStr)
                .setContentText(textStr)
                .build()

            NotificationHandler.notify(n, hash(title + car.plate))
        }

        else if (showOneWeek && date.seconds - currentDate.seconds <= 604800)  { // One week

            notificationRegister[car.plate]!![title]!![4] = false

            val n = NotificationHandler.createNotification(context, NotificationHandler.DATE_CHANNEL)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentTitle(titleStr)
                .setContentText(textStr)
                .build()

            NotificationHandler.notify(n, hash(title + car.plate))
        }

        return true
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

                notificationRegister[plate]!![title] = BooleanArray(5)
                val array = notificationRegister[plate]!![title]!!
                array[0] = true
                array[1] = true
                array[2] = true
                array[3] = true
                array[4] = true
            }
        }

        else {

            notificationRegister[plate] = HashMap()
            notificationRegister[plate]!![title] = BooleanArray(5)
            val array = notificationRegister[plate]!![title]!!
            array[0] = true
            array[1] = true
            array[2] = true
            array[3] = true
            array[4] = true
        }

        if (dateRegister.containsKey(plate)) {

            if (dateRegister[plate]!!.containsKey(title)) {

                if (dateRegister[plate]!![title] != date) {

                    dateRegister[plate]!![title] = date
                    notificationRegister[plate]!![title] = BooleanArray(5)
                    val array = notificationRegister[plate]!![title]!!
                    array[0] = true
                    array[1] = true
                    array[2] = true
                    array[3] = true
                    array[4] = true
                }
            }

            else {

                dateRegister[plate]!![title] = date
            }
        }

        else {

            dateRegister[plate] = HashMap()
            dateRegister[plate]!![title] = date
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

    private val secondsToWait = 10
    private lateinit var context: Context
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private var dateRegister = HashMap<String, HashMap<String, Timestamp>>()
    private var notificationRegister = HashMap<String, HashMap<String, BooleanArray>>()
    private var user: User? = null
}
