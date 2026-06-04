package de.stationpilot.gopilot.ui.admin

import android.app.Application
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.stationpilot.gopilot.data.api.ApiClient
import de.stationpilot.gopilot.data.api.EmployeeItem
import de.stationpilot.gopilot.data.api.SaveNfcRequest
import de.stationpilot.gopilot.data.api.StationItem
import de.stationpilot.gopilot.data.repository.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class NfcWriteStatus {
    IDLE,           // Warten auf Auswahl
    WAITING,        // Warten auf NFC-Chip
    WRITING,        // Chip wird beschrieben
    SUCCESS,        // Erfolgreich geschrieben
    ERROR,          // Fehler
}

data class NfcWriterUiState(
    val isLoadingStations: Boolean = false,
    val isLoadingEmployees: Boolean = false,
    val stations: List<StationItem> = emptyList(),
    val employees: List<EmployeeItem> = emptyList(),
    val selectedStation: StationItem? = null,
    val selectedEmployee: EmployeeItem? = null,
    val nfcAvailable: Boolean = false,
    val writeStatus: NfcWriteStatus = NfcWriteStatus.IDLE,
    val statusMessage: String = "",
    val lastWrittenUid: String = "",
    val error: String? = null,
)

class NfcWriterViewModel(app: Application) : AndroidViewModel(app) {

    private val session = SessionStore(app)
    private val _ui = MutableStateFlow(NfcWriterUiState())
    val ui: StateFlow<NfcWriterUiState> = _ui.asStateFlow()

    init {
        val nfcAvail = NfcAdapter.getDefaultAdapter(app) != null
        _ui.update { it.copy(nfcAvailable = nfcAvail) }
        loadStations()
    }

    private fun loadStations() {
        viewModelScope.launch {
            _ui.update { it.copy(isLoadingStations = true, error = null) }
            try {
                val token = session.deviceToken.first() ?: return@launch
                val resp = ApiClient.api.getStations("Bearer $token")
                if (resp.isSuccessful) {
                    _ui.update { it.copy(stations = resp.body()?.stations ?: emptyList(), isLoadingStations = false) }
                } else {
                    _ui.update { it.copy(isLoadingStations = false, error = "Stationen konnten nicht geladen werden.") }
                }
            } catch (e: Exception) {
                _ui.update { it.copy(isLoadingStations = false, error = "Keine Serververbindung.") }
            }
        }
    }

    fun selectStation(station: StationItem) {
        _ui.update { it.copy(selectedStation = station, selectedEmployee = null, employees = emptyList()) }
        loadEmployees(station.ulid)
    }

    private fun loadEmployees(stationUlid: String) {
        viewModelScope.launch {
            _ui.update { it.copy(isLoadingEmployees = true) }
            try {
                val token = session.deviceToken.first() ?: return@launch
                val resp = ApiClient.api.getEmployees("Bearer $token", stationUlid)
                if (resp.isSuccessful) {
                    _ui.update { it.copy(employees = resp.body()?.employees ?: emptyList(), isLoadingEmployees = false) }
                } else {
                    _ui.update { it.copy(isLoadingEmployees = false, error = "Mitarbeiter konnten nicht geladen werden.") }
                }
            } catch (e: Exception) {
                _ui.update { it.copy(isLoadingEmployees = false, error = "Keine Serververbindung.") }
            }
        }
    }

    fun selectEmployee(employee: EmployeeItem) {
        _ui.update { it.copy(selectedEmployee = employee, writeStatus = NfcWriteStatus.WAITING, statusMessage = "Chip an das Gerät halten...", error = null) }
    }

    fun cancelWrite() {
        _ui.update { it.copy(writeStatus = NfcWriteStatus.IDLE, statusMessage = "", selectedEmployee = null) }
    }

    /**
     * NFC-Tag empfangen — Chip beschreiben und in DB speichern.
     */
    fun onNfcTag(tag: Tag) {
        val employee = _ui.value.selectedEmployee ?: return
        if (_ui.value.writeStatus != NfcWriteStatus.WAITING) return

        _ui.update { it.copy(writeStatus = NfcWriteStatus.WRITING, statusMessage = "Chip wird beschrieben...") }

        viewModelScope.launch {
            try {
                // NFC-UID aus Tag auslesen
                val uid = tag.id.joinToString("") { "%02X".format(it) }

                // NDEF-Nachricht schreiben (enthält scan_code des Mitarbeiters)
                val scanCode = employee.scanCode ?: uid
                val ndefWriteSuccess = writeNdef(tag, scanCode)

                if (!ndefWriteSuccess) {
                    _ui.update { it.copy(writeStatus = NfcWriteStatus.ERROR, statusMessage = "", error = "Chip konnte nicht beschrieben werden. Bitte erneut versuchen.") }
                    return@launch
                }

                // UID + scan_code in DB speichern
                val token = session.deviceToken.first() ?: return@launch
                val resp = ApiClient.api.saveNfc(
                    token = "Bearer $token",
                    employeeUlid = employee.ulid,
                    body = SaveNfcRequest(nfcUid = uid, scanCode = scanCode),
                )

                if (resp.isSuccessful) {
                    _ui.update {
                        it.copy(
                            writeStatus = NfcWriteStatus.SUCCESS,
                            statusMessage = "✓ Chip erfolgreich beschrieben!",
                            lastWrittenUid = uid,
                            error = null,
                            // Mitarbeiter in Liste als "hat NFC" markieren
                            employees = it.employees.map { e ->
                                if (e.ulid == employee.ulid) e.copy(hasNfc = true, nfcUid = uid) else e
                            },
                        )
                    }
                } else if (resp.code() == 409) {
                    _ui.update { it.copy(writeStatus = NfcWriteStatus.ERROR, statusMessage = "", error = resp.errorBody()?.string() ?: "NFC-UID bereits vergeben.") }
                } else {
                    _ui.update { it.copy(writeStatus = NfcWriteStatus.ERROR, statusMessage = "", error = "Fehler beim Speichern in der Datenbank.") }
                }
            } catch (e: Exception) {
                _ui.update { it.copy(writeStatus = NfcWriteStatus.ERROR, statusMessage = "", error = "Fehler: ${e.message}") }
            }
        }
    }

    fun resetAfterSuccess() {
        _ui.update { it.copy(writeStatus = NfcWriteStatus.IDLE, statusMessage = "", selectedEmployee = null, lastWrittenUid = "") }
    }

    // ── NDEF schreiben ────────────────────────────────────────────────────────

    private fun writeNdef(tag: Tag, content: String): Boolean {
        return try {
            val ndefMessage = android.nfc.NdefMessage(
                arrayOf(android.nfc.NdefRecord.createTextRecord("de", content))
            )

            // Versuch 1: Tag ist bereits NDEF-formatiert
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                ndef.writeNdefMessage(ndefMessage)
                ndef.close()
                return true
            }

            // Versuch 2: Tag muss erst formatiert werden
            val formatable = NdefFormatable.get(tag)
            if (formatable != null) {
                formatable.connect()
                formatable.format(ndefMessage)
                formatable.close()
                return true
            }

            false
        } catch (e: Exception) {
            false
        }
    }
}
