package it.uninsubria.benztrack

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import androidx.core.graphics.toColorInt

class PickLocationActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private var selectedPoint: GeoPoint? = null
    private var marker: Marker? = null
    private lateinit var locationManager: LocationManager
    private val LOCATION_PERMISSION_REQUEST = 2001
    private lateinit var tickButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(applicationContext, androidx.preference.PreferenceManager.getDefaultSharedPreferences(applicationContext))

        map = MapView(this)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        setContentView(map)

        // Enable the back arrow in the action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Style for opaque blue rounded buttons
        fun createBlueRoundedBackground(): GradientDrawable {

            return GradientDrawable().apply {

                shape = GradientDrawable.RECTANGLE
                cornerRadius = 20f
                setColor("#29B6F6".toColorInt())
            }
        }

        // Add crosshair button (bottom left)
        val crosshairButton = ImageButton(this)

        crosshairButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_crosshair))
        crosshairButton.setColorFilter(Color.WHITE)
        crosshairButton.background = createBlueRoundedBackground()
        crosshairButton.scaleType = ImageView.ScaleType.CENTER
        crosshairButton.setPadding(0, 0, 0, 0)
        crosshairButton.contentDescription = "My Location"

        val crosshairParams = android.widget.FrameLayout.LayoutParams(150, 150)

        crosshairParams.gravity = android.view.Gravity.BOTTOM or android.view.Gravity.START
        crosshairParams.marginStart = 32
        crosshairParams.bottomMargin = 32

        fun safeAddContentView(view: View, params: ViewGroup.LayoutParams) {

            (view.parent as? ViewGroup)?.removeView(view)
            addContentView(view, params)
        }

        safeAddContentView(crosshairButton, crosshairParams)

        crosshairButton.setOnClickListener {

            centerMapOnUserLocation()
        }

        // Add tick button (bottom right)
        tickButton = ImageButton(this)

        tickButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_check))
        tickButton.setColorFilter(Color.WHITE)
        tickButton.background = createBlueRoundedBackground()
        tickButton.scaleType = ImageView.ScaleType.CENTER
        tickButton.setPadding(0, 0, 0, 0)
        tickButton.contentDescription = getString(R.string.tick)

        val tickParams = android.widget.FrameLayout.LayoutParams(150, 150)

        tickParams.gravity = android.view.Gravity.BOTTOM or android.view.Gravity.END
        tickParams.marginEnd = 32
        tickParams.bottomMargin = 32

        safeAddContentView(tickButton, tickParams)
        tickButton.isEnabled = false
        tickButton.alpha = 0.5f
        tickButton.setOnClickListener {

            selectedPoint?.let {

                val data = Intent().apply {

                    putExtra("latitude", it.latitude)
                    putExtra("longitude", it.longitude)
                }

                setResult(Activity.RESULT_OK, data)
                finish()
            }
        }

        // Tap overlay (after tickButton is defined)
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
                tickButton.isEnabled = true
                tickButton.alpha = 1.0f

                return true
            }
        }

        map.overlays.add(tapOverlay)

        // Add a search bar at the top
        val searchLayout = LinearLayout(this)
        searchLayout.orientation = LinearLayout.HORIZONTAL
        searchLayout.setBackgroundColor("#CCFFFFFF".toColorInt()) // Lighter semi-opaque dark
        val searchEdit = EditText(this)
        searchEdit.hint = "Search location..."
        val searchButton = Button(this)

        // Use magnifying glass icon
        val searchIcon: Drawable? = ContextCompat.getDrawable(this, android.R.drawable.ic_menu_search)
        searchButton.background = null
        searchButton.setCompoundDrawablesWithIntrinsicBounds(searchIcon, null, null, null)
        searchButton.text = ""

        searchLayout.addView(searchEdit, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        // Set button size and margin in dp
        val scale = resources.displayMetrics.density
        val sizeInPx = (50 * scale + 0.5f).toInt() // 32dp
        val marginInPx = (8 * scale + 0.5f).toInt() // 8dp
        val searchButtonParams = LinearLayout.LayoutParams(sizeInPx, sizeInPx)
        searchButtonParams.leftMargin = marginInPx
        searchLayout.addView(searchButton, searchButtonParams)

        safeAddContentView(searchLayout, android.widget.FrameLayout.LayoutParams(

            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT

        ).apply { topMargin = 0 })

        // Add a ListView for search results
        val resultsListView = ListView(this)
        resultsListView.visibility = View.GONE
        resultsListView.setBackgroundColor("#CCFFFFFF".toColorInt()) // Match lighter dark opaque

        safeAddContentView(resultsListView, android.widget.FrameLayout.LayoutParams(

            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT

        ).apply { topMargin = 120 }) // Adjust topMargin as needed

        var lastAddresses: List<Address> = emptyList()

        searchButton.setOnClickListener {

            val query = searchEdit.text.toString().trim()

            try {

                Map.getAddressBasedOnString(query)
                    .addOnSuccessListener { addresses ->

                        if (addresses.isNotEmpty()) {

                            lastAddresses = addresses
                            val items = addresses.map { it.displayName.ifEmpty { "${it.latitude},${it.longitude}" } }
                            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
                            resultsListView.adapter = adapter
                            resultsListView.visibility = View.VISIBLE
                        }

                        else {

                            resultsListView.visibility = View.GONE
                        }
                    }

                    .addOnFailureListener { e ->

                        resultsListView.visibility = View.GONE
                    }

            } catch (_: Exception) {}
        }

        resultsListView.setOnItemClickListener { _, _, position, _ ->

            val addr = lastAddresses[position]
            val geoPoint = GeoPoint(addr.latitude, addr.longitude)
            map.controller.setCenter(geoPoint)
            map.controller.setZoom(18.0)
            selectedPoint = geoPoint

            if (marker == null) {

                marker = Marker(map)
                map.overlays.add(marker)
            }

            marker!!.position = geoPoint
            marker!!.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            map.invalidate()
            tickButton.isEnabled = true
            tickButton.alpha = 1.0f
            resultsListView.visibility = View.GONE
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST)

        else
            centerMapOnUserLocation()
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
                tickButton.isEnabled = true
                tickButton.alpha = 1.0f
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
            tickButton.isEnabled = true
            tickButton.alpha = 1.0f
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

    // Add this override to handle the back arrow click
    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
} 