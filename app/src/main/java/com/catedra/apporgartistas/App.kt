package com.catedra.apporgartistas

import android.app.Application
import com.cloudinary.android.MediaManager

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        MediaManager.init(this, mapOf("cloud_name" to "dnzu5bjk0"))
    }
}