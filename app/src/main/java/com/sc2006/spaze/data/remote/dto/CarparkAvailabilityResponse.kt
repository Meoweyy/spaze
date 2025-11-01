package com.sc2006.spaze.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Response from Data.gov.sg Carpark Availability API
 * https://data.gov.sg/api/action/datastore_search?resource_id=carpark-availability
 */
data class DataGovCarparkResponse(
    @SerializedName("api_info")
    val apiInfo: ApiInfo?,

    @SerializedName("items")
    val items: List<CarparkAvailabilityItem>
)

data class ApiInfo(
    @SerializedName("status")
    val status: String  // "healthy"
)

data class CarparkAvailabilityItem(
    @SerializedName("timestamp")
    val timestamp: String,  // "2025-08-07T09:01:00+08:00"

    @SerializedName("carpark_data")
    val carparkData: List<CarparkData>
)

data class CarparkData(
    @SerializedName("carpark_number")
    val carparkNumber: String,  // "HG50", "ACB", etc.

    @SerializedName("carpark_info")
    val carparkInfo: List<CarparkLotInfo>
)

data class CarparkLotInfo(
    @SerializedName("total_lots")
    val totalLots: String,  // "500"

    @SerializedName("lot_type")
    val lotType: String,    // "C", "H", "Y", "S"

    @SerializedName("lots_available")
    val lotsAvailable: String  // "123"
)