package it.uninsubria.benztrack

import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

public class GeocodeTask(private val geoPoint: GeoPoint) {

    private var successListener: ((String) -> Unit)? = null
    private var failureListener: ((Exception) -> Unit)? = null

    // Method to start the geocoding task
    fun start() {

        // Launch the task in a background thread (using Kotlin coroutines or a simple thread)
        Thread {
            try {

                val address = fetchAddressFromCoordinates(geoPoint)
                if (address != null) {

                    // Simulate a successful result and call the success listener
                    invokeOnMainThread {

                        successListener?.invoke(address)
                    }
                }

                else {
                    // Simulate a failure (couldn't fetch the address)
                    throw Exception("Address not found")
                }
            }

            catch (e: Exception) {

                // Simulate a failure and call the failure listener
                invokeOnMainThread {

                    failureListener?.invoke(e)
                }
            }
        }.start()
    }

    // Method to fetch the address using Nominatim API
    private fun fetchAddressFromCoordinates(geoPoint: GeoPoint): String? {

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
            return jsonResponse.optString("display_name") // Return the address
        }

        catch (e: Exception) {

            throw e // Re-throw the exception if the geocoding fails
        }
    }

    // Helper function to execute code on the main thread
    private fun invokeOnMainThread(action: () -> Unit) {
        Handler(Looper.getMainLooper()).post(action)
    }

    // Setters for success and failure listeners
    fun addOnSuccessListener(listener: (String) -> Unit): GeocodeTask {

        successListener = listener
        return this
    }

    fun addOnFailureListener(listener: (Exception) -> Unit): GeocodeTask {

        failureListener = listener
        return this
    }
}

object Map {

    public fun getNameBasedOnGeoPoint(lat: Double, lon: Double): GeocodeTask {

        return GeocodeTask(GeoPoint(lat, lon))
    }
}
