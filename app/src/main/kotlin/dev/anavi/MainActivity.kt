package dev.anavi

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import dev.anavi.gpx.GpxData
import dev.anavi.gpx.GpxParser
import dev.anavi.gpx.GpxWriter
import dev.anavi.map.TrackOverlay
import dev.anavi.nav.TrackFollower
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import java.util.Locale

class MainActivity : Activity(), LocationListener {

    private lateinit var mapView: MapView
    private var map: MapLibreMap? = null
    private lateinit var speedText: TextView
    private lateinit var distanceText: TextView
    private lateinit var etaText: TextView
    private lateinit var remainText: TextView
    private lateinit var navRow: LinearLayout
    private lateinit var offTrackBanner: TextView
    private lateinit var cameraToggle: ImageButton
    private var locationManager: LocationManager? = null

    private var cameraLocked = true
    private var lastUserInteraction = 0L

    private var trackOverlay: TrackOverlay? = null
    private var activeGpx: GpxData? = null
    private var trackFollower: TrackFollower? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        speedText = findViewById(R.id.speedText)
        distanceText = findViewById(R.id.distanceText)
        etaText = findViewById(R.id.etaText)
        remainText = findViewById(R.id.remainText)
        navRow = findViewById(R.id.navRow)
        offTrackBanner = findViewById(R.id.offTrackBanner)
        cameraToggle = findViewById(R.id.cameraToggle)

        cameraToggle.setOnClickListener {
            cameraLocked = !cameraLocked
            updateToggleAppearance()
        }

        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { mlMap ->
            map = mlMap
            val styleUrl = if (BuildConfig.MAPTILER_KEY.isNotEmpty()) {
                "https://api.maptiler.com/maps/streets-v2/style.json?key=${BuildConfig.MAPTILER_KEY}"
            } else {
                "https://demotiles.maplibre.org/style.json"
            }
            mlMap.setStyle(styleUrl) {
                trackOverlay = TrackOverlay(mlMap)
                activeGpx?.let { showTrack(it) }
            }
            mlMap.cameraPosition = CameraPosition.Builder()
                .target(LatLng(48.2, 16.3))
                .zoom(12.0)
                .build()

            mlMap.addOnMoveListener(object : MapLibreMap.OnMoveListener {
                override fun onMoveBegin(detector: com.mapbox.android.gestures.MoveGestureDetector) {
                    if (cameraLocked) {
                        cameraLocked = false
                        lastUserInteraction = System.currentTimeMillis()
                        updateToggleAppearance()
                    }
                }
                override fun onMove(detector: com.mapbox.android.gestures.MoveGestureDetector) {}
                override fun onMoveEnd(detector: com.mapbox.android.gestures.MoveGestureDetector) {}
            })
        }

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQ_LOCATION
            )
        } else {
            startLocationUpdates()
        }

        handleGpxIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleGpxIntent(intent)
    }

    private fun handleGpxIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        loadGpx(uri)
    }

    fun loadGpx(uri: Uri) {
        try {
            val gpx = contentResolver.openInputStream(uri)?.use { GpxParser.parse(it) } ?: return
            activeGpx = gpx
            trackFollower = if (gpx.allTrackPoints().size >= 2) {
                navRow.visibility = View.VISIBLE
                TrackFollower(gpx)
            } else {
                navRow.visibility = View.GONE
                null
            }
            showTrack(gpx)
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to load GPX: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showTrack(gpx: GpxData) {
        val overlay = trackOverlay ?: return
        overlay.show(gpx)
        overlay.bounds(gpx)?.let { bounds ->
            map?.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 64))
        }
    }

    fun exportGpx(uri: Uri) {
        val gpx = activeGpx ?: return
        try {
            contentResolver.openOutputStream(uri)?.use { GpxWriter.write(gpx, it) }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to export GPX: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun openGpxPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/gpx+xml", "application/xml", "text/xml"))
        }
        startActivityForResult(intent, REQ_OPEN_GPX)
    }

    fun saveGpxPicker() {
        if (activeGpx == null) return
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/gpx+xml"
            putExtra(Intent.EXTRA_TITLE, "track.gpx")
        }
        startActivityForResult(intent, REQ_SAVE_GPX)
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        when (requestCode) {
            REQ_OPEN_GPX -> loadGpx(uri)
            REQ_SAVE_GPX -> exportGpx(uri)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_LOCATION &&
            grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        ) {
            startLocationUpdates()
        }
    }

    private fun startLocationUpdates() {
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        try {
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 1000L, 5f, this
            )
        } catch (_: SecurityException) {
        }
    }

    override fun onLocationChanged(location: Location) {
        val kmh = (location.speed * 3.6).toInt()
        speedText.text = getString(R.string.speed_format, kmh)
        updateNavigation(location, kmh)
        updateCamera(location)
    }

    private fun updateNavigation(location: Location, kmh: Int) {
        val follower = trackFollower ?: return
        val state = follower.update(location.latitude, location.longitude)

        // Off-track banner
        offTrackBanner.visibility = if (state.offTrack) View.VISIBLE else View.GONE

        // Remaining distance
        val remainM = state.distanceRemainingM
        remainText.text = if (remainM >= 1000) {
            getString(R.string.remain_km_format, remainM / 1000.0)
        } else {
            getString(R.string.remain_m_format, remainM.toInt())
        }

        // ETA based on current speed
        if (kmh > 3) {
            val remainH = remainM / 1000.0 / kmh
            val totalMin = (remainH * 60).toInt()
            val h = totalMin / 60
            val m = totalMin % 60
            etaText.text = getString(R.string.eta_format,
                String.format(Locale.ROOT, "%d:%02d", h, m))
        } else {
            etaText.text = getString(R.string.eta_format, "--:--")
        }

        // Total distance covered on the distanceText field
        val coveredKm = state.distanceCoveredM / 1000.0
        distanceText.text = String.format(Locale.ROOT, "%.1f km", coveredKm)
    }

    private fun updateCamera(location: Location) {
        val m = map ?: return
        val kmh = (location.speed * 3.6).toInt()

        // Auto-relock: speed-aware timeout
        // Riding (>20 km/h) → 5s, stationary → 15s
        if (!cameraLocked && lastUserInteraction > 0) {
            val elapsed = System.currentTimeMillis() - lastUserInteraction
            val timeout = if (kmh > 20) RELOCK_RIDING_MS else RELOCK_IDLE_MS
            if (elapsed > timeout) {
                cameraLocked = true
                updateToggleAppearance()
            }
        }

        if (!cameraLocked) return

        val pos = CameraPosition.Builder()
            .target(LatLng(location.latitude, location.longitude))

        // Rotate map to heading only when moving — avoids jitter when stopped
        if (kmh > 5 && location.hasBearing()) {
            pos.bearing(location.bearing.toDouble())
        }

        // Snappier at speed, smoother when slow
        val durationMs = if (kmh > 60) 300 else 600
        m.animateCamera(CameraUpdateFactory.newCameraPosition(pos.build()), durationMs)
    }

    private fun updateToggleAppearance() {
        cameraToggle.alpha = if (cameraLocked) 1.0f else 0.5f
    }

    override fun onStart() { super.onStart(); mapView.onStart() }
    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { super.onPause(); mapView.onPause() }
    override fun onStop() {
        super.onStop()
        mapView.onStop()
        locationManager?.removeUpdates(this)
    }
    override fun onDestroy() { super.onDestroy(); mapView.onDestroy() }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
    override fun onLowMemory() { super.onLowMemory(); mapView.onLowMemory() }

    companion object {
        private const val REQ_LOCATION = 1
        private const val REQ_OPEN_GPX = 2
        private const val REQ_SAVE_GPX = 3
        private const val RELOCK_RIDING_MS = 5_000L
        private const val RELOCK_IDLE_MS = 15_000L
    }
}
