package com.example.trackingdemoapp

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.location.Location
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class LocationService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var locationClient: LocationClient
    private val trackedLocations = mutableListOf<Location>()

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_LOCATION_UPDATE = "ACTION_LOCATION_UPDATE"
    }
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        locationClient = DefaultLocationClient(
            applicationContext, LocationServices.getFusedLocationProviderClient(applicationContext)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> start()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun start() {
        val notification = NotificationCompat.Builder(this, "location")
            .setContentTitle("Tracking location...")
            .setContentText("Location: null")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setOngoing(true)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        locationClient.getLocationUpdate(180000L) // every 3 minutes
            .catch { e ->
                e.printStackTrace()
            }
            .onEach { location ->
                val lat = location.latitude.toString()
                val long = location.longitude.toString()
                val updateNotification = notification.setContentText("Location: {$lat,$long}")
                notificationManager.notify(1, updateNotification.build())
                // Add tracked location to the list
                addTrackedLocation(location)
            }
            .launchIn(serviceScope)

        startForeground(1, notification.build())
    }

    private fun addTrackedLocation(location: Location) {
        trackedLocations.add(location)
        sendBroadcast(Intent(ACTION_LOCATION_UPDATE))
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
