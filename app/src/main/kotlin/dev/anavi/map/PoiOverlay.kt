package dev.anavi.map

import android.graphics.Color
import dev.anavi.poi.Poi
import dev.anavi.poi.PoiCategory
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource

/**
 * Renders POI markers on the map as colored circles with labels.
 */
class PoiOverlay(private val map: MapLibreMap) {

    private var sourceAdded = false

    fun show(pois: List<Poi>) {
        if (pois.isEmpty()) {
            clear()
            return
        }

        val geoJson = toGeoJson(pois)
        val style = map.style ?: return

        if (sourceAdded) {
            style.getSourceAs<GeoJsonSource>(SOURCE_ID)?.setGeoJson(geoJson)
        } else {
            style.addSource(GeoJsonSource(SOURCE_ID, geoJson))
            style.addLayer(
                CircleLayer(CIRCLE_LAYER_ID, SOURCE_ID).withProperties(
                    PropertyFactory.circleRadius(8f),
                    PropertyFactory.circleColor(
                        Expression.match(
                            Expression.get("category"),
                            Expression.color(Color.parseColor("#9E9E9E")),
                            Expression.stop("FUEL", Expression.color(Color.parseColor("#FF9800"))),
                            Expression.stop("FOOD", Expression.color(Color.parseColor("#4CAF50"))),
                            Expression.stop("LODGING", Expression.color(Color.parseColor("#2196F3"))),
                        )
                    ),
                    PropertyFactory.circleStrokeWidth(2f),
                    PropertyFactory.circleStrokeColor(Color.WHITE),
                )
            )
            style.addLayer(
                SymbolLayer(LABEL_LAYER_ID, SOURCE_ID).withProperties(
                    PropertyFactory.textField(Expression.get("name")),
                    PropertyFactory.textSize(11f),
                    PropertyFactory.textOffset(arrayOf(0f, 1.5f)),
                    PropertyFactory.textColor(Color.WHITE),
                    PropertyFactory.textHaloColor(Color.BLACK),
                    PropertyFactory.textHaloWidth(1f),
                    PropertyFactory.textMaxWidth(8f),
                )
            )
            sourceAdded = true
        }
    }

    fun clear() {
        val style = map.style ?: return
        if (sourceAdded) {
            style.removeLayer(LABEL_LAYER_ID)
            style.removeLayer(CIRCLE_LAYER_ID)
            style.removeSource(SOURCE_ID)
            sourceAdded = false
        }
    }

    private fun toGeoJson(pois: List<Poi>): String {
        val features = pois.joinToString(",") { poi ->
            """{"type":"Feature","geometry":{"type":"Point","coordinates":[${poi.lon},${poi.lat}]},"properties":{"name":"${escapeJson(poi.name)}","category":"${poi.category.name}"}}"""
        }
        return """{"type":"FeatureCollection","features":[$features]}"""
    }

    private fun escapeJson(s: String): String = s
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")

    companion object {
        private const val SOURCE_ID = "poi-source"
        private const val CIRCLE_LAYER_ID = "poi-circles"
        private const val LABEL_LAYER_ID = "poi-labels"
    }
}
