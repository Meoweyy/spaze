package com.sc2006.spaze.data.api

import com.sc2006.spaze.BuildConfig
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers


data class CarparkLotInfo(val total_lots: String, val lot_type: String, val lots_available: String)
data class CarparkInfo(val carpark_number: String, val update_datetime: String, val carpark_info: List<CarparkLotInfo>)
data class Item(val carpark_data: List<CarparkInfo>)
data class CarparkResponse(val items: List<Item>)


interface CarparkApiService {
    @Headers("AccountKey: ${BuildConfig.LTA_API_KEY}")
    @GET("carpark-availability")
    fun getCarparkAvailability(): Call<CarparkResponse>
}


object RetrofitClient {
    private const val BASE_URL = "https://api.data.gov.sg/v1/transport/"
    val instance: CarparkApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CarparkApiService::class.java)
    }
}
