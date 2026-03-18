package dev.anavi.gpx

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream

class GpxParserTest {

    @Test
    fun parseTrackWithSegment() {
        val gpx = """
            <?xml version="1.0"?>
            <gpx version="1.1">
              <trk>
                <name>Test Track</name>
                <trkseg>
                  <trkpt lat="48.2" lon="16.3"><ele>200</ele></trkpt>
                  <trkpt lat="48.3" lon="16.4"><ele>210</ele></trkpt>
                </trkseg>
              </trk>
            </gpx>
        """.trimIndent()

        val data = GpxParser.parse(ByteArrayInputStream(gpx.toByteArray()))

        assertEquals(1, data.tracks.size)
        assertEquals("Test Track", data.tracks[0].name)
        assertEquals(1, data.tracks[0].segments.size)
        assertEquals(2, data.tracks[0].segments[0].size)

        val pt = data.tracks[0].segments[0][0]
        assertEquals(48.2, pt.lat, 0.001)
        assertEquals(16.3, pt.lon, 0.001)
        assertEquals(200.0, pt.ele!!, 0.001)
    }

    @Test
    fun parseRoute() {
        val gpx = """
            <?xml version="1.0"?>
            <gpx version="1.1">
              <rte>
                <name>Test Route</name>
                <rtept lat="47.0" lon="15.0"></rtept>
                <rtept lat="47.1" lon="15.1"></rtept>
              </rte>
            </gpx>
        """.trimIndent()

        val data = GpxParser.parse(ByteArrayInputStream(gpx.toByteArray()))

        assertEquals(1, data.routes.size)
        assertEquals("Test Route", data.routes[0].name)
        assertEquals(2, data.routes[0].points.size)
    }

    @Test
    fun parseWaypoints() {
        val gpx = """
            <?xml version="1.0"?>
            <gpx version="1.1">
              <wpt lat="48.2" lon="16.3">
                <name>Gas Station</name>
              </wpt>
            </gpx>
        """.trimIndent()

        val data = GpxParser.parse(ByteArrayInputStream(gpx.toByteArray()))

        assertEquals(1, data.waypoints.size)
        assertEquals("Gas Station", data.waypoints[0].name)
    }

    @Test
    fun roundTrip() {
        val original = GpxData(
            waypoints = listOf(GpxPoint(48.2, 16.3, 200.0, "WP1")),
            tracks = listOf(
                GpxTrack("Track1", listOf(
                    listOf(GpxPoint(48.0, 16.0), GpxPoint(48.1, 16.1))
                ))
            ),
            routes = emptyList(),
        )

        val out = java.io.ByteArrayOutputStream()
        GpxWriter.write(original, out)
        val parsed = GpxParser.parse(ByteArrayInputStream(out.toByteArray()))

        assertEquals(original.waypoints.size, parsed.waypoints.size)
        assertEquals(original.waypoints[0].name, parsed.waypoints[0].name)
        assertEquals(original.tracks.size, parsed.tracks.size)
        assertEquals(original.tracks[0].name, parsed.tracks[0].name)
        assertEquals(
            original.tracks[0].segments[0].size,
            parsed.tracks[0].segments[0].size
        )
    }
}
