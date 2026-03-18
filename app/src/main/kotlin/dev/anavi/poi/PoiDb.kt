package dev.anavi.poi

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * SQLite database for POI storage with FTS5 full-text search.
 *
 * Two tables:
 * - `pois` — main table with lat/lon (indexed for bounding-box queries)
 * - `poi_fts` — FTS5 virtual table for name/category text search
 */
class PoiDb(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE pois (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                category TEXT NOT NULL,
                lat REAL NOT NULL,
                lon REAL NOT NULL
            )
        """)
        db.execSQL("CREATE INDEX idx_pois_lat ON pois(lat)")
        db.execSQL("CREATE INDEX idx_pois_lon ON pois(lon)")
        db.execSQL("""
            CREATE VIRTUAL TABLE poi_fts USING fts5(
                name, category, content='pois', content_rowid='id'
            )
        """)
        // Triggers to keep FTS in sync with main table
        db.execSQL("""
            CREATE TRIGGER pois_ai AFTER INSERT ON pois BEGIN
                INSERT INTO poi_fts(rowid, name, category)
                VALUES (new.id, new.name, new.category);
            END
        """)
        db.execSQL("""
            CREATE TRIGGER pois_ad AFTER DELETE ON pois BEGIN
                INSERT INTO poi_fts(poi_fts, rowid, name, category)
                VALUES ('delete', old.id, old.name, old.category);
            END
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    /** Insert a single POI. Returns the row ID. */
    fun insert(poi: Poi): Long {
        val cv = ContentValues().apply {
            put("name", poi.name)
            put("category", poi.category.name)
            put("lat", poi.lat)
            put("lon", poi.lon)
        }
        return writableDatabase.insert("pois", null, cv)
    }

    /** Bulk insert POIs in a single transaction. */
    fun insertAll(pois: List<Poi>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val stmt = db.compileStatement(
                "INSERT INTO pois (name, category, lat, lon) VALUES (?, ?, ?, ?)"
            )
            for (poi in pois) {
                stmt.bindString(1, poi.name)
                stmt.bindString(2, poi.category.name)
                stmt.bindDouble(3, poi.lat)
                stmt.bindDouble(4, poi.lon)
                stmt.executeInsert()
                stmt.clearBindings()
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Find POIs within a lat/lon bounding box, optionally filtered by category.
     * This is the spatial pre-filter; callers should post-filter by actual distance.
     */
    fun queryBounds(
        minLat: Double, maxLat: Double,
        minLon: Double, maxLon: Double,
        category: PoiCategory? = null,
    ): List<Poi> {
        val where = StringBuilder("lat BETWEEN ? AND ? AND lon BETWEEN ? AND ?")
        val args = mutableListOf(
            minLat.toString(), maxLat.toString(),
            minLon.toString(), maxLon.toString(),
        )
        if (category != null) {
            where.append(" AND category = ?")
            args.add(category.name)
        }
        return query(where.toString(), args.toTypedArray())
    }

    /** Full-text search by name prefix. */
    fun searchByName(prefix: String, limit: Int = 50): List<Poi> {
        val escaped = prefix.replace("\"", "\"\"")
        val ftsQuery = "\"$escaped\"*"
        val sql = """
            SELECT p.id, p.name, p.category, p.lat, p.lon
            FROM pois p
            JOIN poi_fts f ON p.id = f.rowid
            WHERE poi_fts MATCH ?
            LIMIT ?
        """
        return queryRaw(sql, arrayOf(ftsQuery, limit.toString()))
    }

    /** Number of POIs in the database. */
    fun count(): Long {
        val cursor = readableDatabase.rawQuery("SELECT COUNT(*) FROM pois", null)
        cursor.use {
            return if (it.moveToFirst()) it.getLong(0) else 0
        }
    }

    /** Delete all POIs. */
    fun clear() {
        writableDatabase.execSQL("DELETE FROM pois")
    }

    private fun query(where: String, args: Array<String>): List<Poi> {
        val sql = "SELECT id, name, category, lat, lon FROM pois WHERE $where"
        return queryRaw(sql, args)
    }

    private fun queryRaw(sql: String, args: Array<String>): List<Poi> {
        val list = mutableListOf<Poi>()
        readableDatabase.rawQuery(sql, args).use { c ->
            while (c.moveToNext()) {
                val cat = PoiCategory.fromString(c.getString(2)) ?: continue
                list.add(Poi(
                    id = c.getLong(0),
                    name = c.getString(1),
                    category = cat,
                    lat = c.getDouble(3),
                    lon = c.getDouble(4),
                ))
            }
        }
        return list
    }

    companion object {
        private const val DB_NAME = "pois.db"
        private const val DB_VERSION = 1
    }
}
