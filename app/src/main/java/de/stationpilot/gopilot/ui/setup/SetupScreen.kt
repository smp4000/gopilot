package de.stationpilot.gopilot.ui.setup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.stationpilot.gopilot.ui.theme.*

@Composable
fun SetupScreen(
    onSetupComplete: () -> Unit,
    onOpenCamera: (onResult: (String) -> Unit) -> Unit,
    vm: SetupViewModel = viewModel(),
) {
    val ui by vm.ui.collectAsState()

    LaunchedEffect(ui.success) {
        if (ui.success) onSetupComplete()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BlueBackground)
            .verticalScroll(rememberScrollState()),
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(BluePrimary, Color(0xFF1976D2))))
                .padding(horizontal = 20.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("GoPilot einrichten", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                Text("Gerät mit Tankstelle verbinden", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Fehler-Banner ─────────────────────────────────────────────────────
        AnimatedVisibility(visible = ui.error != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFFEBEE))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = ErrorColor)
                Text(ui.error ?: "", style = MaterialTheme.typography.bodyMedium, color = ErrorColor)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Setup Card ────────────────────────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(4.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {

                // ── Schritt 1: Station scannen ─────────────────────────────
                StepHeader(number = "1", title = "Station scannen")

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = ui.stationCode,
                        onValueChange = vm::onStationCodeChange,
                        modifier = Modifier.weight(1f),
                        label = { Text("Station-Code") },
                        placeholder = { Text("QR scannen oder Code eingeben") },
                        leadingIcon = {
                            Icon(Icons.Default.QrCode, contentDescription = null, tint = BluePrimary)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    )

                    // QR-Kamera Button
                    FilledIconButton(
                        onClick = { onOpenCamera { code -> vm.onQrScanned(code) } },
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = BluePrimary),
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "QR scannen", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }

                // Erfolgs-Indikator wenn Code gescannt
                if (ui.stationCode.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessColor, modifier = Modifier.size(18.dp))
                        Text("Station-Code erkannt", style = MaterialTheme.typography.bodySmall, color = SuccessColor)
                    }
                }

                HorizontalDivider(color = Color(0xFFEEEEEE))

                // ── Schritt 2: Gerätename ──────────────────────────────────
                StepHeader(number = "2", title = "Gerätename vergeben")

                OutlinedTextField(
                    value = ui.deviceName,
                    onValueChange = vm::onDeviceNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Gerätename") },
                    placeholder = { Text("z.B. Kasse 1, Lager MDE, Bistro") },
                    leadingIcon = {
                        Icon(Icons.Default.DevicesOther, contentDescription = null, tint = BluePrimary)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done,
                    ),
                )

                // ── Verbinden Button ───────────────────────────────────────
                Button(
                    onClick = vm::register,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    enabled = !ui.isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                ) {
                    if (ui.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
                    } else {
                        Icon(Icons.Default.Link, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("MIT TANKSTELLE VERBINDEN", style = MaterialTheme.typography.labelLarge, color = Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Info-Box ──────────────────────────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BlueContainer.copy(alpha = 0.5f)),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("💡 Wo finde ich den QR-Code?", style = MaterialTheme.typography.labelLarge, color = BlueOnContainer)
                Text(
                    "Im StationPilot Web-Portal → Tankstelle öffnen → Button \"GoPilot QR\" oben rechts. Den QR-Code einmal scannen genügt — das Gerät bleibt dauerhaft verbunden.",
                    style = MaterialTheme.typography.bodySmall,
                    color = BlueOnContainer.copy(alpha = 0.8f),
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun StepHeader(number: String, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(BluePrimary),
            contentAlignment = Alignment.Center,
        ) {
            Text(number, style = MaterialTheme.typography.labelLarge, color = Color.White)
        }
        Text(title, style = MaterialTheme.typography.titleMedium, color = OnSurface)
    }
}
