package it.uninsubria.benztrack

import android.app.Notification
import android.app.NotificationManager
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
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

            Log.d("test", "Start")

            while (isActive) {

                val currentDate = Timestamp.now()
                if (loggedUser != null) {

                    database
                        .getUserCars(loggedUser!!.username)
                        .addOnSuccessListener { cars ->

                            for (car in cars) {

                                car.maintenancedate?.let { checkDate(car, it, currentDate, "Maintenance date") }
                                car.insurancedate?.let { checkDate(car, it, currentDate, "Insurance date") }
                                car.taxdate?.let { checkDate(car, it, currentDate, "Tax date") }
                            }
                        }
                        .addOnFailureListener { e ->

                            Log.e("test", e.message!!)
                        }
                }

                delay(secondsToWait * 1000L) // Check every ${secondsToWait} seconds
            }

            Log.d("test", "End")
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

        var showTomorrow = notificationRegister[car.plate]!![title]!![0]
        var showThreeDays = notificationRegister[car.plate]!![title]!![1]
        var showOneWeek = notificationRegister[car.plate]!![title]!![2]

        //val title = title + " - " + car.plate
        if (currentDate > date) {

            //database.setNewMaintenanceDate(loggedUser!!.username, car.plate, null)
            val n = NotificationHandler.createNotification(context, NotificationHandler.DATE_CHANNEL)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentTitle(title)
                .setContentText("You are late on your ${title.lowercase()}!")
                .build()

            NotificationHandler.notify(n, hash(title))
        }

        else if (showTomorrow && date.seconds - currentDate.seconds <= 86400)  { // Tomorrow

            notificationRegister[car.plate]!![title]!![0] = false
            notificationRegister[car.plate]!![title]!![1] = false
            notificationRegister[car.plate]!![title]!![2] = false

            val n = NotificationHandler.createNotification(context, NotificationHandler.DATE_CHANNEL)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentTitle(title)
                .setContentText("The ${title.lowercase()} is due tomorrow")
                .build()

            NotificationHandler.notify(n, hash(title))
        }

        else if (showThreeDays && date.seconds - currentDate.seconds <= 259200)  { // Three days

            notificationRegister[car.plate]!![title]!![1] = false
            notificationRegister[car.plate]!![title]!![2] = false

            val n = NotificationHandler.createNotification(context, NotificationHandler.DATE_CHANNEL)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentTitle(title)
                .setContentText("The ${title.lowercase()} is due in three days")
                .build()

            NotificationHandler.notify(n, hash(title))
        }

        else if (showOneWeek && date.seconds - currentDate.seconds <= 604800)  { // One week

            notificationRegister[car.plate]!![title]!![2] = false

            val n = NotificationHandler.createNotification(context, NotificationHandler.DATE_CHANNEL)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentTitle(title)
                .setContentText("The ${title.lowercase()} is due in one week")
                .build()

            NotificationHandler.notify(n, hash(title))
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

                notificationRegister[plate]!![title] = BooleanArray(3)
                val array = notificationRegister[plate]!![title]!!
                array[0] = true
                array[1] = true
                array[2] = true
            }
        }

        else {

            notificationRegister[plate] = HashMap<String, BooleanArray>()
            notificationRegister[plate]!![title] = BooleanArray(3)
            val array = notificationRegister[plate]!![title]!!
            array[0] = true
            array[1] = true
            array[2] = true
        }

        if (dateRegister.containsKey(plate)) {

            if (dateRegister[plate]!!.containsKey(title)) {

                if (dateRegister[plate]!![title] != date) {

                    dateRegister[plate]!![title] = date
                    notificationRegister[plate]!![title] = BooleanArray(3)
                    val array = notificationRegister[plate]!![title]!!
                    array[0] = true
                    array[1] = true
                    array[2] = true
                }
            }

            else {

                dateRegister[plate]!![title] = date
            }
        }

        else {

            dateRegister[plate] = HashMap<String, Timestamp>()
            dateRegister[plate]!![title] = date
        }
    }

    private val secondsToWait = 10
    private lateinit var context: Context
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private var dateRegister = HashMap<String, HashMap<String, Timestamp>>()
    private var notificationRegister = HashMap<String, HashMap<String, BooleanArray>>()
}
