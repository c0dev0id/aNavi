package dev.anavi.nav

import dev.anavi.gpx.GpxData
import dev.anavi.gpx.GpxPoint

/**
 * Follows a GPX track: finds the rider's position on the track,
 * calculates remaining distance, and detects off-track conditions.
 */
class TrackFollower(gpx: GpxData) {

    /** Flattened track points in order. */
    private val points: List<GpxPoint> = gpx.allTrackPoints()

    /** Cumulative distance from track start to each point, in meters. */
    private val cumDist: DoubleArray

    /** Total track length in meters. */
    val totalDistanceM: Double

    /** Index of the nearest track point from the last update. */
    private var nearestIdx = 0

    init {
        cumDist = DoubleArray(points.size)
        for (i in 1 until points.size) {
            cumDist[i] = cumDist[i - 1] + Geo.distanceM(
                points[i - 1].lat, points[i - 1].lon,
                points[i].lat, points[i].lon,
            )
        }
        totalDistanceM = if (cumDist.isNotEmpty()) cumDist.last() else 0.0
    }

    /**
     * Update with the rider's current position.
     * Returns a [FollowState] with distance info and off-track status.
     *
     * Uses a sliding search window around the last known nearest index
     * to avoid O(n) full scan every GPS tick.
     */
    fun update(lat: Double, lon: Double): FollowState {
        if (points.isEmpty()) return FollowState.EMPTY

        // Search within a window around last known position
        // Window expands if rider is moving fast or skipped ahead
        val windowSize = SEARCH_WINDOW.coerceAtMost(points.size)
        val searchStart = (nearestIdx - windowSize / 2).coerceAtLeast(0)
        val searchEnd = (nearestIdx + windowSize / 2).coerceAtMost(points.size - 1)

        var bestDist = Double.MAX_VALUE
        var bestIdx = nearestIdx

        for (i in searchStart..searchEnd) {
            val d = Geo.distanceM(lat, lon, points[i].lat, points[i].lon)
            if (d < bestDist) {
                bestDist = d
                bestIdx = i
            }
        }

        // If the best is at the edge of our window, do a full scan
        // (rider may have jumped ahead, e.g. after a detour)
        if (bestIdx == searchStart || bestIdx == searchEnd) {
            for (i in points.indices) {
                val d = Geo.distanceM(lat, lon, points[i].lat, points[i].lon)
                if (d < bestDist) {
                    bestDist = d
                    bestIdx = i
                }
            }
        }

        nearestIdx = bestIdx

        val distanceCoveredM = cumDist[bestIdx]
        val distanceRemainingM = totalDistanceM - distanceCoveredM
        val offTrack = bestDist > OFF_TRACK_THRESHOLD_M

        return FollowState(
            nearestPointIdx = bestIdx,
            distanceToTrackM = bestDist,
            distanceCoveredM = distanceCoveredM,
            distanceRemainingM = distanceRemainingM,
            progress = if (totalDistanceM > 0) distanceCoveredM / totalDistanceM else 0.0,
            offTrack = offTrack,
        )
    }

    companion object {
        /** Search window around last known nearest index. */
        private const val SEARCH_WINDOW = 100

        /** Distance threshold in meters to consider rider off-track. */
        const val OFF_TRACK_THRESHOLD_M = 50.0
    }
}

data class FollowState(
    val nearestPointIdx: Int,
    val distanceToTrackM: Double,
    val distanceCoveredM: Double,
    val distanceRemainingM: Double,
    val progress: Double,
    val offTrack: Boolean,
) {
    companion object {
        val EMPTY = FollowState(0, 0.0, 0.0, 0.0, 0.0, false)
    }
}
