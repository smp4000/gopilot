package de.stationpilot.gopilot.data.repository

import de.stationpilot.gopilot.BuildConfig
import de.stationpilot.gopilot.data.api.ApiClient
import de.stationpilot.gopilot.data.api.HeartbeatRequest

enum class ConnectionStatus {
    CHECKING,       // Prüfung läuft
    CONNECTED,      // Server verbunden, Token gültig
    UNREACHABLE,    // Server nicht erreichbar (kein Netz / Server aus)
    TOKEN_INVALID,  // Token abgelaufen / Gerät deregistriert
    NO_DEVICE,      // Kein Device-Token (noch nicht eingerichtet)
}

object ConnectionChecker {

    suspend fun check(token: String?): ConnectionStatus {
        if (token == null) return ConnectionStatus.NO_DEVICE

        return try {
            val resp = ApiClient.api.heartbeat(
                token = "Bearer $token",
                body  = HeartbeatRequest(appVersion = BuildConfig.VERSION_NAME),
            )
            when (resp.code()) {
                200  -> ConnectionStatus.CONNECTED
                401  -> ConnectionStatus.TOKEN_INVALID
                403  -> ConnectionStatus.TOKEN_INVALID
                else -> ConnectionStatus.UNREACHABLE
            }
        } catch (e: Exception) {
            ConnectionStatus.UNREACHABLE
        }
    }
}
