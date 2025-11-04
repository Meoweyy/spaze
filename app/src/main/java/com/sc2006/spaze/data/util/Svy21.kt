package com.sc2006.spaze.data.util

import kotlin.math.*

/**
 * Accurate SVY21 <-> WGS84 conversion utilities (ported from well-known implementation).
 * Use convertToLatLon(northing, easting) to get latitude/longitude in degrees.
 */
object Svy21 {
    private const val RAD_RATIO = Math.PI / 180.0

    // Datum and projection constants (SVY21 / EPSG:3414)
    private const val A = 6378137.0                 // Semi-major axis
    private const val F = 1 / 298.257223563         // Flattening
    private const val ORIGIN_LAT = 1.366666         // degrees
    private const val ORIGIN_LON = 103.833333       // degrees
    private const val FALSE_NORTHING = 38744.572    // meters
    private const val FALSE_EASTING = 28001.642     // meters
    private const val K = 1.0                       // scale factor

    // Precomputed constants
    private val B = A * (1 - F)
    private val E2 = (2 * F) - (F * F)
    private val E4 = E2 * E2
    private val E6 = E4 * E2
    private val A0 = 1 - (E2 / 4) - (3 * E4 / 64) - (5 * E6 / 256)
    private val A2 = (3.0 / 8.0) * (E2 + (E4 / 4) + (15 * E6 / 128))
    private val A4 = (15.0 / 256.0) * (E4 + (3 * E6 / 4))
    private val A6 = 35.0 * E6 / 3072.0
    private val N = (A - B) / (A + B)
    private val N2 = N * N
    private val N3 = N2 * N
    private val N4 = N2 * N2
    private val G = A * (1 - N) * (1 - N2) * (1 + (9 * N2 / 4) + (225 * N4 / 64)) * RAD_RATIO

    private fun calcM(latDeg: Double): Double {
        val latR = latDeg * RAD_RATIO
        return A * ((A0 * latR) - (A2 * sin(2 * latR)) + (A4 * sin(4 * latR)) - (A6 * sin(6 * latR)))
    }

    private fun calcRho(sin2Lat: Double): Double {
        val num = A * (1 - E2)
        val denom = (1 - E2 * sin2Lat).pow(3.0 / 2.0)
        return num / denom
    }

    private fun calcV(sin2Lat: Double): Double {
        val poly = 1 - E2 * sin2Lat
        return A / sqrt(poly)
    }

    /**
     * Convert SVY21 (northing, easting) to WGS84 latitude/longitude (degrees).
     */
    fun convertToLatLon(northing: Double, easting: Double): Pair<Double, Double> {
        val nPrime = northing - FALSE_NORTHING
        val m0 = calcM(ORIGIN_LAT)
        val mPrime = m0 + (nPrime / K)
        val sigma = (mPrime / G) * RAD_RATIO

        val latPrimeT1 = ((3 * N / 2) - (27 * N3 / 32)) * sin(2 * sigma)
        val latPrimeT2 = ((21 * N2 / 16) - (55 * N4 / 32)) * sin(4 * sigma)
        val latPrimeT3 = (151 * N3 / 96) * sin(6 * sigma)
        val latPrimeT4 = (1097 * N4 / 512) * sin(8 * sigma)
        val latPrime = sigma + latPrimeT1 + latPrimeT2 + latPrimeT3 + latPrimeT4

        val sinLatPrime = sin(latPrime)
        val sin2LatPrime = sinLatPrime * sinLatPrime
        val rhoPrime = calcRho(sin2LatPrime)
        val vPrime = calcV(sin2LatPrime)
        val psiPrime = vPrime / rhoPrime
        val psiPrime2 = psiPrime * psiPrime
        val psiPrime3 = psiPrime2 * psiPrime
        val psiPrime4 = psiPrime3 * psiPrime
        val tPrime = tan(latPrime)
        val tPrime2 = tPrime * tPrime
        val tPrime4 = tPrime2 * tPrime2
        val tPrime6 = tPrime4 * tPrime2
        val ePrime = easting - FALSE_EASTING
        val x = ePrime / (K * vPrime)
        val x2 = x * x
        val x3 = x2 * x
        val x5 = x3 * x2
        val x7 = x5 * x2

        val latFactor = tPrime / (K * rhoPrime)
        val latTerm1 = latFactor * ((ePrime * x) / 2.0)
        val latTerm2 = latFactor * ((ePrime * x3) / 24.0) * ((-4 * psiPrime2 + (9 * psiPrime) * (1 - tPrime2) + (12 * tPrime2)))
        val latTerm3 = latFactor * ((ePrime * x5) / 720.0) * ((8 * psiPrime4) * (11 - 24 * tPrime2) - (12 * psiPrime3) * (21 - 71 * tPrime2) + (15 * psiPrime2) * (15 - 98 * tPrime2 + 15 * tPrime4) + (180 * psiPrime) * (5 * tPrime2 - 3 * tPrime4) + 360 * tPrime4)
        val latTerm4 = latFactor * ((ePrime * x7) / 40320.0) * (1385 - 3633 * tPrime2 + 4095 * tPrime4 + 1575 * tPrime6)
        val lat = latPrime - latTerm1 + latTerm2 - latTerm3 + latTerm4

        val secLatPrime = 1.0 / cos(lat)
        val lonTerm1 = x * secLatPrime
        val lonTerm2 = ((x3 * secLatPrime) / 6.0) * (psiPrime + 2 * tPrime2)
        val lonTerm3 = ((x5 * secLatPrime) / 120.0) * ((-4 * psiPrime3) * (1 - 6 * tPrime2) + psiPrime2 * (9 - 68 * tPrime2) + 72 * psiPrime * tPrime2 + 24 * tPrime4)
        val lonTerm4 = ((x7 * secLatPrime) / 5040.0) * (61 + 662 * tPrime2 + 1320 * tPrime4 + 720 * tPrime6)
        val lon = (ORIGIN_LON * RAD_RATIO) + lonTerm1 - lonTerm2 + lonTerm3 - lonTerm4

        return Pair(lat / RAD_RATIO, lon / RAD_RATIO)
    }
}

