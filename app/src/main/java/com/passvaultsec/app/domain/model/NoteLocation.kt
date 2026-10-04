package com.passvaultsec.app.domain.model

/**
 * Representa la ubicación geográfica real asociada a una nota.
 * Incluye coordenadas GPS, dirección geocodificada y enlaces a Google Maps.
 */
data class NoteLocation(
    val latitude: Double,
    val longitude: Double,
    val address: String = "",
    val placeName: String = "",
    val mapSnapshotUrl: String = ""
) {
    /**
     * Retorna una URL pública de Google Maps para abrir la ubicación en navegador o app.
     */
    fun toGoogleMapsUrl(): String {
        return "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"
    }

    /**
     * Retorna un URI con esquema geo: para ser lanzado con un Intent hacia Google Maps.
     */
    fun toGeoUriString(): String {
        val label = if (address.isNotBlank()) " ($address)" else ""
        return "geo:$latitude,$longitude?q=$latitude,$longitude$label"
    }
}
