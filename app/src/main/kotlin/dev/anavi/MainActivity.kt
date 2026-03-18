package dev.anavi

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import dev.anavi.ui.ExpandH
import dev.anavi.ui.ExpandV
import dev.anavi.ui.Menu
import android.widget.TextView
import android.widget.Toast
import dev.anavi.ui.Crosshair
import dev.anavi.ui.IconButton
import dev.anavi.ui.MenuItem
import dev.anavi.ui.Ring
import dev.anavi.ui.UiMetrics
import dev.anavi.db.FavoriteLocation
import dev.anavi.db.FavoritesDb
import dev.anavi.gpx.GpxData
import dev.anavi.gpx.GpxParser
import dev.anavi.gpx.GpxWriter
import dev.anavi.map.PoiOverlay
import dev.anavi.map.TrackOverlay
import dev.anavi.nav.TrackFollower
import dev.anavi.poi.PoiCategory
import dev.anavi.poi.PoiDb
import dev.anavi.poi.PoiImporter
import dev.anavi.poi.PoiSearch
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
    private lateinit var menuButton: IconButton
    private lateinit var cameraToggle: IconButton
    private var locationManager: LocationManager? = null

    private var cameraLocked = true
    private var lastUserInteraction = 0L
    private var lastLocation: Location? = null

    private var trackOverlay: TrackOverlay? = null
    private var activeGpx: GpxData? = null
    private var trackFollower: TrackFollower? = null

    private lateinit var favoritesDb: FavoritesDb
    private lateinit var poiDb: PoiDb
    private var poiOverlay: PoiOverlay? = null
    private var activeMenu: Menu? = null
    private lateinit var crosshair: Crosshair
    private lateinit var ring: Ring

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        favoritesDb = FavoritesDb(this)
        poiDb = PoiDb(this)

        speedText = findViewById(R.id.speedText)
        distanceText = findViewById(R.id.distanceText)
        etaText = findViewById(R.id.etaText)
        remainText = findViewById(R.id.remainText)
        navRow = findViewById(R.id.navRow)
        offTrackBanner = findViewById(R.id.offTrackBanner)

        mapView = findViewById(R.id.mapView)

        val root = mapView.parent as FrameLayout
        val margin = UiMetrics.dp(this, UiMetrics.MARGIN).toInt()

        menuButton = IconButton(this).apply {
            setIcon(android.R.drawable.ic_menu_more)
            setOnClickListener { showMenu(it) }
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.START
            ).apply { setMargins(margin, margin, margin, margin) }
        }
        root.addView(menuButton)

        cameraToggle = IconButton(this).apply {
            setIcon(android.R.drawable.ic_menu_mylocation)
            setOnClickListener {
                cameraLocked = !cameraLocked
                updateToggleAppearance()
            }
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.END
            ).apply { setMargins(margin, margin, margin, margin) }
        }
        root.addView(cameraToggle)

        ring = Ring(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            onComplete = { x, y -> showContextMenu(x, y) }
        }
        root.addView(ring)

        crosshair = Crosshair(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            alpha = 0f
        }
        root.addView(crosshair)

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { mlMap ->
            map = mlMap
            val maptilerKey = getString(R.string.maptiler_key)
            val styleUrl = if (maptilerKey.isNotEmpty()) {
                "https://api.maptiler.com/maps/streets-v2/style.json?key=$maptilerKey"
            } else {
                "https://demotiles.maplibre.org/style.json"
            }
            mlMap.setStyle(styleUrl) {
                trackOverlay = TrackOverlay(mlMap)
                poiOverlay = PoiOverlay(mlMap)
                activeGpx?.let { showTrack(it) }
            }
            mlMap.cameraPosition = CameraPosition.Builder()
                .target(LatLng(48.2, 16.3))
                .zoom(12.0)
                .build()

            mlMap.addOnMoveListener(object : MapLibreMap.OnMoveListener {
                override fun onMoveBegin(detector: org.maplibre.android.gestures.MoveGestureDetector) {
                    if (cameraLocked) {
                        cameraLocked = false
                        lastUserInteraction = System.currentTimeMillis()
                        updateToggleAppearance()
                    }
                }
                override fun onMove(detector: org.maplibre.android.gestures.MoveGestureDetector) {}
                override fun onMoveEnd(detector: org.maplibre.android.gestures.MoveGestureDetector) {}
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

    // -- Menu --

    private fun showMenu(anchor: View) {
        activeMenu?.dismiss()

        val hasTrack = activeGpx != null
        val items = listOf(
            MenuItem(getString(R.string.menu_open_gpx)) { openGpxPicker() },
            MenuItem(getString(R.string.menu_save_gpx), enabled = hasTrack) { saveGpxPicker() },
            MenuItem(getString(R.string.menu_find_fuel), enabled = hasTrack) { findPoisOnRoute(PoiCategory.FUEL) },
            MenuItem(getString(R.string.menu_find_food), enabled = hasTrack) { findPoisOnRoute(PoiCategory.FOOD) },
            MenuItem(getString(R.string.menu_find_lodging), enabled = hasTrack) { findPoisOnRoute(PoiCategory.LODGING) },
            MenuItem(getString(R.string.menu_save_location)) { showSaveLocationDialog() },
            MenuItem(getString(R.string.menu_favorites)) { showFavoritesList() },
            MenuItem(getString(R.string.menu_import_pois)) { openPoiImportPicker() },
            MenuItem(getString(R.string.menu_clear_track), enabled = hasTrack) { clearTrack() }
        )

        val menu = Menu(this)
        activeMenu = menu
        menu.show(
            parent = mapView.parent as FrameLayout,
            anchor = anchor,
            expandH = ExpandH.RIGHT,
            expandV = ExpandV.DOWN,
            title = getString(R.string.app_name),
            items = items,
            onDismiss = { activeMenu = null }
        )
    }

    private fun showContextMenu(x: Float, y: Float) {
        activeMenu?.dismiss()

        val m = map ?: return
        val latLng = m.projection.fromScreenLocation(
            android.graphics.PointF(x, y)
        )
        val coordStr = String.format(Locale.ROOT, "%.5f, %.5f", latLng.latitude, latLng.longitude)

        val items = listOf(
            MenuItem("Navigate") {
                Toast.makeText(this, "Navigate to $coordStr", Toast.LENGTH_SHORT).show()
            },
            MenuItem("Place drag line") {
                Toast.makeText(this, "Drag line at $coordStr", Toast.LENGTH_SHORT).show()
            }
        )

        val menu = Menu(this)
        activeMenu = menu
        menu.showAt(
            parent = mapView.parent as FrameLayout,
            x = x,
            y = y,
            expandH = ExpandH.RIGHT,
            expandV = ExpandV.DOWN,
            title = "Navigate",
            items = items,
            headerActions = listOf(
                MenuItem("Copy", icon = getDrawable(android.R.drawable.ic_menu_edit)) {
                    val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("coordinates", coordStr))
                    Toast.makeText(this, "Copied $coordStr", Toast.LENGTH_SHORT).show()
                }
            ),
            onDismiss = { activeMenu = null }
        )
    }

    private fun showSaveLocationDialog() {
        val loc = lastLocation
        if (loc == null) {
            Toast.makeText(this, R.string.no_location, Toast.LENGTH_SHORT).show()
            return
        }
        val input = EditText(this).apply {
            hint = getString(R.string.save_location_hint)
            setPadding(48, 32, 48, 32)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.save_location_title)
            .setView(input)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = input.text.toString().ifBlank {
                    String.format(Locale.ROOT, "%.4f, %.4f", loc.latitude, loc.longitude)
                }
                favoritesDb.saveLocation(FavoriteLocation(
                    name = name, lat = loc.latitude, lon = loc.longitude
                ))
                Toast.makeText(this, R.string.location_saved, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showFavoritesList() {
        val locations = favoritesDb.allLocations()
        if (locations.isEmpty()) {
            Toast.makeText(this, "No favorites saved", Toast.LENGTH_SHORT).show()
            return
        }
        val names = locations.map { it.name }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(R.string.menu_favorites)
            .setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, names)) { _, which ->
                val fav = locations[which]
                map?.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(LatLng(fav.lat, fav.lon), 15.0)
                )
                cameraLocked = false
                lastUserInteraction = System.currentTimeMillis()
                updateToggleAppearance()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun findPoisOnRoute(category: PoiCategory) {
        val gpx = activeGpx
        if (gpx == null) {
            Toast.makeText(this, R.string.no_track_loaded, Toast.LENGTH_SHORT).show()
            return
        }
        if (poiDb.count() == 0L) {
            Toast.makeText(this, R.string.no_pois_data, Toast.LENGTH_SHORT).show()
            return
        }
        val results = PoiSearch(poiDb).findAlongRoute(gpx, category)
        if (results.isEmpty()) {
            Toast.makeText(this,
                getString(R.string.no_pois_found, category.label), Toast.LENGTH_SHORT).show()
            poiOverlay?.clear()
            return
        }
        poiOverlay?.show(results.map { it.poi })
        Toast.makeText(this,
            getString(R.string.pois_found, results.size, category.label), Toast.LENGTH_SHORT).show()
    }

    private fun openPoiImportPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
        startActivityForResult(intent, REQ_IMPORT_POI)
    }

    private fun importPois(uri: Uri) {
        try {
            val result = contentResolver.openInputStream(uri)?.use {
                PoiImporter.importJson(it, poiDb)
            } ?: return
            Toast.makeText(this,
                getString(R.string.pois_imported, result.imported, result.skipped),
                Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "POI import failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearTrack() {
        trackOverlay?.clear()
        poiOverlay?.clear()
        activeGpx = null
        trackFollower = null
        navRow.visibility = View.GONE
        offTrackBanner.visibility = View.GONE
        distanceText.text = "-- km"
        Toast.makeText(this, R.string.track_cleared, Toast.LENGTH_SHORT).show()
    }

    // -- GPX --

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

    private fun openGpxPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/gpx+xml", "application/xml", "text/xml"))
        }
        startActivityForResult(intent, REQ_OPEN_GPX)
    }

    private fun saveGpxPicker() {
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
            REQ_IMPORT_POI -> importPois(uri)
        }
    }

    // -- Location --

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
        lastLocation = location
        val kmh = (location.speed * 3.6).toInt()
        speedText.text = getString(R.string.speed_format, kmh)
        updateNavigation(location, kmh)
        updateCamera(location)
        updateCrosshair(location)
    }

    private fun updateCrosshair(location: Location) {
        val m = map ?: return
        val puck = m.projection.toScreenLocation(
            LatLng(location.latitude, location.longitude)
        )
        val cx = crosshair.width / 2f
        val cy = crosshair.height / 2f
        val dx = puck.x - cx
        val dy = puck.y - cy
        val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        crosshair.setPuckDistance(dist)
    }

    private fun updateNavigation(location: Location, kmh: Int) {
        val follower = trackFollower ?: return
        val state = follower.update(location.latitude, location.longitude)

        offTrackBanner.visibility = if (state.offTrack) View.VISIBLE else View.GONE

        val remainM = state.distanceRemainingM
        remainText.text = if (remainM >= 1000) {
            getString(R.string.remain_km_format, remainM / 1000.0)
        } else {
            getString(R.string.remain_m_format, remainM.toInt())
        }

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

        val coveredKm = state.distanceCoveredM / 1000.0
        distanceText.text = String.format(Locale.ROOT, "%.1f km", coveredKm)
    }

    private fun updateCamera(location: Location) {
        val m = map ?: return
        val kmh = (location.speed * 3.6).toInt()

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

        if (kmh > 5 && location.hasBearing()) {
            pos.bearing(location.bearing.toDouble())
        }

        val durationMs = if (kmh > 60) 300 else 600
        m.animateCamera(CameraUpdateFactory.newCameraPosition(pos.build()), durationMs)
    }

    private fun updateToggleAppearance() {
        cameraToggle.alpha = if (cameraLocked) 1.0f else 0.5f
    }

    // -- Lifecycle --

    override fun onStart() { super.onStart(); mapView.onStart() }
    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { super.onPause(); mapView.onPause() }
    override fun onStop() {
        super.onStop()
        mapView.onStop()
        locationManager?.removeUpdates(this)
    }
    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
        favoritesDb.close()
        poiDb.close()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
    override fun onLowMemory() { super.onLowMemory(); mapView.onLowMemory() }

    companion object {
        private const val REQ_LOCATION = 1
        private const val REQ_OPEN_GPX = 2
        private const val REQ_SAVE_GPX = 3
        private const val REQ_IMPORT_POI = 4
        private const val RELOCK_RIDING_MS = 5_000L
        private const val RELOCK_IDLE_MS = 15_000L
    }
}
