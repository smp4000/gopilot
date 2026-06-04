package de.stationpilot.gopilot.ui.home

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.stationpilot.gopilot.ui.theme.*
import kotlinx.coroutines.launch

// ── Info-Banner Modell ────────────────────────────────────────────────────────

data class InfoBanner(
    val id: String,
    val message: String,
    val type: BannerType,  // ERROR | WARNING | INFO
)

enum class BannerType { ERROR, WARNING, INFO }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    vm: HomeViewModel = viewModel(),
) {
    val ui by vm.ui.collectAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Beispiel-Banner (später dynamisch aus API)
    var banners by remember {
        mutableStateOf(
            listOf(
                InfoBanner("shift", "Schicht läuft seit über 8 Stunden.", BannerType.WARNING),
            )
        )
    }

    LaunchedEffect(ui.isReady) {
        if (ui.isReady && ui.employeeName.isEmpty()) onLogout()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            NavigationDrawerContent(
                ui = ui,
                onLogout = { vm.logout(); onLogout() },
                onClose = { scope.launch { drawerState.close() } },
            )
        },
    ) {
        Scaffold(
            topBar = {
                // ── App Bar ────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BluePrimary)
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, null, tint = Color.White)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "GoPilot",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                            )
                            if (ui.stationName.isNotEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocationOn, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(12.dp))
                                    Spacer(Modifier.width(2.dp))
                                    Text(ui.stationName, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                                }
                            }
                        }
                        IconButton(onClick = { vm.logout(); onLogout() }) {
                            Icon(Icons.Default.ExitToApp, null, tint = Color.White)
                        }
                    }
                }
            },
            containerColor = Color(0xFFF2F5FA),
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                // ── Begrüßung ──────────────────────────────────────────────
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                    ) {
                        Text(
                            text = "Hallo, ${ui.employeeName}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A2E),
                        )
                        if (ui.stationName.isNotEmpty()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = ui.stationName,
                                fontSize = 13.sp,
                                color = Color(0xFF6B7280),
                            )
                        }
                    }
                    HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 0.5.dp)
                }

                // ── Info-Banner ────────────────────────────────────────────
                if (banners.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            banners.forEach { banner ->
                                AnimatedVisibility(
                                    visible = true,
                                    exit = fadeOut() + slideOutHorizontally(),
                                ) {
                                    InfoBannerCard(
                                        banner = banner,
                                        onDismiss = { banners = banners.filter { it.id != banner.id } },
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Kacheln Überschrift ────────────────────────────────────
                item {
                    Spacer(Modifier.height(4.dp))
                }

                // ── Kacheln ────────────────────────────────────────────────
                item {
                    if (ui.tiles.isNotEmpty()) {
                        TileGrid(tiles = ui.tiles)
                    } else {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.GridView, null, tint = Color(0xFFD1D5DB), modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("Keine Kacheln verfügbar", color = Color(0xFF9CA3AF), fontSize = 14.sp)
                                Text("Kontaktiere deinen Administrator", color = Color(0xFFD1D5DB), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Info-Banner ───────────────────────────────────────────────────────────────

@Composable
fun InfoBannerCard(banner: InfoBanner, onDismiss: () -> Unit) {
    val (bg, border, icon, textColor) = when (banner.type) {
        BannerType.ERROR   -> listOf(Color(0xFFFEF2F2), Color(0xFFFCA5A5), Icons.Default.ErrorOutline,   Color(0xFFB91C1C))
        BannerType.WARNING -> listOf(Color(0xFFFFFBEB), Color(0xFFFCD34D), Icons.Default.WarningAmber,   Color(0xFF92400E))
        BannerType.INFO    -> listOf(Color(0xFFEFF6FF), Color(0xFF93C5FD), Icons.Default.InfoOutlined,    Color(0xFF1E40AF))
    }

    @Suppress("UNCHECKED_CAST")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg as Color)
            .clickable { }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon as ImageVector, null, tint = textColor as Color, modifier = Modifier.size(20.dp))
        Text(
            text = banner.message,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            color = textColor,
            lineHeight = 18.sp,
        )
        IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Close, null, tint = textColor.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
        }
    }
}

// ── Kachel Grid ───────────────────────────────────────────────────────────────

@Composable
fun TileGrid(tiles: List<HomeTile>) {
    val rows = tiles.chunked(2)
    Column(
        modifier = Modifier.padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        rows.forEach { rowTiles ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                rowTiles.forEach { tile ->
                    TileCard(tile = tile, modifier = Modifier.weight(1f))
                }
                // Leer-Placeholder wenn ungerade Anzahl
                if (rowTiles.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun TileCard(tile: HomeTile, modifier: Modifier = Modifier) {
    val iconColor = when (tile.color) {
        TileColor.BLUE   -> BluePrimary
        TileColor.GREEN  -> Color(0xFF16A34A)
        TileColor.ORANGE -> Color(0xFFEA580C)
        TileColor.RED    -> Color(0xFFDC2626)
        TileColor.PURPLE -> Color(0xFF7C3AED)
    }
    val iconBg = iconColor.copy(alpha = 0.1f)

    Card(
        modifier = modifier
            .height(100.dp)
            .clickable { },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Icon in farbigem Kreis
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                TileIcon(tile.icon, iconColor)
            }
            // Label
            Text(
                text = tile.label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1F2937),
                lineHeight = 16.sp,
            )
        }
    }
}

@Composable
fun TileIcon(name: String, tint: Color) {
    val icon = when (name) {
        "play_circle"       -> Icons.Default.PlayCircle
        "key"               -> Icons.Default.Key
        "local_gas_station" -> Icons.Default.LocalGasStation
        "warning"           -> Icons.Default.Warning
        "point_of_sale"     -> Icons.Default.PointOfSale
        "restaurant"        -> Icons.Default.Restaurant
        "assignment"        -> Icons.Default.Assignment
        "thermostat"        -> Icons.Default.Thermostat
        "inventory"         -> Icons.Default.Inventory
        "schedule"          -> Icons.Default.Schedule
        "receipt_long"      -> Icons.Default.ReceiptLong
        else                -> Icons.Default.Apps
    }
    Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
}

// ── Navigation Drawer ─────────────────────────────────────────────────────────

@Composable
fun NavigationDrawerContent(
    ui: HomeUiState,
    onLogout: () -> Unit,
    onClose: () -> Unit,
) {
    ModalDrawerSheet(
        modifier = Modifier.width(290.dp),
        drawerContainerColor = Color.White,
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(BluePrimary, Color(0xFF1565C0))))
                .padding(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(30.dp))
                }
                Column {
                    Text(ui.employeeName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    Text(ui.stationName, fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // Nav Items
        LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
            items(ui.navItems) { item ->
                DrawerNavItem(item = item, onClose = onClose)
            }
        }

        HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)

        // Footer
        Column(modifier = Modifier.padding(8.dp)) {
            DrawerFooterItem(Icons.Default.Settings, "Einstellungen", Color(0xFF374151), onClose)
            DrawerFooterItem(Icons.Default.Logout, "Abmelden", Color(0xFFDC2626), onLogout)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun DrawerNavItem(item: NavItem, onClose: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val navIcon = navIconFor(item.icon)

    if (item.children.isEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (item.id == "home") BluePrimary.copy(alpha = 0.1f) else Color.Transparent)
                .clickable { onClose() }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(navIcon, null, tint = if (item.id == "home") BluePrimary else Color(0xFF6B7280), modifier = Modifier.size(20.dp))
            Text(item.label, fontSize = 14.sp, fontWeight = if (item.id == "home") FontWeight.SemiBold else FontWeight.Normal, color = if (item.id == "home") BluePrimary else Color(0xFF374151))
        }
    } else {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(navIcon, null, tint = Color(0xFF6B7280), modifier = Modifier.size(20.dp))
                Text(item.label, modifier = Modifier.weight(1f), fontSize = 14.sp, color = Color(0xFF374151))
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(18.dp),
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(start = 48.dp, end = 8.dp)) {
                    item.children.forEach { child ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onClose() }
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(Color(0xFF9CA3AF)))
                            Text(child.label, fontSize = 13.sp, color = Color(0xFF4B5563))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DrawerFooterItem(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        Text(label, fontSize = 14.sp, color = color, fontWeight = FontWeight.Medium)
    }
}

fun navIconFor(name: String): ImageVector = when (name) {
    "home"              -> Icons.Default.Home
    "restaurant"        -> Icons.Default.Restaurant
    "storefront"        -> Icons.Default.Storefront
    "local_gas_station" -> Icons.Default.LocalGasStation
    "more_horiz"        -> Icons.Default.MoreHoriz
    "settings"          -> Icons.Default.Settings
    else                -> Icons.Default.Circle
}
