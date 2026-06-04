package de.stationpilot.gopilot

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import de.stationpilot.gopilot.data.repository.ConnectionStatus
import de.stationpilot.gopilot.ui.login.LoginScreen
import de.stationpilot.gopilot.ui.login.LoginViewModel
import de.stationpilot.gopilot.ui.scanner.QrScannerScreen
import de.stationpilot.gopilot.ui.setup.SetupScreen
import de.stationpilot.gopilot.ui.setup.SetupViewModel
import de.stationpilot.gopilot.ui.splash.SplashScreen
import de.stationpilot.gopilot.ui.theme.GoPilotTheme

class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private var loginVm: LoginViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        setContent {
            GoPilotTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    GoPilotNavHost()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // NFC Foreground Dispatch aktivieren — App empfängt NFC-Tags bevorzugt
        nfcAdapter?.enableReaderMode(
            this,
            { tag: Tag -> loginVm?.loginWithNfc(tag) },
            NfcAdapter.FLAG_READER_NFC_A or
            NfcAdapter.FLAG_READER_NFC_B or
            NfcAdapter.FLAG_READER_NFC_F or
            NfcAdapter.FLAG_READER_NFC_V,
            null,
        )
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }
}

@Composable
fun GoPilotNavHost() {
    val appVm: AppViewModel  = viewModel()
    val appState by appVm.state.collectAsState()

    var currentScreen by remember { mutableStateOf("splash") }
    var scannerCallback by remember { mutableStateOf<((String) -> Unit)?>(null) }
    val setupVm: SetupViewModel = viewModel()
    val loginVm: LoginViewModel = viewModel()

    // Wenn Initialisierung fertig → zum Startscreen navigieren
    LaunchedEffect(appState.isReady) {
        if (appState.isReady) {
            currentScreen = appState.startDestination
        }
    }

    when (currentScreen) {

        // ── Splash / Verbindungscheck ─────────────────────────────────────────
        "splash" -> SplashScreen()

        // ── Gerät einrichten ─────────────────────────────────────────────────
        "setup" -> SetupScreen(
            onSetupComplete = {
                appVm.onDeviceRegistered()
                currentScreen = "splash"
            },
            onOpenCamera = { onResult ->
                scannerCallback = onResult
                currentScreen = "scanner_setup"
            },
            vm = setupVm,
        )

        // ── QR-Scanner für Setup ──────────────────────────────────────────────
        "scanner_setup" -> QrScannerScreen(
            onResult = { code ->
                scannerCallback?.invoke(code)
                scannerCallback = null
                currentScreen = "setup"
            },
            onBack = { currentScreen = "setup" },
        )

        // ── QR-Scanner für Mitarbeiter-Login ─────────────────────────────────
        "scanner_login" -> QrScannerScreen(
            onResult = { code ->
                loginVm.loginWithScan(code)
                currentScreen = "login"
            },
            onBack = { currentScreen = "login" },
        )

        // ── Mitarbeiter Login ─────────────────────────────────────────────────
        "login" -> LoginScreen(
            onLoginSuccess      = { currentScreen = "home" },
            onOpenCamera        = { currentScreen = "scanner_login" },
            connectionStatus    = appState.connectionStatus,
            onRetryConnection   = { appVm.retryConnection() },
            vm                  = loginVm,
        )

        // ── Home / Dashboard ──────────────────────────────────────────────────
        "home" -> {
            // TODO: HomeScreen
            LoginScreen(
                onLoginSuccess    = { },
                onOpenCamera      = { currentScreen = "scanner_login" },
                connectionStatus  = appState.connectionStatus,
                onRetryConnection = { appVm.retryConnection() },
                vm                = loginVm,
            )
        }
    }
}
