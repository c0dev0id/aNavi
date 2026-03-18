package dev.anavi.gpx

data class GpxPoint(
    val lat: Double,
    val lon: Double,
    val ele: Double? = null,
    val name: String? = null,
)

data class GpxTrack(
    val name: String?,
    val segments: List<List<GpxPoint>>,
)

data class GpxRoute(
    val name: String?,
    val points: List<GpxPoint>,
)

data class GpxData(
    val waypoints: List<GpxPoint>,
    val tracks: List<GpxTrack>,
    val routes: List<GpxRoute>,
) {
    /** All points from all tracks and routes, flattened in order. */
    fun allTrackPoints(): List<GpxPoint> =
        tracks.flatMap { it.segments.flatten() } + routes.flatMap { it.points }
}
