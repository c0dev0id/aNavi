package dev.anavi.poi

import org.json.JSONArray
import java.io.InputStream

/**
 * Imports POI data from a JSON array.
 *
 * Expected format:
 * ```json
 * [
 *   {"name": "Shell Station", "category": "FUEL", "lat": 48.2, "lon": 16.3},
 *   ...
 * ]
 * ```
 *
 * Category values: FUEL, FOOD, LODGING (case-insensitive).
 */
object PoiImporter {

    data class ImportResult(val imported: Int, val skipped: Int)

    fun importJson(input: InputStream, db: PoiDb): ImportResult {
        val json = input.bufferedReader().readText()
        val array = JSONArray(json)
        val pois = mutableListOf<Poi>()
        var skipped = 0

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val category = PoiCategory.fromString(obj.optString("category", ""))
            if (category == null) {
                skipped++
                continue
            }
            val name = obj.optString("name", "").ifBlank {
                "${category.label} #${i + 1}"
            }
            pois.add(Poi(
                name = name,
                category = category,
                lat = obj.getDouble("lat"),
                lon = obj.getDouble("lon"),
            ))
        }

        db.insertAll(pois)
        return ImportResult(pois.size, skipped)
    }
}
