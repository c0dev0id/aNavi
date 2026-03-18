package dev.anavi.poi

data class Poi(
    val id: Long = 0,
    val name: String,
    val category: PoiCategory,
    val lat: Double,
    val lon: Double,
)
