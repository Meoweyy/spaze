package com.sc2006.spaze.data.local.seed

import com.sc2006.spaze.data.local.entity.CarparkEntity.PriceTier

data class CarparkMetadata(
    val carparkNumber: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val totalLots: Int,
    val priceTier: PriceTier,
    val baseHourlyRate: Double
)

object CarparkMetadataProvider {

    private val metadataByNumber: Map<String, CarparkMetadata> = listOf(
        CarparkMetadata(
            carparkNumber = "MBSC",
            name = "Marina Bay Sands",
            address = "10 Bayfront Ave, Singapore 018956",
            latitude = 1.2834,
            longitude = 103.8607,
            totalLots = 1016,
            priceTier = PriceTier.PREMIUM,
            baseHourlyRate = 8.0
        ),
        CarparkMetadata(
            carparkNumber = "CHANGI",
            name = "Changi Airport T3",
            address = "65 Airport Blvd, Singapore 819663",
            latitude = 1.3616,
            longitude = 103.9906,
            totalLots = 2500,
            priceTier = PriceTier.PREMIUM,
            baseHourlyRate = 5.0
        ),
        CarparkMetadata(
            carparkNumber = "VIVO",
            name = "VivoCity",
            address = "1 HarbourFront Walk, Singapore 098585",
            latitude = 1.2644,
            longitude = 103.8223,
            totalLots = 1200,
            priceTier = PriceTier.STANDARD,
            baseHourlyRate = 2.4
        ),
        CarparkMetadata(
            carparkNumber = "HDB_QUEENSTOWN",
            name = "HDB Queenstown",
            address = "10 Commonwealth Ave, Singapore 149603",
            latitude = 1.2946,
            longitude = 103.8057,
            totalLots = 420,
            priceTier = PriceTier.BUDGET,
            baseHourlyRate = 0.9
        ),
        CarparkMetadata(
            carparkNumber = "NLB",
            name = "National Library",
            address = "100 Victoria St, Singapore 188064",
            latitude = 1.2966,
            longitude = 103.8535,
            totalLots = 280,
            priceTier = PriceTier.STANDARD,
            baseHourlyRate = 1.5
        )
    ).associateBy { it.carparkNumber }

    fun getMetadata(carparkNumber: String): CarparkMetadata? = metadataByNumber[carparkNumber]

    fun allMetadata(): List<CarparkMetadata> = metadataByNumber.values.toList()
}
