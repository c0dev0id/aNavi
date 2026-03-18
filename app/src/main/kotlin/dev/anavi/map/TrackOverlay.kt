package dev.anavi.map

import android.graphics.Color
import dev.anavi.gpx.GpxData
import dev.anavi.gpx.GpxPoint
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource

/**
 * Renders GPX tracks and routes as polylines on a MapLibre map.
 */
class TrackOverlay(private val map: MapLibreMap) {

    private var sourceAdded = false

    fun show(gpx: GpxData) {
        val points = gpx.allTrackPoints()
        if (points.isEmpty()) return

        val geoJson = toGeoJson(points)
        val style = map.style ?: return

        if (sourceAdded) {
            style.getSourceAs<GeoJsonSource>(SOURCE_ID)?.setGeoJson(geoJson)
        } else {
            style.addSource(GeoJsonSource(SOURCE_ID, geoJson))
            style.addLayer(
                LineLayer(LAYER_ID, SOURCE_ID).withProperties(
                    PropertyFactory.lineColor(Color.parseColor(TRACK_COLOR)),
                    PropertyFactory.lineWidth(4f),
                    PropertyFactory.lineOpacity(0.85f),
                )
            )
            sourceAdded = true
        }
    }

    fun clear() {
        val style = map.style ?: return
        if (sourceAdded) {
            style.removeLayer(LAYER_ID)
            style.removeSource(SOURCE_ID)
            sourceAdded = false
        }
    }

    fun bounds(gpx: GpxData): LatLngBounds? {
        val points = gpx.allTrackPoints()
        if (points.size < 2) return null
        val builder = LatLngBounds.Builder()
        for (pt in points) {
            builder.include(LatLng(pt.lat, pt.lon))
        }
        return builder.build()
    }

    private fun toGeoJson(points: List<GpxPoint>): String {
        val coords = points.joinToString(",") { "[${it.lon},${it.lat}]" }
        return """{"type":"Feature","geometry":{"type":"LineString","coordinates":[$coords]}}"""
    }

    companion object {
        private const val SOURCE_ID = "gpx-track"
        private const val LAYER_ID = "gpx-track-line"
        private const val TRACK_COLOR = "#FF4444"
    }
}
