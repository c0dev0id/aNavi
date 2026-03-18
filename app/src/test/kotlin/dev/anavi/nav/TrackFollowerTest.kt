package dev.anavi.nav

import dev.anavi.gpx.GpxData
import dev.anavi.gpx.GpxPoint
import dev.anavi.gpx.GpxTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackFollowerTest {

    private fun trackOf(vararg pts: Pair<Double, Double>): GpxData {
        val points = pts.map { GpxPoint(it.first, it.second) }
        return GpxData(
            waypoints = emptyList(),
            tracks = listOf(GpxTrack("test", listOf(points))),
            routes = emptyList(),
        )
    }

    @Test
    fun nearTrackIsNotOffTrack() {
        // Simple north-south track
        val gpx = trackOf(48.200 to 16.300, 48.210 to 16.300, 48.220 to 16.300)
        val follower = TrackFollower(gpx)

        // Position right on the track midpoint
        val state = follower.update(48.210, 16.300)
        assertFalse(state.offTrack)
        assertTrue(state.distanceToTrackM < 1.0)
    }

    @Test
    fun farFromTrackIsOffTrack() {
        val gpx = trackOf(48.200 to 16.300, 48.210 to 16.300)
        val follower = TrackFollower(gpx)

        // Position ~1 km east of the track
        val state = follower.update(48.205, 16.315)
        assertTrue(state.offTrack)
    }

    @Test
    fun progressIncreasesAlongTrack() {
        val gpx = trackOf(
            48.200 to 16.300,
            48.205 to 16.300,
            48.210 to 16.300,
            48.215 to 16.300,
            48.220 to 16.300,
        )
        val follower = TrackFollower(gpx)

        val start = follower.update(48.200, 16.300)
        val mid = follower.update(48.210, 16.300)
        val end = follower.update(48.220, 16.300)

        assertTrue(start.progress < mid.progress)
        assertTrue(mid.progress < end.progress)
        assertTrue(start.distanceRemainingM > mid.distanceRemainingM)
        assertTrue(mid.distanceRemainingM > end.distanceRemainingM)
    }

    @Test
    fun totalDistanceIsReasonable() {
        // ~2.22 km north-south at 16.3°E
        val gpx = trackOf(48.200 to 16.300, 48.220 to 16.300)
        val follower = TrackFollower(gpx)

        // 0.02° latitude ≈ 2224 m
        assertEquals(2224.0, follower.totalDistanceM, 50.0)
    }
}
