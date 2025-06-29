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

class PickLocationActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private var selectedPoint: GeoPoint? = null
    private var marker: Marker? = null

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
        addContentView(confirmButton, android.widget.FrameLayout.LayoutParams(

            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT

        ).apply { topMargin = 40 })

        map.controller.setZoom(15.0)
        map.controller.setCenter(GeoPoint(45.0, 9.0)) // Default center (Italy)

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
} 