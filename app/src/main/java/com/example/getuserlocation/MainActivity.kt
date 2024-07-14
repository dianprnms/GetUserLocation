package com.example.getuserlocation

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.getuserlocation.databinding.ActivityMainBinding
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import java.io.IOException
import java.util.Locale

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var locationRequest: LocationRequest
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        binding.btnGetLocation.setOnClickListener {
            checkLocationPermission()
        }
    }

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
                // when permssion get already grant
            checkGPS()
        }
        else {
            // when permission denied
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
        }

    }

    private fun checkGPS() {
        locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 5000
            fastestInterval = 2000
        }

        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(locationRequest)

        builder.setAlwaysShow(true)
        val result = LocationServices.getSettingsClient(
            this.applicationContext
        )
            .checkLocationSettings(builder.build())

        result.addOnCompleteListener { task ->
            try {
                task.getResult(
                    ApiException::class.java
                )
                getUserLocation()
            } catch (e : ApiException) {
                e.printStackTrace()

                when(e.statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                        val resolvableApiException = e as ResolvableApiException
                        resolvableApiException.startResolutionForResult(this,200)
                    } catch (_: IntentSender.SendIntentException){


                    }

                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {

                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun getUserLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Handle case where permissions are not granted
            return
        }

        fusedLocationProviderClient.lastLocation.addOnCompleteListener { task ->
            if (task.isSuccessful && task.result != null) {
                val location = task.result
                Log.d(TAG, "Location found: ${location.latitude}, ${location.longitude}")

                try {
                    val geoCoder = Geocoder(this, Locale.getDefault())
                    val address = geoCoder.getFromLocation(location.latitude, location.longitude, 1)
                    val addressLine = address!![0].getAddressLine(0)
                    Log.d(TAG, "Address found: $addressLine")

                    // Set address in TextView
                    binding.pointAddress.text = addressLine

                    // Open location on map when TextView is clicked
                    openLocation(addressLine)
                } catch (e: IOException) {
                    binding.pointAddress.text = "Failed to get address: ${e.message}"
                }
            } else {
                Log.e(TAG, "Failed to get location: ${task.exception}")
            }
        }
    }

    private fun openLocation(location: String) {
        // here we open this location in google map

        //lets button on click
        binding.btnGetLocation.setOnClickListener {
            if (binding.pointAddress.text.isNotEmpty()) {
                // when loation is not empty
                val uri = Uri.parse("geo:0, 0?q:$location")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                intent.setPackage("com.google.android.apps.maps")
                startActivities(arrayOf(intent))
            }
        }


    }
}