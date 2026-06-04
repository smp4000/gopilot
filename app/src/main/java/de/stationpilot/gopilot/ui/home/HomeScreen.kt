package de.stationpilot.gopilot.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.stationpilot.gopilot.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    vm: HomeViewModel = viewModel(),
) {
    val ui by vm.ui.collectAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Wenn kein Mitarbeiter mehr eingeloggt → zurück zum Login
    LaunchedEffect(ui.employeeName) {
        if (ui.isReady && ui.employeeName.isEmpty()) onLogout()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            NavigationDrawerContent(
                ui = ui,
                onLogout = {
                    vm.logout()
                    onLogout()
                },
                onClose = { scope.launch { drawerState.close() } },
            )
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("GoPilot", style = MaterialTheme.typography.titleLarge, color = Color.White)
                            if (ui.stationName.isNotEmpty()) {
                                Text(ui.stationName, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menü", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = BluePrimary,
                    ),
                )
            },
        ) { padding ->
            DashboardContent(ui = ui, modifier = Modifier.padding(padding))
        }
    }
}

// ── Navigation Drawer ─────────────────────────────────────────────────────────

@Composable
fun NavigationDrawerContent(
    ui: HomeUiState,
    onLogout: () -> Unit,
    onClose: () -> Unit,
) {
    ModalDrawerSheet(
        modifier = Modifier.width(300.dp),
        drawerContainerColor = Color.White,
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(BluePrimary, Color(0xFF1976D2))))
                .padding(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Text(ui.employeeName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                Text(ui.stationName, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
            }
        }

        Spacer(Modifier.height(8.dp))

        // Navigation Items
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(ui.navItems) { item ->
                if (item.id == "misc.settings" || item.id == "home") return@items
                NavDrawerItem(item = item, onClose = onClose)
            }
        }

        HorizontalDivider()

        // Einstellungen + Abmelden
        NavigationDrawerItem(
            label = { Text("Einstellungen") },
            selected = false,
            onClick = onClose,
            icon = { Icon(Icons.Default.Settings, null) },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
        NavigationDrawerItem(
            label = { Text("Abmelden", color = ErrorColor) },
            selected = false,
            onClick = onLogout,
            icon = { Icon(Icons.Default.Logout, null, tint = ErrorColor) },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun NavDrawerItem(item: NavItem, onClose: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    if (item.children.isEmpty()) {
        NavigationDrawerItem(
            label = { Text(item.label) },
            selected = item.id == "home",
            onClick = onClose,
            icon = { NavIcon(item.icon) },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
        )
    } else {
        // Erweiterbar
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                NavIcon(item.icon)
                Text(item.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null,
                    tint = OnSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp),
                )
            }

            if (expanded) {
                item.children.forEach { child ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onClose() }
                            .padding(start = 56.dp, end = 24.dp, top = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        NavIcon(child.icon, size = 18)
                        Text(child.label, style = MaterialTheme.typography.bodyMedium, color = OnSurface.copy(alpha = 0.8f))
                    }
                }
            }
        }
    }
}

@Composable
fun NavIcon(name: String, size: Int = 22) {
    val icon = when (name) {
        "home"              -> Icons.Default.Home
        "restaurant"        -> Icons.Default.Restaurant
        "storefront"        -> Icons.Default.Storefront
        "local_gas_station" -> Icons.Default.LocalGasStation
        "more_horiz"        -> Icons.Default.MoreHoriz
        "settings"          -> Icons.Default.Settings
        "receipt"           -> Icons.Default.Receipt
        "today"             -> Icons.Default.Today
        "local_shipping"    -> Icons.Default.LocalShipping
        "point_of_sale"     -> Icons.Default.PointOfSale
        "inventory"         -> Icons.Default.Inventory
        "assignment"        -> Icons.Default.Assignment
        "opacity"           -> Icons.Default.Opacity
        "warning"           -> Icons.Default.Warning
        else                -> Icons.Default.Circle
    }
    Icon(icon, null, tint = BluePrimary, modifier = Modifier.size(size.dp))
}

// ── Dashboard ─────────────────────────────────────────────────────────────────

@Composable
fun DashboardContent(ui: HomeUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BlueBackground)
            .navigationBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Begrüßung
        if (ui.employeeName.isNotEmpty()) {
            Text(
                "Hallo, ${ui.employeeName.split(" ").first()}!",
                style = MaterialTheme.typography.headlineMedium,
                color = OnSurface,
            )
        }

        // Kacheln
        if (ui.tiles.isNotEmpty()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(ui.tiles) { tile ->
                    DashboardTile(tile = tile)
                }
            }
        }
    }
}

@Composable
fun DashboardTile(tile: HomeTile) {
    val bgColor = when (tile.color) {
        TileColor.BLUE   -> BluePrimary
        TileColor.GREEN  -> SuccessColor
        TileColor.ORANGE -> Color(0xFFE65100)
        TileColor.RED    -> ErrorColor
        TileColor.PURPLE -> Color(0xFF6A1B9A)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable { },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(4.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            NavIcon(tile.icon, size = 28)
            Text(tile.label, style = MaterialTheme.typography.labelLarge, color = Color.White)
        }
    }
}
