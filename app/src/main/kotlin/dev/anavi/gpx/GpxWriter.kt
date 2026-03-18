package dev.anavi.gpx

import java.io.OutputStream

/** Writes GPX 1.1 XML. */
object GpxWriter {

    fun write(data: GpxData, out: OutputStream) {
        out.writer(Charsets.UTF_8).use { w ->
            w.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            w.append("<gpx version=\"1.1\" creator=\"aNavi\"")
            w.append(" xmlns=\"http://www.topografix.com/GPX/1/1\">\n")

            for (wpt in data.waypoints) {
                writePoint(w, "wpt", wpt, indent = "  ")
            }

            for (rte in data.routes) {
                w.append("  <rte>\n")
                rte.name?.let { w.append("    <name>${escape(it)}</name>\n") }
                for (pt in rte.points) {
                    writePoint(w, "rtept", pt, indent = "    ")
                }
                w.append("  </rte>\n")
            }

            for (trk in data.tracks) {
                w.append("  <trk>\n")
                trk.name?.let { w.append("    <name>${escape(it)}</name>\n") }
                for (seg in trk.segments) {
                    w.append("    <trkseg>\n")
                    for (pt in seg) {
                        writePoint(w, "trkpt", pt, indent = "      ")
                    }
                    w.append("    </trkseg>\n")
                }
                w.append("  </trk>\n")
            }

            w.append("</gpx>\n")
        }
    }

    private fun writePoint(w: Appendable, tag: String, pt: GpxPoint, indent: String) {
        w.append("$indent<$tag lat=\"${pt.lat}\" lon=\"${pt.lon}\">")
        val hasChildren = pt.ele != null || pt.name != null
        if (hasChildren) {
            w.append("\n")
            pt.ele?.let { w.append("$indent  <ele>$it</ele>\n") }
            pt.name?.let { w.append("$indent  <name>${escape(it)}</name>\n") }
            w.append("$indent</$tag>\n")
        } else {
            w.append("</$tag>\n")
        }
    }

    private fun escape(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
}
