package com.catedra.apporgartistas

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.cloudinary.android.MediaManager

class MainApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val config = mutableMapOf<String, String>()
        config["cloud_name"] = "dnzu5bjk0"
        MediaManager.init(this, config)

        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channelId = getString(R.string.notif_channel_id)
        val name = getString(R.string.notif_channel_name)
        val descriptionText = getString(R.string.notif_channel_desc)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelId, name, importance).apply {
            description = descriptionText
        }
        // Register the channel with the system
        val notificationManager: NotificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}