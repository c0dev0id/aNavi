package dev.anavi.nav

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Geodesic utilities. */
object Geo {

    private const val EARTH_RADIUS_M = 6_371_000.0

    /** Haversine distance in meters between two WGS84 points. */
    fun distanceM(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        return EARTH_RADIUS_M * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    /** Sum of segment distances along a list of lat/lon points, in meters. */
    fun polylineDistanceM(points: List<Pair<Double, Double>>): Double {
        var total = 0.0
        for (i in 1 until points.size) {
            total += distanceM(
                points[i - 1].first, points[i - 1].second,
                points[i].first, points[i].second,
            )
        }
        return total
    }
}
