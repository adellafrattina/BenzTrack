package it.uninsubria.benztrack

import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

public data class Address(

    var road: String,
    var hamlet: String,
    var town: String,
    var county: String,
    var state: String,
    var postcode: String,
    var country: String,
    var countryCode: String,
    var latitude: Double,
    var longitude: Double,
    var displayName: String

) {

    constructor(): this("", "", "", "", "", "", "", "", Double.NaN, Double.NaN, "")
}

sealed class ReverseGeocodeTaskResult {

    data class Success(val address: Address) : ReverseGeocodeTaskResult()
    data class Failure(val exception: Exception) : ReverseGeocodeTaskResult()
}

sealed class GeocodeTaskResult {

    data class Success(val geoPoints: List<Address>) : GeocodeTaskResult()
    data class Failure(val exception: Exception) : GeocodeTaskResult()
}

public class ReverseGeocodeTask(private val geoPoint: GeoPoint, private val timeout: Long = 5000L) {

    private var successListener: ((Address?) -> Unit)? = null
    private var failureListener: ((Exception) -> Unit)? = null
    private var completeListener: ((ReverseGeocodeTaskResult) -> Unit)? = null
    private var countdownRunnable: Runnable? = null
    private val handler = Handler(Looper.getMainLooper())
    private var requestStartedTime: Long = 0

    init {

        start()
    }

    // Method to start the geocoding task
    private fun start() {

        // Launch the task in a background thread (using Kotlin coroutines or a simple thread)
        Thread {

            try {

                requestStartedTime = System.currentTimeMillis()
                val address = fetchAddressFromCoordinates(geoPoint)
                if (System.currentTimeMillis() - requestStartedTime < timeout) {

                    cancelCountdown()
                    if (address != null) {

                        // Simulate a successful result and call the success listener
                        invokeOnMainThread {

                            successListener?.invoke(address)
                            completeListener?.invoke(ReverseGeocodeTaskResult.Success(address))
                        }
                    }

                    else {

                        invokeOnMainThread {

                            failureListener?.invoke(Exception("Address not found"))
                            completeListener?.invoke(ReverseGeocodeTaskResult.Failure(Exception("Address not found")))
                        }
                    }
                }

                else {

                    invokeOnMainThread {

                        failureListener?.invoke(Exception("No matching locations found"))
                        completeListener?.invoke(ReverseGeocodeTaskResult.Failure(Exception("No matching locations found")))
                    }
                }
            }

            catch (e: Exception) {

                // Simulate a failure and call the failure listener
                invokeOnMainThread {

                    cancelCountdown()
                    failureListener?.invoke(e)
                    completeListener?.invoke(ReverseGeocodeTaskResult.Failure(e))
                }
            }
        }.start()
    }

    // Method to fetch the address using Nominatim API
    private fun fetchAddressFromCoordinates(geoPoint: GeoPoint): Address? {

        val lat = geoPoint.latitude
        val lon = geoPoint.longitude
        val apiUrl = "https://nominatim.openstreetmap.org/reverse?lat=$lat&lon=$lon&format=json"

        try {

            val url = URL(apiUrl)
            val urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.requestMethod = "GET"
            val reader = BufferedReader(InputStreamReader(urlConnection.inputStream))
            val response = StringBuilder()

            reader.use {
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
            }

            // Parse the JSON response
            val jsonResponse = JSONObject(response.toString())

            if (jsonResponse.optString("error").isNotEmpty())
                return null

            val address = Address()
            val jsonAddress = jsonResponse.getJSONObject("address")
            address.road = jsonAddress.optString("road")
            address.hamlet = jsonAddress.optString("hamlet")
            address.town = jsonAddress.optString("town")
            address.county = jsonAddress.optString("county")
            address.state = jsonAddress.optString("state")
            address.postcode = jsonAddress.optString("postcode")
            address.country = jsonAddress.optString("country")
            address.countryCode = jsonAddress.optString("country_code")
            address.latitude = jsonResponse.optDouble("lat")
            address.longitude = jsonResponse.optDouble("lon")
            address.displayName = jsonResponse.optString("display_name")

            return address // Return the address
        }

        catch (e: Exception) {

            throw e // Re-throw the exception if the geocoding fails
        }
    }

    // Helper function to execute code on the main thread
    private fun invokeOnMainThread(action: () -> Unit) {
        handler.post(action)
    }

    // Setters for success and failure listeners
    fun addOnSuccessListener(listener: (Address?) -> Unit): ReverseGeocodeTask {

        successListener = listener
        return this
    }

    fun addOnFailureListener(listener: (Exception) -> Unit): ReverseGeocodeTask {

        failureListener = listener
        return this
    }

    fun addOnCompleteListener(listener: (ReverseGeocodeTaskResult) -> Unit): ReverseGeocodeTask {

        completeListener = listener
        return this
    }

    // Start the countdown timer
    private fun startCountdown() {

        countdownRunnable = Runnable {

            // Timeout has occurred, call the timeout listener
            invokeOnMainThread {

                failureListener?.invoke(Exception("Request timed out"))
            }
        }

        // Post the countdown to run after the specified timeout
        handler.postDelayed(countdownRunnable!!, timeout)
    }

    // Cancel the countdown timer if the request finishes in time
    private fun cancelCountdown() {

        countdownRunnable?.let {

            handler.removeCallbacks(it)
        }
    }
}

public class GeocodeTask(private val address: String, private val timeout: Long = 5000L) {

    private var successListener: ((List<Address>) -> Unit)? = null
    private var failureListener: ((Exception) -> Unit)? = null
    private var completeListener: ((GeocodeTaskResult) -> Unit)? = null
    private var countdownRunnable: Runnable? = null
    private val handler = Handler(Looper.getMainLooper())
    private var requestStartedTime: Long = 0

    init {

        start()
    }

    // Method to start the geocoding task
    private fun start() {

        startCountdown()

        // Launch the task in a background thread (using Kotlin coroutines or a simple thread)
        Thread {

            try {

                requestStartedTime = System.currentTimeMillis()
                val geoPoints = fetchCoordinatesFromAddress(address)

                if (System.currentTimeMillis() - requestStartedTime < timeout) {

                    cancelCountdown()
                    if (geoPoints.isNotEmpty()) {

                        // Simulate a successful result and call the success listener
                        invokeOnMainThread {

                            successListener?.invoke(geoPoints)
                            completeListener?.invoke(GeocodeTaskResult.Success(geoPoints))
                        }
                    }

                    else {

                        invokeOnMainThread {

                            failureListener?.invoke(Exception("No matching locations found"))
                            completeListener?.invoke(GeocodeTaskResult.Failure(Exception("No matching locations found")))
                        }
                    }
                }
            }

            catch (e: Exception) {

                // Simulate a failure and call the failure listener
                invokeOnMainThread {

                    cancelCountdown()
                    failureListener?.invoke(e)
                }
            }
        }.start()
    }

    // Method to fetch coordinates (latitude and longitude) using Nominatim API
    private fun fetchCoordinatesFromAddress(address: String): List<Address> {

        val apiUrl = "https://nominatim.openstreetmap.org/search?q=$address&format=json&addressdetails=1"
        val geoPoints = mutableListOf<Address>()

        try {

            val url = URL(apiUrl)
            val urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.requestMethod = "GET"
            val reader = BufferedReader(InputStreamReader(urlConnection.inputStream))
            val response = StringBuilder()

            reader.use {
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
            }

            // Parse the JSON response (multiple results)
            val jsonResponse = JSONArray(response.toString())
            for (i in 0 until jsonResponse.length()) {

                val result = jsonResponse.getJSONObject(i)

                val a = Address()
                val jsonAddress = result.getJSONObject("address")
                a.road = jsonAddress.optString("road")
                a.hamlet = jsonAddress.optString("hamlet")
                a.town = jsonAddress.optString("town")
                a.county = jsonAddress.optString("county")
                a.state = jsonAddress.optString("state")
                a.postcode = jsonAddress.optString("postcode")
                a.country = jsonAddress.optString("country")
                a.countryCode = jsonAddress.optString("country_code")
                a.latitude = result.optDouble("lat")
                a.longitude = result.optDouble("lon")
                a.displayName = result.optString("display_name")

                geoPoints.add(a)
            }
        }

        catch (e: Exception) {

            throw e // Re-throw the exception if the geocoding fails
        }

        return geoPoints
    }

    // Helper function to execute code on the main thread
    private fun invokeOnMainThread(action: () -> Unit) {

        handler.post(action)
    }

    // Setters for success and failure listeners
    fun addOnSuccessListener(listener: (List<Address>) -> Unit): GeocodeTask {

        successListener = listener
        return this
    }

    fun addOnFailureListener(listener: (Exception) -> Unit): GeocodeTask {

        failureListener = listener
        return this
    }

    fun addOnCompleteListener(listener: (GeocodeTaskResult) -> Unit): GeocodeTask {

        completeListener = listener
        return this
    }

    // Start the countdown timer
    private fun startCountdown() {

        countdownRunnable = Runnable {

            // Timeout has occurred, call the timeout listener
            invokeOnMainThread {

                failureListener?.invoke(Exception("Request timed out"))
            }
        }

        // Post the countdown to run after the specified timeout
        handler.postDelayed(countdownRunnable!!, timeout)
    }

    // Cancel the countdown timer if the request finishes in time
    private fun cancelCountdown() {

        countdownRunnable?.let {

            handler.removeCallbacks(it)
        }
    }
}

object Map {

    public fun getAddressBasedOnGeoPoint(lat: Double, lon: Double): ReverseGeocodeTask {

        return ReverseGeocodeTask(GeoPoint(lat, lon))
    }

    public fun getAddressBasedOnString(str: String): GeocodeTask {

        return GeocodeTask(str)
    }
}
