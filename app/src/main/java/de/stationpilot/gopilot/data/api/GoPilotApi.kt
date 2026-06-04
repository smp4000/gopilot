package de.stationpilot.gopilot.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.*

// ── Request / Response Models ─────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class DeviceRegisterRequest(
    @Json(name = "station_code")  val stationCode: String,
    @Json(name = "device_name")   val deviceName: String,
    @Json(name = "device_model")  val deviceModel: String? = null,
    @Json(name = "android_id")    val androidId: String? = null,
    @Json(name = "app_version")   val appVersion: String? = null,
)

@JsonClass(generateAdapter = true)
data class DeviceRegisterResponse(
    val message: String,
    @Json(name = "device_token") val deviceToken: String,
    val device: DeviceInfo,
)

@JsonClass(generateAdapter = true)
data class DeviceInfo(
    val ulid: String,
    @Json(name = "device_name") val deviceName: String,
    val station: StationInfo,
    val tenant: TenantInfo,
)

@JsonClass(generateAdapter = true)
data class StationInfo(val ulid: String, val name: String, val city: String?)

@JsonClass(generateAdapter = true)
data class TenantInfo(val ulid: String, val name: String)

@JsonClass(generateAdapter = true)
data class EmployeeLoginRequest(
    val method: String,   // "pin" | "scan" | "nfc"
    val value: String,
)

@JsonClass(generateAdapter = true)
data class EmployeeLoginResponse(
    val message: String,
    val employee: EmployeeInfo,
    val permissions: List<String>,
    val roles: List<String>,
    @Json(name = "session_expires_at") val sessionExpiresAt: String,
)

@JsonClass(generateAdapter = true)
data class EmployeeInfo(
    val ulid: String,
    val name: String,
    @Json(name = "job_title") val jobTitle: String?,
    val station: StationInfo,
    @Json(name = "avatar_url") val avatarUrl: String?,
)

@JsonClass(generateAdapter = true)
data class NavigationResponse(
    val navigation: List<NavItem>,
    val tiles: List<Tile>,
)

@JsonClass(generateAdapter = true)
data class NavItem(
    val id: String,
    val label: String,
    val icon: String,
    val route: String? = null,
    val children: List<NavItem>? = null,
)

@JsonClass(generateAdapter = true)
data class Tile(
    val id: String,
    val label: String,
    val icon: String,
    val color: String,
    val route: String,
)

@JsonClass(generateAdapter = true)
data class HeartbeatRequest(
    @Json(name = "app_version") val appVersion: String?,
)

// ── Retrofit Interface ────────────────────────────────────────────────────────

interface GoPilotApi {

    // Gerät registrieren (kein Auth-Header nötig)
    @POST("mde/device/register")
    suspend fun registerDevice(@Body body: DeviceRegisterRequest): Response<DeviceRegisterResponse>

    // Heartbeat
    @POST("mde/device/heartbeat")
    suspend fun heartbeat(
        @Header("Authorization") token: String,
        @Body body: HeartbeatRequest,
    ): Response<Unit>

    // Mitarbeiter Login
    @POST("mde/auth/login")
    suspend fun loginEmployee(
        @Header("Authorization") token: String,
        @Body body: EmployeeLoginRequest,
    ): Response<EmployeeLoginResponse>

    // Mitarbeiter Logout
    @POST("mde/auth/logout")
    suspend fun logoutEmployee(
        @Header("Authorization") token: String,
    ): Response<Unit>

    // Navigation laden
    @GET("mde/navigation")
    suspend fun getNavigation(
        @Header("Authorization") token: String,
        @Query("employee_ulid") employeeUlid: String,
    ): Response<NavigationResponse>
}
