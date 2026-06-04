package de.stationpilot.gopilot.ui.login

import android.app.Application
import android.nfc.NfcAdapter
import android.nfc.Tag
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.stationpilot.gopilot.data.api.ApiClient
import de.stationpilot.gopilot.data.api.EmployeeLoginRequest
import de.stationpilot.gopilot.data.repository.SessionStore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val codeInput: String = "",
    val showCode: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val stationName: String = "",
    val stationCity: String = "",
    val nfcAvailable: Boolean = false,
    val scannerAvailable: Boolean = true,
)

class LoginViewModel(app: Application) : AndroidViewModel(app) {

    private val session = SessionStore(app)
    private val _ui = MutableStateFlow(LoginUiState())
    val ui: StateFlow<LoginUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            combine(session.stationName, session.stationCity) { name, city ->
                Pair(name ?: "", city ?: "")
            }.collect { (name, city) ->
                _ui.update { it.copy(stationName = name, stationCity = city) }
            }
        }

        val nfcAvail = NfcAdapter.getDefaultAdapter(app) != null
        _ui.update { it.copy(nfcAvailable = nfcAvail) }
    }

    fun onCodeChange(code: String) {
        _ui.update { it.copy(codeInput = code, error = null) }
    }

    fun toggleShowCode() {
        _ui.update { it.copy(showCode = !it.showCode) }
    }

    fun loginWithPin() {
        val pin = _ui.value.codeInput.trim()
        if (pin.isEmpty()) {
            _ui.update { it.copy(error = "Bitte Code eingeben.") }
            return
        }
        doLogin("pin", pin)
    }

    fun loginWithScan(code: String) {
        doLogin("scan", code)
    }

    fun loginWithNfc(tag: Tag) {
        // NFC-UID als Hex-String — erst ins Feld schreiben, dann einloggen
        val uid = tag.id.joinToString("") { "%02X".format(it) }
        _ui.update { it.copy(codeInput = uid, error = null) }
        doLogin("nfc", uid)
    }

    fun clearError() {
        _ui.update { it.copy(error = null) }
    }

    private fun doLogin(method: String, value: String) {
        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true, error = null) }

            try {
                val token = session.deviceToken.first()
                if (token == null) {
                    _ui.update { it.copy(isLoading = false, error = "Gerät nicht registriert.") }
                    return@launch
                }

                val resp = ApiClient.api.loginEmployee(
                    token = "Bearer $token",
                    body  = EmployeeLoginRequest(method = method, value = value),
                )

                if (resp.isSuccessful && resp.body() != null) {
                    val body = resp.body()!!
                    session.saveEmployee(body.employee, body.permissions)
                    _ui.update { it.copy(isLoading = false, success = true, codeInput = "") }
                } else {
                    val msg = when (resp.code()) {
                        401 -> "Code nicht erkannt. Bitte erneut scannen."
                        403 -> "Mitarbeiter ist inaktiv."
                        else -> "Anmeldung fehlgeschlagen."
                    }
                    _ui.update { it.copy(isLoading = false, error = msg) }
                }
            } catch (e: Exception) {
                _ui.update { it.copy(isLoading = false, error = "Keine Serververbindung.") }
            }
        }
    }
}
