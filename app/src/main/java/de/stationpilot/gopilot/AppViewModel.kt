package de.stationpilot.gopilot

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.stationpilot.gopilot.data.repository.ConnectionChecker
import de.stationpilot.gopilot.data.repository.ConnectionStatus
import de.stationpilot.gopilot.data.repository.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppState(
    val isReady: Boolean = false,           // Initialisierung abgeschlossen
    val startDestination: String = "splash",
    val connectionStatus: ConnectionStatus = ConnectionStatus.CHECKING,
    val stationName: String = "",
    val stationCity: String = "",
    val deviceToken: String? = null,
)

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val session = SessionStore(app)

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    init {
        initialize()
    }

    fun initialize() {
        viewModelScope.launch {
            _state.update { it.copy(isReady = false, connectionStatus = ConnectionStatus.CHECKING) }

            val token       = session.deviceToken.first()
            val stationName = session.stationName.first() ?: ""
            val stationCity = session.stationCity.first() ?: ""

            _state.update { it.copy(deviceToken = token, stationName = stationName, stationCity = stationCity) }

            val status = ConnectionChecker.check(token)

            val destination = when (status) {
                ConnectionStatus.NO_DEVICE    -> "setup"
                ConnectionStatus.TOKEN_INVALID -> "setup"   // Token ungültig → neu einrichten
                ConnectionStatus.CONNECTED    -> "login"
                ConnectionStatus.UNREACHABLE  -> "login"    // Offline → trotzdem Login zeigen mit Warnung
                ConnectionStatus.CHECKING     -> "login"
            }

            _state.update {
                it.copy(
                    isReady          = true,
                    connectionStatus = status,
                    startDestination = destination,
                )
            }
        }
    }

    fun retryConnection() {
        viewModelScope.launch {
            _state.update { it.copy(connectionStatus = ConnectionStatus.CHECKING) }
            val token  = _state.value.deviceToken
            val status = ConnectionChecker.check(token)
            _state.update { it.copy(connectionStatus = status) }
        }
    }

    fun onDeviceRegistered() {
        // Nach erfolgreicher Registrierung neu prüfen
        initialize()
    }
}
