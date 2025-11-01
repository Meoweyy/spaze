package com.sc2006.spaze.data.util

import android.content.Context
import com.sc2006.spaze.data.remote.dto.CarparkCsvDto
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Utility to parse HDB Carpark CSV file
 */
object CsvParser {

    /**
     * Parse CSV from assets folder
     * @param context Android context to access assets
     * @param fileName Name of CSV file in assets folder
     * @return List of CarparkCsvDto objects
     */
    fun parseCarparkCsv(context: Context, fileName: String = "HDBCarparkInformation.csv"): List<CarparkCsvDto> {
        val carparks = mutableListOf<CarparkCsvDto>()

        try {
            val inputStream = context.assets.open(fileName)
            val reader = BufferedReader(InputStreamReader(inputStream))

            // Skip header line
            reader.readLine()

            // Read each line
            reader.forEachLine { line ->
                try {
                    val columns = line.split(",").map { it.trim() }

                    if (columns.size >= 12) {
                        val dto = CarparkCsvDto(
                            carParkNo = columns[0],
                            address = columns[1],
                            xCoord = columns[2],
                            yCoord = columns[3],
                            carParkType = columns[4],
                            typeOfParkingSystem = columns[5],
                            shortTermParking = columns[6],
                            freeParking = columns[7],
                            nightParking = columns[8],
                            carParkDecks = columns[9],
                            gantryHeight = columns[10],
                            carParkBasement = columns[11]
                        )
                        carparks.add(dto)
                    }
                } catch (e: Exception) {
                    // Skip malformed lines
                    android.util.Log.w("CsvParser", "Skipped line: $line", e)
                }
            }

            reader.close()
        } catch (e: Exception) {
            android.util.Log.e("CsvParser", "Failed to parse CSV", e)
        }

        return carparks
    }
}