package com.joon.chalkak.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import com.joon.chalkak.domain.GeoLocation
import com.joon.chalkak.domain.LocationReading
import com.joon.chalkak.domain.LocationSpeedSample
import com.joon.chalkak.domain.toSpeedSample
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class AndroidLocationSpeedTracker(
    context: Context
) {
    private val appContext = context.applicationContext
    private val locationManager =
        appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    fun speedSamples(highAccuracy: Boolean): Flow<LocationSpeedSample> = callbackFlow {
        if (!appContext.hasLocationPermission()) {
            close(LocationPermissionMissingException())
            return@callbackFlow
        }

        val providers = enabledProviders(highAccuracy)
        if (providers.isEmpty()) {
            close(LocationProviderUnavailableException())
            return@callbackFlow
        }

        var previousReading: LocationReading? = null
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val reading = location.toReading()
                trySend(reading.toSpeedSample(previousReading))
                previousReading = reading
            }

            @Deprecated("Deprecated in Android framework")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        }

        try {
            providers.forEach { provider ->
                locationManager.requestLocationUpdates(
                    provider,
                    MIN_UPDATE_INTERVAL_MILLIS,
                    MIN_UPDATE_DISTANCE_METERS,
                    listener,
                    Looper.getMainLooper()
                )
            }
        } catch (exception: SecurityException) {
            close(LocationPermissionMissingException())
        }

        awaitClose {
            locationManager.removeUpdates(listener)
        }
    }

    private fun enabledProviders(highAccuracy: Boolean): List<String> {
        val preferredProviders = if (highAccuracy) {
            listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        } else {
            listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
        }

        return preferredProviders
            .filter { provider -> locationManager.isProviderEnabled(provider) }
    }

    private fun Location.toReading(): LocationReading =
        LocationReading(
            location = GeoLocation(latitude = latitude, longitude = longitude),
            elapsedRealtimeMillis = elapsedRealtimeNanos / NANOS_PER_MILLIS,
            speedMetersPerSecond = if (hasSpeed()) speed.toDouble() else null,
            accuracyMeters = if (hasAccuracy()) accuracy else null,
            bearingDegrees = if (hasBearing()) bearing.toDouble() else null
        )

    private fun Context.hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineGranted || coarseGranted
    }

    private companion object {
        const val MIN_UPDATE_INTERVAL_MILLIS = 1_000L
        const val MIN_UPDATE_DISTANCE_METERS = 0f
        const val NANOS_PER_MILLIS = 1_000_000L
    }
}
