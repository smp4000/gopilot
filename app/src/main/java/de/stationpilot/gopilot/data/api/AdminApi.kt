package de.stationpilot.gopilot.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StationItem(
    val ulid: String,
    val name: String,
    val city: String?,
    val street: String?,
)

@JsonClass(generateAdapter = true)
data class StationsResponse(val stations: List<StationItem>)

@JsonClass(generateAdapter = true)
data class EmployeeItem(
    val ulid: String,
    val name: String,
    @Json(name = "job_title") val jobTitle: String?,
    @Json(name = "scan_code") val scanCode: String?,
    @Json(name = "has_nfc") val hasNfc: Boolean,
    @Json(name = "nfc_uid") val nfcUid: String?,
)

@JsonClass(generateAdapter = true)
data class EmployeesResponse(val employees: List<EmployeeItem>)

@JsonClass(generateAdapter = true)
data class SaveNfcRequest(
    @Json(name = "nfc_uid")   val nfcUid: String,
    @Json(name = "scan_code") val scanCode: String?,
)

@JsonClass(generateAdapter = true)
data class SaveNfcResponse(val message: String)
