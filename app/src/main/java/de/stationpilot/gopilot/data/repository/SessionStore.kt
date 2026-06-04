package de.stationpilot.gopilot.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import de.stationpilot.gopilot.data.api.DeviceInfo
import de.stationpilot.gopilot.data.api.EmployeeInfo
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "gopilot_session")

/**
 * Persistente Sitzungsdaten:
 *  - Device-Token (dauerhaft nach Registrierung)
 *  - Station-Infos
 *  - Aktuell eingeloggter Mitarbeiter (flüchtig, wird bei Abmelden gelöscht)
 */
class SessionStore(private val context: Context) {

    companion object {
        val KEY_DEVICE_TOKEN    = stringPreferencesKey("device_token")
        val KEY_DEVICE_ULID     = stringPreferencesKey("device_ulid")
        val KEY_DEVICE_NAME     = stringPreferencesKey("device_name")
        val KEY_STATION_ULID    = stringPreferencesKey("station_ulid")
        val KEY_STATION_NAME    = stringPreferencesKey("station_name")
        val KEY_STATION_CITY    = stringPreferencesKey("station_city")
        val KEY_TENANT_NAME     = stringPreferencesKey("tenant_name")
        val KEY_EMPLOYEE_ULID   = stringPreferencesKey("employee_ulid")
        val KEY_EMPLOYEE_NAME   = stringPreferencesKey("employee_name")
        val KEY_EMPLOYEE_TITLE  = stringPreferencesKey("employee_title")
        val KEY_PERMISSIONS     = stringPreferencesKey("permissions")  // JSON-Array als String
    }

    val data: Flow<Preferences> = context.dataStore.data
    val deviceToken: Flow<String?> = context.dataStore.data.map { it[KEY_DEVICE_TOKEN] }
    val stationName: Flow<String?> = context.dataStore.data.map { it[KEY_STATION_NAME] }
    val stationCity: Flow<String?> = context.dataStore.data.map { it[KEY_STATION_CITY] }
    val tenantName:  Flow<String?> = context.dataStore.data.map { it[KEY_TENANT_NAME] }
    val employeeUlid:Flow<String?> = context.dataStore.data.map { it[KEY_EMPLOYEE_ULID] }
    val employeeName:Flow<String?> = context.dataStore.data.map { it[KEY_EMPLOYEE_NAME] }

    suspend fun saveDevice(token: String, device: DeviceInfo) {
        context.dataStore.edit {
            it[KEY_DEVICE_TOKEN]  = token
            it[KEY_DEVICE_ULID]   = device.ulid
            it[KEY_DEVICE_NAME]   = device.deviceName
            it[KEY_STATION_ULID]  = device.station.ulid
            it[KEY_STATION_NAME]  = device.station.name
            it[KEY_STATION_CITY]  = device.station.city ?: ""
            it[KEY_TENANT_NAME]   = device.tenant.name
        }
    }

    suspend fun saveEmployee(employee: EmployeeInfo, permissions: List<String>) {
        context.dataStore.edit {
            it[KEY_EMPLOYEE_ULID]  = employee.ulid
            it[KEY_EMPLOYEE_NAME]  = employee.name
            it[KEY_EMPLOYEE_TITLE] = employee.jobTitle ?: ""
            it[KEY_PERMISSIONS]    = permissions.joinToString(",")
        }
    }

    suspend fun clearEmployee() {
        context.dataStore.edit {
            it.remove(KEY_EMPLOYEE_ULID)
            it.remove(KEY_EMPLOYEE_NAME)
            it.remove(KEY_EMPLOYEE_TITLE)
            it.remove(KEY_PERMISSIONS)
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
