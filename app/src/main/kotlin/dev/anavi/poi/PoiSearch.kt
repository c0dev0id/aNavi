package dev.anavi.poi

import dev.anavi.gpx.GpxData
import dev.anavi.gpx.GpxPoint
import dev.anavi.nav.Geo

/**
 * Finds POIs within a corridor along a GPX track.
 *
 * Strategy:
 * 1. Divide track into segments, compute bounding box per segment + buffer
 * 2. Query POI database with merged bounding box
 * 3. Post-filter: keep only POIs within [corridorM] meters of any track point
 */
class PoiSearch(private val db: PoiDb) {

    /**
     * Find POIs of [category] within [corridorM] meters of the track.
     * Returns results sorted by distance along the track (earliest first).
     */
    fun findAlongRoute(
        gpx: GpxData,
        category: PoiCategory,
        corridorM: Double = DEFAULT_CORRIDOR_M,
    ): List<PoiResult> {
        val points = gpx.allTrackPoints()
        if (points.isEmpty()) return emptyList()

        // Compute bounding box of entire track + buffer
        val bufferDeg = corridorM / 111_000.0 // rough meters-to-degrees
        var minLat = Double.MAX_VALUE
        var maxLat = -Double.MAX_VALUE
        var minLon = Double.MAX_VALUE
        var maxLon = -Double.MAX_VALUE
        for (pt in points) {
            if (pt.lat < minLat) minLat = pt.lat
            if (pt.lat > maxLat) maxLat = pt.lat
            if (pt.lon < minLon) minLon = pt.lon
            if (pt.lon > maxLon) maxLon = pt.lon
        }
        minLat -= bufferDeg
        maxLat += bufferDeg
        minLon -= bufferDeg
        maxLon += bufferDeg

        // Bounding-box query
        val candidates = db.queryBounds(minLat, maxLat, minLon, maxLon, category)

        // Post-filter by actual distance to track
        val results = mutableListOf<PoiResult>()
        for (poi in candidates) {
            val nearest = findNearestTrackInfo(poi, points)
            if (nearest.distanceM <= corridorM) {
                results.add(PoiResult(poi, nearest.distanceM, nearest.trackIndex))
            }
        }

        // Sort by position along the track
        results.sortBy { it.trackIndex }
        return results
    }

    private fun findNearestTrackInfo(poi: Poi, points: List<GpxPoint>): NearestInfo {
        var bestDist = Double.MAX_VALUE
        var bestIdx = 0
        for (i in points.indices) {
            val d = Geo.distanceM(poi.lat, poi.lon, points[i].lat, points[i].lon)
            if (d < bestDist) {
                bestDist = d
                bestIdx = i
            }
        }
        return NearestInfo(bestDist, bestIdx)
    }

    private data class NearestInfo(val distanceM: Double, val trackIndex: Int)

    companion object {
        /** Default corridor width: 2 km from the track. */
        const val DEFAULT_CORRIDOR_M = 2_000.0
    }
}

data class PoiResult(
    val poi: Poi,
    /** Distance from the POI to the nearest track point, in meters. */
    val distanceToTrackM: Double,
    /** Index of the nearest track point (for ordering along the route). */
    val trackIndex: Int,
)
