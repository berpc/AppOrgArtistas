package com.catedra.apporgartistas

import android.app.Application
import com.cloudinary.android.MediaManager

class MainApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val config = mutableMapOf<String, String>()
        config["cloud_name"] = "dnzu5bjk0"
        MediaManager.init(this, config)
    }
}