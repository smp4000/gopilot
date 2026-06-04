package de.stationpilot.gopilot.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.stationpilot.gopilot.data.repository.SessionStore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class NavItem(
    val id: String,
    val label: String,
    val icon: String,
    val children: List<NavItem> = emptyList(),
)

data class HomeTile(
    val id: String,
    val label: String,
    val icon: String,
    val color: TileColor,
)

enum class TileColor { BLUE, GREEN, ORANGE, RED, PURPLE }

data class HomeUiState(
    val employeeName: String = "",
    val employeeTitle: String = "",
    val stationName: String = "",
    val permissions: List<String> = emptyList(),
    val navItems: List<NavItem> = emptyList(),
    val tiles: List<HomeTile> = emptyList(),
    val isReady: Boolean = false,
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val session = SessionStore(app)
    private val _ui = MutableStateFlow(HomeUiState())
    val ui: StateFlow<HomeUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                session.employeeName,
                session.stationName,
                session.data.map { it[SessionStore.KEY_EMPLOYEE_TITLE] ?: "" },
                session.data.map { it[SessionStore.KEY_PERMISSIONS] ?: "" },
            ) { name, station, title, permsStr ->
                val perms = if (permsStr.isBlank()) emptyList() else permsStr.split(",")
                HomeUiState(
                    employeeName  = name ?: "",
                    employeeTitle = title,
                    stationName   = station ?: "",
                    permissions   = perms,
                    navItems      = buildNav(perms),
                    tiles         = buildTiles(perms),
                    isReady       = true,
                )
            }.collect { state -> _ui.value = state }
        }
    }

    fun logout() {
        viewModelScope.launch {
            session.clearEmployee()
        }
    }

    // ── Navigation aufbauen ───────────────────────────────────────────────────

    private fun buildNav(perms: List<String>): List<NavItem> {
        val nav = mutableListOf<NavItem>()

        nav += NavItem("home", "Startseite", "home")

        if (has(perms, "employee.bistro", "partner.bistro")) {
            nav += NavItem("bistro", "Bistro", "restaurant", listOf(
                NavItem("bistro.orders",   "Bestellungen",   "receipt"),
                NavItem("bistro.daily",    "Tagesabschluss", "today"),
                NavItem("bistro.delivery", "Wareneingang",   "local_shipping"),
            ))
        }

        if (has(perms, "employee.shop", "partner.shop")) {
            nav += NavItem("shop", "Shop", "storefront", listOf(
                NavItem("shop.cashier",   "Kassenabschluss", "point_of_sale"),
                NavItem("shop.delivery",  "Wareneingang",    "local_shipping"),
                NavItem("shop.inventory", "Inventur",        "inventory"),
            ))
        }

        if (has(perms, "employee.station", "partner.stations")) {
            nav += NavItem("station", "Tankstelle", "local_gas_station", listOf(
                NavItem("station.shift",    "Schichtprotokoll", "assignment"),
                NavItem("station.tank",     "Tankkontrolle",    "opacity"),
                NavItem("station.incident", "Störung melden",   "warning"),
            ))
        }

        nav += NavItem("misc", "Sonstiges", "more_horiz", listOf(
            NavItem("misc.settings", "Einstellungen", "settings"),
        ))

        return nav
    }

    // ── Kacheln aufbauen ─────────────────────────────────────────────────────

    private fun buildTiles(perms: List<String>): List<HomeTile> {
        val tiles = mutableListOf<HomeTile>()
        tiles += HomeTile("shift",      "Schichtabrechnung",  "receipt_long",      TileColor.BLUE)
        if (has(perms, "employee.station.shift"))    tiles += HomeTile("tank",      "Tankbetrug",         "local_gas_station", TileColor.BLUE)
        if (has(perms, "employee.station.tank"))     tiles += HomeTile("temp",      "Temperaturen",       "thermostat",        TileColor.BLUE)
        if (has(perms, "employee.shop.inventory"))   tiles += HomeTile("inventory", "MHD-Kontrolle",      "schedule",          TileColor.BLUE)
        if (has(perms, "employee.shop.cashier"))     tiles += HomeTile("cashier",   "Kassenabschluss",    "point_of_sale",     TileColor.BLUE)
        if (has(perms, "employee.shop"))             tiles += HomeTile("abschr",    "Abschriften",        "inventory",         TileColor.BLUE)
        if (has(perms, "employee.station.incident")) tiles += HomeTile("incident",  "Störung melden",     "warning",           TileColor.RED)
        if (has(perms, "employee.bistro"))           tiles += HomeTile("bistro",    "Bistro",             "restaurant",        TileColor.ORANGE)
        if (has(perms, "partner.keys"))              tiles += HomeTile("keys",      "Schlüssel",          "key",               TileColor.PURPLE)
        // Wenn keine spez. Permissions → Standard-Kacheln zeigen
        if (tiles.size <= 1) {
            tiles += HomeTile("tank",      "Tankbetrug",         "local_gas_station", TileColor.BLUE)
            tiles += HomeTile("temp",      "Temperaturen",       "thermostat",        TileColor.BLUE)
            tiles += HomeTile("inventory", "MHD-Kontrolle",      "schedule",          TileColor.BLUE)
            tiles += HomeTile("abschr",    "Abschriften",        "inventory",         TileColor.BLUE)
            tiles += HomeTile("info",      "Artikelinfo",        "receipt_long",      TileColor.BLUE)
        }
        return tiles
    }

    private fun has(perms: List<String>, vararg prefixes: String): Boolean =
        perms.isEmpty() || prefixes.any { prefix -> perms.any { it.startsWith(prefix) } }
}
