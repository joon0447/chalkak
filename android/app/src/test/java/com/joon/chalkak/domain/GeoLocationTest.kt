package com.joon.chalkak.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoLocationTest {
    @Test
    fun distanceToMeters_returnsZero_forSameLocation() {
        val location = GeoLocation(latitude = 37.5665, longitude = 126.9780)

        assertEquals(0.0, location.distanceToMeters(location), 0.001)
    }

    @Test
    fun distanceToMeters_calculatesDistanceBetweenNearbyCoordinates() {
        val seoulCityHall = GeoLocation(latitude = 37.5665, longitude = 126.9780)
        val gwanghwamun = GeoLocation(latitude = 37.5759, longitude = 126.9768)

        val distanceMeters = seoulCityHall.distanceToMeters(gwanghwamun)

        assertEquals(1050.0, distanceMeters, 80.0)
    }

    @Test
    fun boundsForRadius_containsLocationMovedWithinRadius() {
        val center = GeoLocation(latitude = 37.5665, longitude = 126.9780)
        val bounds = center.boundsForRadius(radiusMeters = 1_000.0)

        assertTrue(37.5665 in bounds.minLatitude..bounds.maxLatitude)
        assertTrue(126.9780 in bounds.minLongitude..bounds.maxLongitude)
        assertTrue(bounds.maxLatitude > bounds.minLatitude)
        assertTrue(bounds.maxLongitude > bounds.minLongitude)
    }
}
