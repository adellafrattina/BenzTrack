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

            Log.d("test", "Start")

            while (isActive) {

                val currentDate = Timestamp.now()
                if (loggedUser != null) {

                    database
                        .getUserCars(loggedUser!!.username)
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

            Log.d("test", "End")
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
        val showTomorrow = notificationRegister[car.plate]!![title]!![1]
        val showThreeDays = notificationRegister[car.plate]!![title]!![2]
        val showOneWeek = notificationRegister[car.plate]!![title]!![3]

        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val formattedDate = sdf.format(date.toDate())
        val timeDifferenceInDays = ceil(abs(date.seconds - currentDate.seconds).toDouble() / 86400.0)

        val titleStr = title + " date" + " - " + car.plate
        val textStr =
            if (timeDifferenceInDays > 1.4)
                "You have ${timeDifferenceInDays.toInt()} days left to pay your car ${title.lowercase()} (deadline is $formattedDate)"
            else
                "The ${title.lowercase()} payment is due tomorrow"

        if (showLate && currentDate > date) {

            notificationRegister[car.plate]!![title]!![0] = false
            notificationRegister[car.plate]!![title]!![1] = false
            notificationRegister[car.plate]!![title]!![2] = false
            notificationRegister[car.plate]!![title]!![3] = false

            val n = NotificationHandler.createNotification(context, NotificationHandler.DATE_CHANNEL)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentTitle(titleStr)
                .setContentText("You are ${timeDifferenceInDays.toInt()} days late on your car ${title.lowercase()}! (deadline was $formattedDate)")
                .build()

            NotificationHandler.notify(n, hash(title))

            return false
        }

        else if (showTomorrow && date.seconds - currentDate.seconds <= 86400)  { // Tomorrow

            notificationRegister[car.plate]!![title]!![1] = false
            notificationRegister[car.plate]!![title]!![2] = false
            notificationRegister[car.plate]!![title]!![3] = false

            val n = NotificationHandler.createNotification(context, NotificationHandler.DATE_CHANNEL)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentTitle(titleStr)
                .setContentText(textStr)
                .build()

            NotificationHandler.notify(n, hash(title))
        }

        else if (showThreeDays && date.seconds - currentDate.seconds <= 259200)  { // Three days

            notificationRegister[car.plate]!![title]!![2] = false
            notificationRegister[car.plate]!![title]!![3] = false

            val n = NotificationHandler.createNotification(context, NotificationHandler.DATE_CHANNEL)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentTitle(title)
                .setContentText(textStr)
                .build()

            NotificationHandler.notify(n, hash(title))
        }

        else if (showOneWeek && date.seconds - currentDate.seconds <= 604800)  { // One week

            notificationRegister[car.plate]!![title]!![3] = false

            val n = NotificationHandler.createNotification(context, NotificationHandler.DATE_CHANNEL)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentTitle(title)
                .setContentText(textStr)
                .build()

            NotificationHandler.notify(n, hash(title))
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

                notificationRegister[plate]!![title] = BooleanArray(4)
                val array = notificationRegister[plate]!![title]!!
                array[0] = true
                array[1] = true
                array[2] = true
                array[3] = true
            }
        }

        else {

            notificationRegister[plate] = HashMap()
            notificationRegister[plate]!![title] = BooleanArray(4)
            val array = notificationRegister[plate]!![title]!!
            array[0] = true
            array[1] = true
            array[2] = true
            array[3] = true
        }

        if (dateRegister.containsKey(plate)) {

            if (dateRegister[plate]!!.containsKey(title)) {

                if (dateRegister[plate]!![title] != date) {

                    dateRegister[plate]!![title] = date
                    notificationRegister[plate]!![title] = BooleanArray(4)
                    val array = notificationRegister[plate]!![title]!!
                    array[0] = true
                    array[1] = true
                    array[2] = true
                    array[3] = true
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

    private val secondsToWait = 10
    private lateinit var context: Context
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private var dateRegister = HashMap<String, HashMap<String, Timestamp>>()
    private var notificationRegister = HashMap<String, HashMap<String, BooleanArray>>()
}
