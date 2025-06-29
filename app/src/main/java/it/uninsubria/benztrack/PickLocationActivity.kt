package it.uninsubria.benztrack

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import android.view.MotionEvent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

class PickLocationActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private var selectedPoint: GeoPoint? = null
    private var marker: Marker? = null
    private lateinit var locationManager: LocationManager
    private val LOCATION_PERMISSION_REQUEST = 2001

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(applicationContext, androidx.preference.PreferenceManager.getDefaultSharedPreferences(applicationContext))

        map = MapView(this)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        setContentView(map)

        val confirmButton = Button(this)
        confirmButton.text = "Confirm Location"
        confirmButton.isEnabled = false
        confirmButton.tag = "confirm_button"

        addContentView(confirmButton, android.widget.FrameLayout.LayoutParams(

            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT

        ).apply { topMargin = 40 })

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST)

        else
            centerMapOnUserLocation()

        val tapOverlay = object : Overlay() {

            override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {

                val proj = mapView.projection
                val geoPoint = proj.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint
                selectedPoint = geoPoint

                if (marker == null) {

                    marker = Marker(map)
                    map.overlays.add(marker)
                }

                marker!!.position = geoPoint
                marker!!.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                map.invalidate()
                confirmButton.isEnabled = true

                return true
            }
        }

        map.overlays.add(tapOverlay)

        confirmButton.setOnClickListener {

            selectedPoint?.let {

                val data = Intent().apply {

                    putExtra("latitude", it.latitude)
                    putExtra("longitude", it.longitude)
                }

                setResult(Activity.RESULT_OK, data)
                finish()
            }
        }
    }

    private fun centerMapOnUserLocation() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            map.controller.setCenter(GeoPoint(45.0, 9.0))
            map.controller.setZoom(18.0)
            return
        }

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        val listener = object : LocationListener {

            override fun onLocationChanged(location: Location) {

                val userPoint = GeoPoint(location.latitude, location.longitude)
                map.controller.setCenter(userPoint)
                map.controller.setZoom(18.0)
                selectedPoint = userPoint
                if (marker == null) {
                    marker = Marker(map)
                    map.overlays.add(marker)
                }
                marker!!.position = userPoint
                marker!!.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                map.invalidate()
                val confirmButton = (map.parent as android.view.ViewGroup).findViewWithTag<Button>("confirm_button")
                confirmButton?.isEnabled = true
                locationManager.removeUpdates(this)
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, listener, Looper.getMainLooper())

        val lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (lastKnown != null) {

            val userPoint = GeoPoint(lastKnown.latitude, lastKnown.longitude)
            map.controller.setCenter(userPoint)
            map.controller.setZoom(18.0)
            selectedPoint = userPoint
            if (marker == null) {
                marker = Marker(map)
                map.overlays.add(marker)
            }
            marker!!.position = userPoint
            marker!!.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            map.invalidate()
            val confirmButton = (map.parent as android.view.ViewGroup).findViewWithTag<Button>("confirm_button")
            confirmButton?.isEnabled = true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            centerMapOnUserLocation()

        else {
            map.controller.setCenter(GeoPoint(45.0, 9.0))
            map.controller.setZoom(18.0)
        }
    }
} 