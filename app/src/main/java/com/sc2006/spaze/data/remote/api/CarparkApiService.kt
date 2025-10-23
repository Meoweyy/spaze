package com.sc2006.spaze.data.remote.api

import com.sc2006.spaze.data.remote.dto.CarparkAvailabilityResponse
import com.sc2006.spaze.data.remote.dto.UraCarparkResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * Retrofit API Service for Carpark Data
 * Uses LTA DataMall API for carpark availability
 */
interface CarparkApiService {

    /**
     * Get carpark availability from LTA DataMall
     * Requires API key in AccountKey header
     */
    @GET("ltaodataservice/CarParkAvailabilityv2")
    suspend fun getCarparkAvailability(
        @Header("AccountKey") apiKey: String,
        @Query("\$skip") skip: Int = 0
    ): Response<CarparkAvailabilityResponse>

    companion object {
        const val LTA_BASE_URL = "http://datamall2.mytransport.sg/"
    }
}

/**
 * URA Carpark API Service (Alternative source)
 */
interface UraCarparkApiService {

    @GET("CarParkDetails")
    suspend fun getUraCarparkAvailability(
        @Header("AccessKey") accessKey: String,
        @Header("Token") token: String
    ): Response<UraCarparkResponse>

    companion object {
        const val URA_BASE_URL = "https://www.ura.gov.sg/uraDataService/invokeUraDS"
    }
}
