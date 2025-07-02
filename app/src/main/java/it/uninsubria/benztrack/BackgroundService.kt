package it.uninsubria.benztrack

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.firebase.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale
import kotlin.math.abs

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
            .setContentTitle(getString(R.string.running_in_the_background))
            .setContentText(getString(R.string.syncing_messages))
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setOngoing(true)
            .build()

        startForeground(1, notification)

        Handler.database.setContext(this)

        today = Timestamp.now()
        context = baseContext
        mainScope.launch {

            while (isActive) {

                mutex.withLock {

                    val currentDate = Timestamp.now()

                    if (user != Handler.loggedUser || currentDate.seconds >= 86400 + today.seconds) {

                        user = Handler.loggedUser
                        today = currentDate

                        NotificationHandler.cancelAllNotifications()
                        notificationDateRegister = HashMap()
                    }
                }
            }
        }

        dateScope.launch {

            while (isActive) {

                mutex.withLock {

                    if (user != null) {

                        val currentDate = Timestamp.now()
                        Handler.database
                            .getUserCars(user!!.username)
                            .addOnSuccessListener { cars ->

                                for (car in cars) {

                                    car.maintenancedate?.let { checkDate(car, it, currentDate, getString(R.string.maintenance)) }
                                    car.insurancedate?.let { checkDate(car, it, currentDate, getString(R.string.insurance)) }
                                    car.taxdate?.let { checkDate(car, it, currentDate, getString(R.string.tax)) }
                                }
                            }
                    }
                }

                delay(10_000) // Check every 10 seconds
            }
        }

        co2Scope.launch {

            while (isActive) {

                mutex.withLock {

                    if (user != null) {

                        val u = user
                        Handler.database
                            .getUserCars(user!!.username)
                            .addOnSuccessListener { cars ->

                                for (car in cars) {

                                    Handler.database
                                        .getUserCarModel(u!!.username, car.plate)
                                        .addOnSuccessListener { model ->

                                            Handler.database
                                                .getRefillData(u.username, car.plate, lastDate)
                                                .addOnSuccessListener { refills ->

                                                    if (refills.size >= 3) {

                                                        var prevRefill = refills[0]
                                                        var lastEmittedCO2 = 0.0f
                                                        var sumEmittedCO2 = 0.0f
                                                        var lastCost = 0.0f
                                                        var sumCost = 0.0f
                                                        var lastTravelledKm = 0.0f
                                                        var lastDaysInterval = 0L
                                                        for (i in 1 until refills.size) {

                                                            val currentRefill = refills[i]

                                                            val consumedLiters = prevRefill.currentfuelamount + prevRefill.amount / prevRefill.ppl - currentRefill.currentfuelamount
                                                            val travelledKm = if (currentRefill.mileage - prevRefill.mileage > 0) currentRefill.mileage - prevRefill.mileage else 1.0f
                                                            lastTravelledKm = travelledKm
                                                            val daysInterval = if (daysBetweenDates(currentRefill.date, prevRefill.date) > 0) daysBetweenDates(currentRefill.date, prevRefill.date) else 1
                                                            lastDaysInterval = daysInterval
                                                            val emittedCO2 = ((consumedLiters * model.fuel.value) / travelledKm) / daysInterval
                                                            lastEmittedCO2 = emittedCO2
                                                            sumEmittedCO2 += emittedCO2
                                                            val cost = ((consumedLiters * prevRefill.ppl) / travelledKm) / daysInterval
                                                            lastCost = cost
                                                            sumCost += cost

                                                            prevRefill = currentRefill
                                                        }

                                                        // Calculate CO2 emission percentage
                                                        sumEmittedCO2 -= lastEmittedCO2
                                                        val avgEmittedCO2 = sumEmittedCO2 / (refills.size - 2)
                                                        val percCO2 = (abs(avgEmittedCO2 - lastEmittedCO2) * 100.0f) / avgEmittedCO2

                                                        // Calculate fuel cost
                                                        sumCost -= lastCost
                                                        val avgCost = sumCost / (refills.size - 2)

                                                        val titleStr = getString(R.string.co2_emissions) + " - " + car.plate
                                                        val textStr =
                                                            if (avgEmittedCO2 > lastEmittedCO2) {

                                                                getString(R.string.congratulations_co2, DecimalFormat("#.#").format(percCO2), DecimalFormat("#.##").format((avgCost - lastCost) * lastTravelledKm * lastDaysInterval))
                                                                //"Congratulations! You reduced your CO2 emissions by ${DecimalFormat("#.#").format(percCO2)}% and saved up €${DecimalFormat("#.##").format((avgCost - lastCost) * lastTravelledKm * lastDaysInterval)}"
                                                            }

                                                            else if (avgEmittedCO2 < lastEmittedCO2) {

                                                                getString(R.string.warning_co2, DecimalFormat("#.#").format(percCO2), DecimalFormat("#.##").format((lastCost - avgCost)* lastTravelledKm * lastDaysInterval))
                                                                //"Warning! You increased your CO2 emissions by ${DecimalFormat("#.#").format(percCO2)}% and lost the equivalent of €${DecimalFormat("#.##").format((lastCost - avgCost)* lastTravelledKm * lastDaysInterval)}"
                                                            }

                                                            else {

                                                                getString(R.string.you_can_do_better_co2, DecimalFormat("#.#").format(lastEmittedCO2 * 1000.0f))
                                                                //"You can do better! Your CO2 emissions are steady (${DecimalFormat("#.#").format(lastEmittedCO2 * 1000.0f)} g/km of CO2 per day)"
                                                            }

                                                        val n = NotificationHandler.createNotification(context, NotificationHandler.DATE_CHANNEL)
                                                            .setSmallIcon(R.drawable.ic_launcher_foreground)
                                                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                                                            .setContentTitle(titleStr)
                                                            .setContentText(textStr)
                                                            .setStyle(NotificationCompat.BigTextStyle().bigText(textStr))
                                                            .build()

                                                        NotificationHandler.notify(n, hash(titleStr + car.plate))

                                                        lastDate = refills[refills.size - 1].date.toDate()
                                                    }
                                                }
                                        }
                                }
                            }
                    }
                }

                delay(5_000) // Check every 5 seconds
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {

        isRunning = false

        super.onDestroy()
        mainJob.cancel()
        dateJob.cancel()
        co2Job.cancel()
    }

    private fun checkDate(car: Car, date: Timestamp, currentDate: Timestamp, title: String) {

        initRegisters(notificationDateRegister, car.plate, title)

        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val formattedDate = sdf.format(date.toDate())
        val timeDifferenceInDays = abs(daysBetweenDates(currentDate, date))

        val titleStr = title + " - " + car.plate
        val textStr: String =
            if (!isSameDay(currentDate, date) && currentDate > date) {

                if (timeDifferenceInDays > 1)
                    getString(R.string.pay_you_are_late_days, timeDifferenceInDays.toString(), title.lowercase(), formattedDate)
                    //"You are $timeDifferenceInDays days late on your car ${title.lowercase()}! (deadline was $formattedDate)"
                else
                    getString(R.string.pay_you_are_late_day, title.lowercase(), formattedDate)
                    //"You are one day late on your car ${title.lowercase()}! (deadline was $formattedDate)"
            }

            else if (isSameDay(currentDate, date)) {

                getString(R.string.pay_today, title.lowercase())
                //"Today is the last day to pay the car ${title.lowercase()}!"
            }

            else if (date.seconds - currentDate.seconds <= 604800) {

                if (timeDifferenceInDays > 1)
                    getString(R.string.pay_days_left, timeDifferenceInDays.toString(), title.lowercase(), formattedDate)
                    //"You have $timeDifferenceInDays days left to pay your car ${title.lowercase()} (deadline is $formattedDate)"
                else
                    getString(R.string.pay_tomorrow, title.lowercase())
                    //"The ${title.lowercase()} payment is due tomorrow"
            }

            else {

                ""
            }

        val showNotification = textStr.isNotEmpty() && notificationDateRegister[car.plate]!![title]!!

        if (showNotification) {

            notificationDateRegister[car.plate]!![title] = false

            val n = NotificationHandler.createNotification(context, NotificationHandler.DATE_CHANNEL)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentTitle(titleStr)
                .setContentText(textStr)
                .setStyle(NotificationCompat.BigTextStyle().bigText(textStr))
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

    private fun initRegisters(register: HashMap<String, HashMap<String, Boolean>>, plate: String, title: String) {

        if (register.containsKey(plate)) {

            if (!register[plate]!!.containsKey(title)) {

                register[plate]!![title] = true
            }
        }

        else {

            register[plate] = HashMap()
            register[plate]!![title] = true
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

    private var user: User? = null
    private lateinit var today: Timestamp
    private lateinit var context: Context
    private val mutex = Mutex()
    private val mainJob = Job()
    private val mainScope = CoroutineScope(Dispatchers.IO + mainJob)
    private val dateJob = Job()
    private val dateScope = CoroutineScope(Dispatchers.IO + dateJob)
    private val co2Job = Job()
    private val co2Scope = CoroutineScope(Dispatchers.IO + co2Job)
    private var notificationDateRegister = HashMap<String, HashMap<String, Boolean>>()
    private var lastDate = Date.from(Instant.EPOCH)
}
