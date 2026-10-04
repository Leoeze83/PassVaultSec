package com.passvaultsec.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteLocationTest {

    @Test
    fun toGoogleMapsUrl_formatsCorrectCoordinates() {
        val location = NoteLocation(
            latitude = 40.4168,
            longitude = -3.7038,
            address = "Puerta del Sol, Madrid",
            placeName = "Puerta del Sol"
        )

        val url = location.toGoogleMapsUrl()

        assertEquals("https://www.google.com/maps/search/?api=1&query=40.4168,-3.7038", url)
    }

    @Test
    fun toGeoUriString_includesLabelWhenAddressIsPresent() {
        val location = NoteLocation(
            latitude = -34.6037,
            longitude = -58.3816,
            address = "Buenos Aires",
            placeName = "Centro"
        )

        val geoUri = location.toGeoUriString()

        assertTrue(geoUri.startsWith("geo:-34.6037,-58.3816?q=-34.6037,-58.3816"))
        assertTrue(geoUri.contains("Buenos Aires"))
    }
}
