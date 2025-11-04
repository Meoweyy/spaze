package com.sc2006.spaze.util

import com.google.android.gms.maps.model.LatLng

/**
 * Utility object for decoding Google Maps encoded polyline strings
 * Based on Google's polyline encoding algorithm
 */
object PolylineDecoder {
    /**
     * Decodes a Google Maps encoded polyline string into a list of LatLng points
     *
     * @param encodedPolyline The encoded polyline string from Google Directions API
     * @return List of LatLng points representing the decoded path
     */
    fun decode(encodedPolyline: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encodedPolyline.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encodedPolyline[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encodedPolyline[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val p = LatLng(
                lat.toDouble() / 1E5,
                lng.toDouble() / 1E5
            )
            poly.add(p)
        }

        return poly
    }
}
