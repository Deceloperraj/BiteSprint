package com.example.tracking

import com.example.data.model.OrderStatus
import com.example.data.model.Waypoint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object TrackingEngine {

    /**
     * Generate intermediate realistic street waypoints between start (Restaurant) and end (Customer Address).
     */
    fun generateRouteWaypoints(
        startLat: Double,
        startLng: Double,
        destLat: Double,
        destLng: Double
    ): List<Waypoint> {
        val midLat1 = startLat + (destLat - startLat) * 0.35 + 0.0015
        val midLng1 = startLng + (destLng - startLng) * 0.25 - 0.0010

        val midLat2 = startLat + (destLat - startLat) * 0.70 - 0.0008
        val midLng2 = startLng + (destLng - startLng) * 0.80 + 0.0012

        return listOf(
            Waypoint(startLat, startLng, "Restaurant Pickup"),
            Waypoint(midLat1, midLng1, "Downtown Ave"),
            Waypoint(midLat2, midLng2, "Market St Corner"),
            Waypoint(destLat, destLng, "Customer Delivery Location")
        )
    }

    /**
     * Interpolates position along a multi-segment route given progress [0.0..1.0].
     */
    fun interpolateLocation(waypoints: List<Waypoint>, progress: Float): Pair<Double, Double> {
        if (waypoints.isEmpty()) return Pair(0.0, 0.0)
        if (waypoints.size == 1 || progress <= 0f) return Pair(waypoints.first().lat, waypoints.first().lng)
        if (progress >= 1f) return Pair(waypoints.last().lat, waypoints.last().lng)

        val totalSegments = waypoints.size - 1
        val segmentFloat = progress * totalSegments
        val segmentIndex = segmentFloat.toInt().coerceIn(0, totalSegments - 1)
        val localProgress = segmentFloat - segmentIndex

        val p1 = waypoints[segmentIndex]
        val p2 = waypoints[segmentIndex + 1]

        val lat = p1.lat + (p2.lat - p1.lat) * localProgress
        val lng = p1.lng + (p2.lng - p1.lng) * localProgress

        return Pair(lat, lng)
    }

    /**
     * Computes the heading/bearing angle in degrees from point 1 to point 2.
     */
    fun calculateBearing(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val dLng = Math.toRadians(lng2 - lng1)
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)

        val y = sin(dLng) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(dLng)

        val degrees = Math.toDegrees(atan2(y, x))
        return ((degrees + 360) % 360).toFloat()
    }

    /**
     * Computes distance in kilometers between two points using Haversine formula.
     */
    fun calculateDistanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371.0 // Earth radius km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    /**
     * Map simulation progress (0..1) to order status during an active tracking run.
     */
    fun progressToStatus(progress: Float): OrderStatus {
        return when {
            progress < 0.10f -> OrderStatus.PLACED
            progress < 0.25f -> OrderStatus.CONFIRMED
            progress < 0.45f -> OrderStatus.PREPARING
            progress < 0.95f -> OrderStatus.OUT_FOR_DELIVERY
            else -> OrderStatus.DELIVERED
        }
    }
}
