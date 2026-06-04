package de.stationpilot.gopilot.ui.login

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.stationpilot.gopilot.data.repository.ConnectionStatus
import de.stationpilot.gopilot.ui.theme.*

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onOpenCamera: () -> Unit,
    connectionStatus: ConnectionStatus = ConnectionStatus.CONNECTED,
    onRetryConnection: () -> Unit = {},
    vm: LoginViewModel = viewModel(),
) {
    val ui by vm.ui.collectAsState()

    LaunchedEffect(ui.success) {
        if (ui.success) onLoginSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BlueBackground),
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        GoPilotHeader(
            stationName = ui.stationName,
            stationCity = ui.stationCity,
        )

        // ── Status-Banner ─────────────────────────────────────────────────────
        ConnectionStatusBanner(
            status = connectionStatus,
            onRetry = onRetryConnection,
        )

        // ── Fehler-Banner ─────────────────────────────────────────────────────
        AnimatedVisibility(visible = ui.error != null) {
            ErrorBanner(message = ui.error ?: "")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Login-Card ────────────────────────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Anmelden",
                    style = MaterialTheme.typography.headlineMedium,
                    color = OnSurface,
                )

                // Code-Eingabe + Kamera-Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = ui.codeInput,
                        onValueChange = vm::onCodeChange,
                        modifier = Modifier.weight(1f),
                        label = { Text("Code eingeben") },
                        leadingIcon = {
                            Icon(Icons.Default.QrCode, contentDescription = null, tint = BluePrimary)
                        },
                        trailingIcon = {
                            IconButton(onClick = vm::toggleShowCode) {
                                Icon(
                                    if (ui.showCode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                )
                            }
                        },
                        visualTransformation = if (ui.showCode) VisualTransformation.None
                                               else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { vm.loginWithPin() }),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                    )

                    // Kamera-Button (blauer Kreis)
                    FilledIconButton(
                        onClick = onOpenCamera,
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = BluePrimary,
                        ),
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "QR-Code scannen",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }

                // Anmelden-Button
                Button(
                    onClick = vm::loginWithPin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    enabled = !ui.isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessColor),
                ) {
                    if (ui.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp,
                        )
                    } else {
                        Icon(Icons.Default.Login, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "ANMELDEN",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Anmeldemethoden Info ──────────────────────────────────────────────
        LoginMethodsInfo(nfcAvailable = ui.nfcAvailable)
    }
}

@Composable
fun GoPilotHeader(stationName: String, stationCity: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(listOf(BluePrimary, Color(0xFF1976D2)))
            )
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // App-Icon Kreis
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.LocalGasStation,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "GoPilot",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                    )
                    if (stationName.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = buildString {
                                    append(stationName)
                                    if (stationCity.isNotEmpty()) append(" $stationCity")
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f),
                            )
                        }
                    }
                }
            }

            IconButton(onClick = { /* TODO: Einstellungen */ }) {
                Icon(Icons.Default.Refresh, contentDescription = "Neu verbinden", tint = Color.White)
            }
        }
    }
}

@Composable
fun ConnectionStatusBanner(
    status: ConnectionStatus,
    onRetry: () -> Unit = {},
) {
    val (bgColor, icon, text, tint, showRetry) = when (status) {
        ConnectionStatus.CONNECTED    -> BannerConfig(Color(0xFFE8F5E9), Icons.Default.CheckCircle,  "Server verbunden",                   SuccessColor, false)
        ConnectionStatus.CHECKING     -> BannerConfig(Color(0xFFFFF8E1), Icons.Default.HourglassTop, "Verbindung wird geprüft...",          Color(0xFFF57F17), false)
        ConnectionStatus.UNREACHABLE  -> BannerConfig(Color(0xFFFFEBEE), Icons.Default.WifiOff,       "Server nicht erreichbar",             ErrorColor,   true)
        ConnectionStatus.TOKEN_INVALID-> BannerConfig(Color(0xFFFFEBEE), Icons.Default.ErrorOutline,  "Gerät nicht mehr registriert",        ErrorColor,   false)
        ConnectionStatus.NO_DEVICE    -> BannerConfig(Color(0xFFFFEBEE), Icons.Default.DeviceUnknown, "Kein Gerät eingerichtet",             ErrorColor,   false)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = tint)
        }
        if (showRetry) {
            TextButton(onClick = onRetry, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                Text("Erneut", style = MaterialTheme.typography.labelMedium, color = tint)
            }
        }
    }
}

private data class BannerConfig(
    val bgColor: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val text: String,
    val tint: Color,
    val showRetry: Boolean,
)

@Composable
fun ErrorBanner(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFEBEE))
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = ErrorColor, modifier = Modifier.size(20.dp))
        Text(text = message, style = MaterialTheme.typography.bodyMedium, color = ErrorColor)
    }
}

@Composable
fun LoginMethodsInfo(nfcAvailable: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.7f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Anmeldemethoden",
                style = MaterialTheme.typography.labelLarge,
                color = OnSurface.copy(alpha = 0.6f),
            )
            LoginMethodRow(icon = Icons.Default.QrCodeScanner, label = "Hardware-Scanner: Taste drücken zum Scannen")
            LoginMethodRow(icon = Icons.Default.CameraAlt, label = "Kamera: Blauen Button antippen")
            if (nfcAvailable) {
                LoginMethodRow(icon = Icons.Default.Nfc, label = "NFC: Karte/Chip auflegen")
            }
        }
    }
}

@Composable
fun LoginMethodRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, contentDescription = null, tint = SuccessColor, modifier = Modifier.size(18.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = OnSurface.copy(alpha = 0.8f))
    }
}
