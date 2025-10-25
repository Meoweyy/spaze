package com.sc2006.spaze.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Objects for LTA DataMall Carpark Availability API
 * API Endpoint: https://datamall2.mytransport.sg/ltaodataservice/CarParkAvailabilityv2
 */

data class CarparkAvailabilityResponse(
    @SerializedName("odata.metadata")
    val metadata: String?,
    @SerializedName("value")
    val carparks: List<CarparkAvailabilityDto>
)

data class CarparkAvailabilityDto(
    @SerializedName("CarParkID")
    val carparkID: String,
    @SerializedName("Area")
    val area: String,
    @SerializedName("Development")
    val development: String,
    @SerializedName("Location")
    val location: String,
    @SerializedName("AvailableLots")
    val availableLots: Int,
    @SerializedName("LotType")
    val lotType: String,
    @SerializedName("Agency")
    val agency: String
)

/**
 * Alternative: URA Carpark Availability Response
 * API: https://www.ura.gov.sg/maps/api/
 */
data class UraCarparkResponse(
    @SerializedName("Status")
    val status: String,
    @SerializedName("Message")
    val message: String,
    @SerializedName("Result")
    val result: List<UraCarparkDto>
)

data class UraCarparkDto(
    @SerializedName("carparkNo")
    val carparkNo: String,
    @SerializedName("lotsAvailable")
    val lotsAvailable: String,
    @SerializedName("lotType")
    val lotType: String,
    @SerializedName("geometries")
    val geometries: List<UraGeometry>
)

data class UraGeometry(
    @SerializedName("coordinates")
    val coordinates: String
)
