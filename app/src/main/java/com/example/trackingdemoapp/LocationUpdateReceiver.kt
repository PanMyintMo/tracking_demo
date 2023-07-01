package com.example.trackingdemoapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class LocationUpdateReceiver(private val intent: Intent) : BroadcastReceiver() {
    private val _locationFlow: Flow<Location> = callbackFlow {
        val location = intent.getParcelableExtra<Location>(LocationService.EXTRA_LOCATION)
        location?.let {
            trySend(it).isSuccess // Emit the location update to the flow
        }
        awaitClose()
    }

    val locationFlow: Flow<Location>
        get() = _locationFlow

    override fun onReceive(context: Context?, intent: Intent?) {
        // No need to handle the received intent here
    }
}
