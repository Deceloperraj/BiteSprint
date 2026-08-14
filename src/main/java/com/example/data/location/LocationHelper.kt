package com.example.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

data class UserLocationResult(
    val latitude: Double,
    val longitude: Double,
    val addressLabel: String,
    val fullAddress: String,
    val city: String
)

class LocationHelper(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    fun hasLocationPermission(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocation || coarseLocation
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        if (!hasLocationPermission()) return null

        return withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { continuation ->
                val cancellationTokenSource = CancellationTokenSource()

                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        continuation.resume(location)
                    } else {
                        // Fallback to last known location
                        fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc: Location? ->
                            continuation.resume(lastLoc)
                        }.addOnFailureListener {
                            continuation.resume(null)
                        }
                    }
                }.addOnFailureListener {
                    // Try last location
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc: Location? ->
                        continuation.resume(lastLoc)
                    }.addOnFailureListener {
                        continuation.resume(null)
                    }
                }

                continuation.invokeOnCancellation {
                    cancellationTokenSource.cancel()
                }
            }
        }
    }

    suspend fun reverseGeocode(latitude: Double, longitude: Double): UserLocationResult {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses: List<Address>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine { cont ->
                        geocoder.getFromLocation(latitude, longitude, 1) { list ->
                            cont.resume(list)
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(latitude, longitude, 1)
                }

                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    val feature = addr.featureName
                    val subLocality = addr.subLocality ?: addr.subAdminArea ?: ""
                    val locality = addr.locality ?: addr.adminArea ?: "Current Location"
                    val line = addr.getAddressLine(0) ?: "$subLocality, $locality"

                    val shortLabel = when {
                        !subLocality.isNullOrBlank() -> subLocality
                        !feature.isNullOrBlank() -> feature
                        else -> locality
                    }

                    UserLocationResult(
                        latitude = latitude,
                        longitude = longitude,
                        addressLabel = shortLabel,
                        fullAddress = line,
                        city = locality
                    )
                } else {
                    UserLocationResult(
                        latitude = latitude,
                        longitude = longitude,
                        addressLabel = "Nearby GPS Location",
                        fullAddress = "Coordinates: ${String.format("%.4f", latitude)}, ${String.format("%.4f", longitude)}",
                        city = "Current Area"
                    )
                }
            } catch (e: Exception) {
                UserLocationResult(
                    latitude = latitude,
                    longitude = longitude,
                    addressLabel = "GPS Location",
                    fullAddress = "Near ${String.format("%.4f", latitude)}, ${String.format("%.4f", longitude)}",
                    city = "Local Area"
                )
            }
        }
    }
}
