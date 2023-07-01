package com.example.trackingdemoapp.viewmodel

import android.location.Location
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class LocationViewModel : ViewModel() {
    val locationFlow = MutableStateFlow<List<Location>>(emptyList())
}
