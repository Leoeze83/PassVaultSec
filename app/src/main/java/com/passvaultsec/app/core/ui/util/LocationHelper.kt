package com.passvaultsec.app.core.ui.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.passvaultsec.app.domain.model.NoteLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Utilidad para captura de ubicación geográfica en tiempo real
 * con resolución de dirección mediante geocodificación inversa y enlaces a Google Maps.
 */
class LocationHelper(private val context: Context) {

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    /**
     * Verifica si se han concedido los permisos de ubicación necesarios.
     */
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

    /**
     * Obtiene la ubicación geográfica actual y resuelve su dirección de forma asíncrona.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentNoteLocation(): Result<NoteLocation> = withContext(Dispatchers.IO) {
        if (!hasLocationPermission()) {
            return@withContext Result.failure(SecurityException("Permiso de ubicación no concedido"))
        }

        runCatching {
            var rawLocation: Location? = null

            // 1. Intentar con FusedLocationProviderClient (Google Play Services)
            try {
                val cts = CancellationTokenSource()
                rawLocation = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cts.token
                ).await()
            } catch (e: Exception) {
                // Fused location falló, fallback a LocationManager estándar
            }

            // 2. Si no se obtuvo, intentar con la última ubicación conocida
            if (rawLocation == null) {
                try {
                    rawLocation = fusedLocationClient.lastLocation.await()
                } catch (e: Exception) {
                    // Fallback a LocationManager del sistema
                }
            }

            // 3. Fallback a LocationManager nativo del sistema
            if (rawLocation == null) {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                if (locationManager != null) {
                    val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
                    for (provider in providers) {
                        try {
                            val loc = locationManager.getLastKnownLocation(provider)
                            if (loc != null) {
                                rawLocation = loc
                                break
                            }
                        } catch (e: Exception) {
                            // Continuar con el siguiente proveedor
                        }
                    }
                }
            }

            val finalLocation = rawLocation
                ?: throw IllegalStateException("No se pudo obtener la posición GPS. Asegúrate de tener la ubicación activada.")

            val lat = finalLocation.latitude
            val lng = finalLocation.longitude

            // Geocodificación inversa para obtener la dirección legible
            val resolvedAddress = resolveAddress(lat, lng)

            // URL para previsualización de mapa
            val mapPreviewUrl = "https://staticmap.openstreetmap.de/staticmap.php?center=$lat,$lng&zoom=15&size=600x300&markers=$lat,$lng,ol-marker"

            NoteLocation(
                latitude = lat,
                longitude = lng,
                address = resolvedAddress.first,
                placeName = resolvedAddress.second,
                mapSnapshotUrl = mapPreviewUrl
            )
        }
    }

    /**
     * Resuelve coordenadas en (Dirección completa, Nombre de localidad / punto de interés).
     */
    private fun resolveAddress(latitude: Double, longitude: Double): Pair<String, String> {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(latitude, longitude, 1)
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(latitude, longitude, 1)
            }

            val first = addresses?.firstOrNull()
            if (first != null) {
                val thoroughfare = first.thoroughfare.orEmpty()
                val subThoroughfare = first.subThoroughfare.orEmpty()
                val streetAddress = if (thoroughfare.isNotEmpty()) {
                    if (subThoroughfare.isNotEmpty()) "$thoroughfare $subThoroughfare" else thoroughfare
                } else ""

                val locality = first.locality ?: first.subAdminArea.orEmpty()
                val adminArea = first.adminArea.orEmpty()
                val country = first.countryName.orEmpty()

                val parts = listOfNotNull(
                    streetAddress.ifEmpty { null },
                    locality.ifEmpty { null },
                    adminArea.ifEmpty { null },
                    country.ifEmpty { null }
                )

                val fullAddress = if (parts.isNotEmpty()) parts.joinToString(", ") else "Lat: %.5f, Lng: %.5f".format(latitude, longitude)
                val placeName = first.featureName ?: locality.ifEmpty { "Ubicación actual" }

                Pair(fullAddress, placeName)
            } else {
                Pair("Lat: %.5f, Lng: %.5f".format(latitude, longitude), "Ubicación actual")
            }
        } catch (e: Exception) {
            Pair("Lat: %.5f, Lng: %.5f".format(latitude, longitude), "Ubicación actual")
        }
    }
}
