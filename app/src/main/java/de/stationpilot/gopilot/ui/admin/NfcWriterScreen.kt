package de.stationpilot.gopilot.ui.admin

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.stationpilot.gopilot.data.api.EmployeeItem
import de.stationpilot.gopilot.data.api.StationItem
import de.stationpilot.gopilot.ui.theme.*

@Composable
fun NfcWriterScreen(
    onBack: () -> Unit,
    vm: NfcWriterViewModel = viewModel(),
) {
    val ui by vm.ui.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BlueBackground)
            .navigationBarsPadding(),
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(Color(0xFF6A1B9A), Color(0xFF8E24AA))))
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                }
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.Nfc, null, tint = Color.White, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("NFC-Chips beschreiben", style = MaterialTheme.typography.titleLarge, color = Color.White)
                    Text("Admin-Bereich", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                }
            }
        }

        // ── Fehler Banner ─────────────────────────────────────────────────────
        if (ui.error != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFEBEE))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Default.ErrorOutline, null, tint = ErrorColor, modifier = Modifier.size(18.dp))
                Text(ui.error ?: "", style = MaterialTheme.typography.bodySmall, color = ErrorColor, modifier = Modifier.weight(1f))
                IconButton(onClick = { vm.cancelWrite() }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, null, tint = ErrorColor, modifier = Modifier.size(16.dp))
                }
            }
        }

        // ── Inhalt ────────────────────────────────────────────────────────────
        when (ui.writeStatus) {
            NfcWriteStatus.WAITING, NfcWriteStatus.WRITING -> {
                NfcWaitingCard(
                    employee = ui.selectedEmployee!!,
                    isWriting = ui.writeStatus == NfcWriteStatus.WRITING,
                    onCancel = vm::cancelWrite,
                )
            }
            NfcWriteStatus.SUCCESS -> {
                NfcSuccessCard(
                    employee = ui.selectedEmployee!!,
                    uid = ui.lastWrittenUid,
                    onNext = vm::resetAfterSuccess,
                )
            }
            else -> {
                SelectionContent(
                    ui = ui,
                    onStationSelect = vm::selectStation,
                    onEmployeeSelect = vm::selectEmployee,
                )
            }
        }
    }
}

@Composable
private fun SelectionContent(
    ui: NfcWriterUiState,
    onStationSelect: (StationItem) -> Unit,
    onEmployeeSelect: (EmployeeItem) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Stationen
        item {
            Text("1. Station auswählen", style = MaterialTheme.typography.titleMedium, color = OnSurface)
        }

        if (ui.isLoadingStations) {
            item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
        } else {
            items(ui.stations) { station ->
                StationCard(
                    station = station,
                    isSelected = ui.selectedStation?.ulid == station.ulid,
                    onClick = { onStationSelect(station) },
                )
            }
        }

        // Mitarbeiter (nur wenn Station gewählt)
        if (ui.selectedStation != null) {
            item {
                Spacer(Modifier.height(4.dp))
                Text("2. Mitarbeiter auswählen", style = MaterialTheme.typography.titleMedium, color = OnSurface)
                Text(
                    "Mitarbeiter antippen → Chip anhalten",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurface.copy(alpha = 0.6f),
                )
            }

            if (ui.isLoadingEmployees) {
                item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            } else if (ui.employees.isEmpty()) {
                item {
                    Text("Keine Mitarbeiter gefunden.", style = MaterialTheme.typography.bodyMedium, color = OnSurface.copy(alpha = 0.5f))
                }
            } else {
                items(ui.employees) { employee ->
                    EmployeeCard(
                        employee = employee,
                        onClick = { onEmployeeSelect(employee) },
                    )
                }
            }
        }
    }
}

@Composable
private fun StationCard(station: StationItem, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .then(
                if (isSelected) Modifier.border(2.dp, BluePrimary, RoundedCornerShape(14.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) BlueContainer else Color.White,
        ),
        elevation = CardDefaults.cardElevation(if (isSelected) 4.dp else 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Default.LocalGasStation,
                null,
                tint = if (isSelected) BluePrimary else OnSurface.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(station.name, style = MaterialTheme.typography.titleSmall, color = OnSurface)
                if (!station.city.isNullOrEmpty()) {
                    Text(station.city, style = MaterialTheme.typography.bodySmall, color = OnSurface.copy(alpha = 0.6f))
                }
            }
            if (isSelected) Icon(Icons.Default.CheckCircle, null, tint = BluePrimary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun EmployeeCard(employee: EmployeeItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (employee.hasNfc) Color(0xFFE8F5E9) else BlueContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (employee.hasNfc) Icons.Default.Nfc else Icons.Default.Person,
                    null,
                    tint = if (employee.hasNfc) SuccessColor else BluePrimary,
                    modifier = Modifier.size(22.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(employee.name, style = MaterialTheme.typography.titleSmall, color = OnSurface)
                Text(employee.jobTitle ?: "", style = MaterialTheme.typography.bodySmall, color = OnSurface.copy(alpha = 0.6f))
                if (employee.hasNfc) {
                    Text("NFC: ${employee.nfcUid}", style = MaterialTheme.typography.bodySmall, color = SuccessColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            // NFC Status Badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (employee.hasNfc) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
            ) {
                Text(
                    if (employee.hasNfc) "✓ NFC" else "Neu beschreiben",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (employee.hasNfc) SuccessColor else Color(0xFFE65100),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun NfcWaitingCard(
    employee: EmployeeItem,
    isWriting: Boolean,
    onCancel: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "nfc")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scale",
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            // Pulsierendes NFC Icon
            Box(
                modifier = Modifier
                    .scale(if (!isWriting) scale else 1f)
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(
                        if (isWriting) Color(0xFF6A1B9A).copy(alpha = 0.15f)
                        else BluePrimary.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Nfc,
                    null,
                    tint = if (isWriting) Color(0xFF6A1B9A) else BluePrimary,
                    modifier = Modifier.size(64.dp),
                )
            }

            Text(
                if (isWriting) "Chip wird beschrieben..." else "Chip an das Gerät halten",
                style = MaterialTheme.typography.headlineSmall,
                color = OnSurface,
            )

            // Mitarbeiter Info
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Default.Person, null, tint = BluePrimary, modifier = Modifier.size(24.dp))
                    Column {
                        Text(employee.name, style = MaterialTheme.typography.titleSmall)
                        Text(employee.jobTitle ?: "", style = MaterialTheme.typography.bodySmall, color = OnSurface.copy(alpha = 0.6f))
                        Text("Code: ${employee.scanCode ?: "–"}", style = MaterialTheme.typography.bodySmall, color = BluePrimary)
                    }
                }
            }

            if (isWriting) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                OutlinedButton(onClick = onCancel, shape = RoundedCornerShape(10.dp)) {
                    Text("Abbrechen")
                }
            }
        }
    }
}

@Composable
private fun NfcSuccessCard(
    employee: EmployeeItem,
    uid: String,
    onNext: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8F5E9)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.CheckCircle, null, tint = SuccessColor, modifier = Modifier.size(60.dp))
            }

            Text("Erfolgreich beschrieben!", style = MaterialTheme.typography.headlineSmall, color = SuccessColor)

            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(employee.name, style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("NFC-UID:", style = MaterialTheme.typography.bodySmall, color = OnSurface.copy(alpha = 0.6f))
                        Text(uid, style = MaterialTheme.typography.bodySmall, color = BluePrimary)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Scan-Code:", style = MaterialTheme.typography.bodySmall, color = OnSurface.copy(alpha = 0.6f))
                        Text(employee.scanCode ?: "–", style = MaterialTheme.typography.bodySmall, color = BluePrimary)
                    }
                }
            }

            Button(
                onClick = onNext,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SuccessColor),
            ) {
                Icon(Icons.Default.ArrowForward, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Nächsten Mitarbeiter", color = Color.White)
            }
        }
    }
}
