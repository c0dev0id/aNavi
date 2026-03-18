package dev.anavi.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class FavoriteLocation(
    val id: Long = 0,
    val name: String,
    val lat: Double,
    val lon: Double,
    val createdAt: Long = System.currentTimeMillis(),
)

data class FavoriteRide(
    val id: Long = 0,
    val name: String,
    val gpxUri: String,
    val createdAt: Long = System.currentTimeMillis(),
)

class FavoritesDb(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE locations (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                lat REAL NOT NULL,
                lon REAL NOT NULL,
                created_at INTEGER NOT NULL
            )
        """)
        db.execSQL("""
            CREATE TABLE rides (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                gpx_uri TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Future migrations go here
    }

    fun saveLocation(fav: FavoriteLocation): Long {
        val cv = ContentValues().apply {
            put("name", fav.name)
            put("lat", fav.lat)
            put("lon", fav.lon)
            put("created_at", fav.createdAt)
        }
        return writableDatabase.insert("locations", null, cv)
    }

    fun allLocations(): List<FavoriteLocation> {
        val list = mutableListOf<FavoriteLocation>()
        readableDatabase.rawQuery(
            "SELECT id, name, lat, lon, created_at FROM locations ORDER BY created_at DESC", null
        ).use { c ->
            while (c.moveToNext()) {
                list.add(FavoriteLocation(
                    id = c.getLong(0),
                    name = c.getString(1),
                    lat = c.getDouble(2),
                    lon = c.getDouble(3),
                    createdAt = c.getLong(4),
                ))
            }
        }
        return list
    }

    fun deleteLocation(id: Long) {
        writableDatabase.delete("locations", "id = ?", arrayOf(id.toString()))
    }

    fun saveRide(ride: FavoriteRide): Long {
        val cv = ContentValues().apply {
            put("name", ride.name)
            put("gpx_uri", ride.gpxUri)
            put("created_at", ride.createdAt)
        }
        return writableDatabase.insert("rides", null, cv)
    }

    fun allRides(): List<FavoriteRide> {
        val list = mutableListOf<FavoriteRide>()
        readableDatabase.rawQuery(
            "SELECT id, name, gpx_uri, created_at FROM rides ORDER BY created_at DESC", null
        ).use { c ->
            while (c.moveToNext()) {
                list.add(FavoriteRide(
                    id = c.getLong(0),
                    name = c.getString(1),
                    gpxUri = c.getString(2),
                    createdAt = c.getLong(3),
                ))
            }
        }
        return list
    }

    fun deleteRide(id: Long) {
        writableDatabase.delete("rides", "id = ?", arrayOf(id.toString()))
    }

    companion object {
        private const val DB_NAME = "favorites.db"
        private const val DB_VERSION = 1
    }
}
