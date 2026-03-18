package dev.anavi.gpx

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream

/**
 * Streaming GPX parser using XmlPullParser (built into Android).
 * Handles GPX 1.0 and 1.1 track, route, and waypoint elements.
 */
object GpxParser {

    fun parse(input: InputStream): GpxData {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val xpp = factory.newPullParser()
        xpp.setInput(input, null)

        val waypoints = mutableListOf<GpxPoint>()
        val tracks = mutableListOf<GpxTrack>()
        val routes = mutableListOf<GpxRoute>()

        var event = xpp.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                when (xpp.name) {
                    "wpt" -> waypoints.add(parsePoint(xpp, "wpt"))
                    "trk" -> tracks.add(parseTrack(xpp))
                    "rte" -> routes.add(parseRoute(xpp))
                }
            }
            event = xpp.next()
        }

        return GpxData(waypoints, tracks, routes)
    }

    private fun parseTrack(xpp: XmlPullParser): GpxTrack {
        var name: String? = null
        val segments = mutableListOf<List<GpxPoint>>()

        var depth = 1
        while (depth > 0) {
            when (xpp.next()) {
                XmlPullParser.START_TAG -> {
                    depth++
                    when (xpp.name) {
                        "name" -> if (name == null) name = readText(xpp).also { depth-- }
                        "trkseg" -> segments.add(parseSegment(xpp)).also { depth-- }
                    }
                }
                XmlPullParser.END_TAG -> depth--
            }
        }

        return GpxTrack(name, segments)
    }

    private fun parseSegment(xpp: XmlPullParser): List<GpxPoint> {
        val points = mutableListOf<GpxPoint>()
        var depth = 1
        while (depth > 0) {
            when (xpp.next()) {
                XmlPullParser.START_TAG -> {
                    depth++
                    if (xpp.name == "trkpt") {
                        points.add(parsePoint(xpp, "trkpt"))
                        depth--
                    }
                }
                XmlPullParser.END_TAG -> depth--
            }
        }
        return points
    }

    private fun parseRoute(xpp: XmlPullParser): GpxRoute {
        var name: String? = null
        val points = mutableListOf<GpxPoint>()

        var depth = 1
        while (depth > 0) {
            when (xpp.next()) {
                XmlPullParser.START_TAG -> {
                    depth++
                    when (xpp.name) {
                        "name" -> if (name == null) name = readText(xpp).also { depth-- }
                        "rtept" -> points.add(parsePoint(xpp, "rtept")).also { depth-- }
                    }
                }
                XmlPullParser.END_TAG -> depth--
            }
        }

        return GpxRoute(name, points)
    }

    private fun parsePoint(xpp: XmlPullParser, endTag: String): GpxPoint {
        val lat = xpp.getAttributeValue(null, "lat")?.toDoubleOrNull() ?: 0.0
        val lon = xpp.getAttributeValue(null, "lon")?.toDoubleOrNull() ?: 0.0
        var ele: Double? = null
        var name: String? = null

        var depth = 1
        while (depth > 0) {
            when (xpp.next()) {
                XmlPullParser.START_TAG -> {
                    depth++
                    when (xpp.name) {
                        "ele" -> ele = readText(xpp).toDoubleOrNull().also { depth-- }
                        "name" -> name = readText(xpp).also { depth-- }
                    }
                }
                XmlPullParser.END_TAG -> depth--
            }
        }

        return GpxPoint(lat, lon, ele, name)
    }

    private fun readText(xpp: XmlPullParser): String {
        var result = ""
        if (xpp.next() == XmlPullParser.TEXT) {
            result = xpp.text
            xpp.next() // consume END_TAG
        }
        return result
    }
}
