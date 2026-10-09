package com.nagpurpulse.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

class LocationHelper(
    private val context: Context
) {

    @SuppressLint("MissingPermission")
    suspend fun getSubLocality(): String {

        return suspendCancellableCoroutine { continuation ->

            val fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(context)

            fusedLocationClient.getCurrentLocation(
                com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                null
            ).addOnSuccessListener { location ->

                if (location == null) {
                    continuation.resume("Near You")
                    return@addOnSuccessListener
                }

                try {

                    val geocoder =
                        Geocoder(context, Locale.getDefault())

                    val addresses =
                        geocoder.getFromLocation(
                            location.latitude,
                            location.longitude,
                            1
                        )

                    val address = addresses?.firstOrNull()

                    val locality =
                        address?.subLocality
                            ?: address?.featureName

                    val city =
                        address?.locality

                    val area = when {

                        locality.isNullOrBlank() && city.isNullOrBlank() ->
                            "Near You"

                        locality.isNullOrBlank() ->
                            city!!

                        city.isNullOrBlank() ->
                            locality

                        locality.contains(city, ignoreCase = true) ->
                            locality

                        else ->
                            "$locality, $city"
                    }

                    continuation.resume(area)

                }

                catch (e: Exception) {
                    continuation.resume("Near You")
                }

            }.addOnFailureListener {
                continuation.resume("Near You")
            }
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun getCoordinates(): Pair<Double, Double>? {

        return suspendCancellableCoroutine { continuation ->

            val fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(context)

            fusedLocationClient.getCurrentLocation(
                com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                null
            ).addOnSuccessListener { location ->

                if (location == null) {
                    continuation.resume(null)
                    return@addOnSuccessListener
                }

                continuation.resume(
                    Pair(
                        location.latitude,
                        location.longitude
                    )
                )
            }.addOnFailureListener {
                continuation.resume(null)
            }
        }
    }
}