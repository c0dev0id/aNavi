package dev.anavi

import android.app.Application
import org.maplibre.android.MapLibre

class ANaviApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(this)
    }
}
