package dev.anavi.poi

enum class PoiCategory(val label: String, val osmTags: List<String>) {
    FUEL("Fuel", listOf("amenity=fuel")),
    FOOD("Food", listOf("amenity=restaurant", "amenity=fast_food", "amenity=cafe")),
    LODGING("Lodging", listOf("tourism=hotel", "tourism=motel", "tourism=guest_house")),
    ;

    companion object {
        fun fromString(s: String): PoiCategory? =
            entries.firstOrNull { it.name.equals(s, ignoreCase = true) }
    }
}
