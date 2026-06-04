package de.stationpilot.gopilot.ui.setup

import android.app.Application
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.stationpilot.gopilot.BuildConfig
import de.stationpilot.gopilot.data.api.ApiClient
import de.stationpilot.gopilot.data.api.DeviceRegisterRequest
import de.stationpilot.gopilot.data.repository.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SetupUiState(
    val isLoading: Boolean = false,
    val stationCode: String = "",  // ULID aus QR-Scan
    val deviceName: String = "",   // z.B. "Kasse 1"
    val error: String? = null,
    val success: Boolean = false,
    val isRegistered: Boolean = false,  // Gerät bereits eingerichtet?
)

class SetupViewModel(app: Application) : AndroidViewModel(app) {

    private val session = SessionStore(app)
    private val _ui = MutableStateFlow(SetupUiState())
    val ui: StateFlow<SetupUiState> = _ui.asStateFlow()

    init {
        // Prüfen ob Gerät bereits registriert ist
        viewModelScope.launch {
            val token = session.deviceToken.first()
            _ui.update { it.copy(isRegistered = token != null) }
        }
    }

    fun onStationCodeChange(code: String) {
        _ui.update { it.copy(stationCode = code, error = null) }
    }

    fun onDeviceNameChange(name: String) {
        _ui.update { it.copy(deviceName = name, error = null) }
    }

    fun onQrScanned(code: String) {
        _ui.update { it.copy(stationCode = code.trim(), error = null) }
    }

    fun register() {
        val state = _ui.value
        if (state.stationCode.isBlank()) {
            _ui.update { it.copy(error = "Bitte Station-QR scannen oder Code eingeben.") }
            return
        }
        if (state.deviceName.isBlank()) {
            _ui.update { it.copy(error = "Bitte Gerätename eingeben (z.B. \"Kasse 1\").") }
            return
        }

        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true, error = null) }

            try {
                val androidId = Settings.Secure.getString(
                    getApplication<Application>().contentResolver,
                    Settings.Secure.ANDROID_ID,
                )

                val resp = ApiClient.api.registerDevice(
                    DeviceRegisterRequest(
                        stationCode = state.stationCode.trim(),
                        deviceName  = state.deviceName.trim(),
                        deviceModel = android.os.Build.MODEL,
                        androidId   = androidId,
                        appVersion  = BuildConfig.VERSION_NAME,
                    )
                )

                if (resp.isSuccessful && resp.body() != null) {
                    val body = resp.body()!!
                    session.saveDevice(body.deviceToken, body.device)
                    _ui.update { it.copy(isLoading = false, success = true, isRegistered = true) }
                } else {
                    val msg = when (resp.code()) {
                        404 -> "Station nicht gefunden. QR-Code prüfen."
                        403 -> "Zugriff verweigert."
                        else -> "Registrierung fehlgeschlagen (${resp.code()})."
                    }
                    _ui.update { it.copy(isLoading = false, error = msg) }
                }
            } catch (e: Exception) {
                _ui.update { it.copy(isLoading = false, error = "Keine Serververbindung.") }
            }
        }
    }
}
