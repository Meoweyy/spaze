package com.sc2006.spaze.data.remote.api

import com.sc2006.spaze.data.remote.dto.DataGovCarparkResponse
import retrofit2.Response
import retrofit2.http.GET

/**
 * API Service for Data.gov.sg Carpark Availability
 */
interface CarparkApiService {

    /**
     * Get real-time carpark availability from Data.gov.sg
     * Resource: carpark-availability
     */
    @GET("v1/transport/carpark-availability")
    suspend fun getCarparkAvailability(): Response<DataGovCarparkResponse>

    companion object {
        const val BASE_URL = "https://data.gov.sg/"
    }
}