package com.example.trackingdemoapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.trackingdemoapp.ui.theme.TrackingDemoAppTheme
import com.example.trackingdemoapp.viewmodel.LocationViewModel
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.*

class MainActivity : ComponentActivity() {
    private val trackedLocations = mutableStateListOf<Location>()
    private lateinit var locationViewModel: LocationViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        locationViewModel = ViewModelProvider(this)[LocationViewModel::class.java]


        lifecycleScope.launch {
            locationViewModel.locationFlow.collect { locations ->
                trackedLocations.clear()
                trackedLocations.addAll(locations)
            }
        }
   ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ), 0
        )
        val fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(applicationContext)

        Intent(this, LocationService::class.java).apply {
            action = LocationService.ACTION_START
            startService(this)
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                trackedLocations.add(location)
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        setContent {
            TrackingDemoAppTheme {
                Column(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(trackedLocations) { location ->
                            val geocoder = Geocoder(this@MainActivity, Locale.getDefault())
                            val addresses: List<Address>?
                            var addressText = ""

                            try {
                                addresses = geocoder.getFromLocation(
                                    location.latitude,
                                    location.longitude,
                                    1
                                )

                                if (!addresses.isNullOrEmpty()) {
                                    val address: Address = addresses[0]
                                    addressText = address.getAddressLine(0)
                                }
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                            val latitudeDMS = convertToDMS(location.latitude)
                            val longitudeDMS = convertToDMS(location.longitude)

                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = "Location Name: $addressText")
                                Text(text = "Latitude: $latitudeDMS")
                                Text(text = "Longitude: $longitudeDMS")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun convertToDMS(decimalDegrees: Double): String {
        val degrees = decimalDegrees.toInt()
        val minutes = ((decimalDegrees - degrees) * 60).toInt()
        val seconds = ((decimalDegrees - degrees - minutes / 60.0) * 3600)
        val formattedSeconds = String.format("%.3f", seconds)

        val direction = if (decimalDegrees >= 0) {
            if (seconds >= 0) "N" else "E"
        } else {
            if (seconds >= 0) "S" else "W"
        }
        return "$degrees° $minutes' $formattedSeconds'' $direction"
    }
}
