package com.example.trackingdemoapp

import android.Manifest
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.example.trackingdemoapp.ui.theme.TrackingDemoAppTheme
import com.google.android.gms.location.LocationServices
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private val trackedLocations = mutableStateListOf<Location>()

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestLocationPermission()


    LocationService.ACTION_START


   


        setContent {
            TrackingDemoAppTheme {
                Column(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(trackedLocations.reversed()) { location ->
                            val geocoder = Geocoder(this@MainActivity, Locale.getDefault())
                            val addresses: List<Address>?
                            var addressText = ""

                            val currentTime = Calendar.getInstance().time
                            val timeFormat = SimpleDateFormat("h:mm:ss", Locale.getDefault())
                            val formattedTime = timeFormat.format(currentTime)

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
                                Text(text = "Time: $formattedTime")
                            }
                        }
                    }

                    Button(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(20.dp),
                        onClick = {
                            if (hasLocationPermission()) {
                                val locationManager =
                                    getSystemService(LOCATION_SERVICE) as LocationManager
                                val isGpsEnabled =
                                    locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                                val isNetworkEnabled =
                                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

                                if (!isGpsEnabled && !isNetworkEnabled)  {
                                    // Location services are disabled, show a dialog to prompt the user to enable it
                                    val dialogBuilder = AlertDialog.Builder(this@MainActivity)
                                    dialogBuilder.setTitle("Location Services Disabled")
                                    dialogBuilder.setMessage("Please enable location services to use this app.")

                                    dialogBuilder.setPositiveButton("Open Settings", DialogInterface.OnClickListener { dialog, which ->
                                        // Open location settings when the user clicks the positive button
                                        val settingsIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                        startActivity(settingsIntent)
                                    })

                                    dialogBuilder.setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, which ->
                                        // Handle cancel click (if needed)
                                        dialog.dismiss()
                                    })

                                    val dialog = dialogBuilder.create()
                                    dialog.show()

                                } else {
                                    if (hasInternetAccess()) {
                                        startLocationService()
                                    } else {
                                        // No internet access, show a dialog to prompt the user to open internet settings
                                        showNoInternetAccessDialog()
                                    }
                                }

                            } else {
                                requestLocationPermission()
                            }
                        }
                    ) {
                        Text("Track Location")
                    }
                }
            }
        }
    }

    private fun showNoInternetAccessDialog() {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle("No Internet Access")
        dialogBuilder.setMessage("Please enable internet access to use this feature.")

        dialogBuilder.setPositiveButton("Open Settings") { _, _ ->
            // Open internet settings when the user clicks the positive button
            val settingsIntent = Intent(Settings.ACTION_WIFI_SETTINGS)
            startActivity(settingsIntent)
        }

        dialogBuilder.setNegativeButton("Cancel") { dialog, _ ->
            // Handle cancel click (if needed)
            dialog.dismiss()
        }

        val dialog = dialogBuilder.create()
        dialog.show()
    }
    private fun hasInternetAccess(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun startLocationService() {
        Intent(this, LocationService::class.java).apply {
            action = LocationService.ACTION_START
            startService(this)

            val fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(applicationContext)

            if (ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    location?.let {
                        trackedLocations.add(location)
                    }
                }
            }
        }

        // Get the last tracked location from the list
        val lastTrackedLocation = trackedLocations.lastOrNull()
        lastTrackedLocation?.let { location ->
            val geocoder = Geocoder(this@MainActivity, Locale.getDefault())
            val addresses: List<Address>?
            var addressText = ""

            val currentTime = Calendar.getInstance().time
            val timeFormat = SimpleDateFormat("h:mm:ss", Locale.getDefault())
            val formattedTime = timeFormat.format(currentTime)

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

           println("Location Name: $addressText")
            println("Latitude: $latitudeDMS")
            println("Longitude: $longitudeDMS")
            println("Time: $formattedTime")
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
