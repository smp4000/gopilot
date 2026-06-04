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
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import de.stationpilot.gopilot.data.repository.ConnectionStatus
import de.stationpilot.gopilot.data.repository.SessionStore
import de.stationpilot.gopilot.ui.login.LoginScreen
import de.stationpilot.gopilot.ui.login.LoginViewModel
import de.stationpilot.gopilot.ui.admin.NfcWriterScreen
import de.stationpilot.gopilot.ui.admin.NfcWriterViewModel
import de.stationpilot.gopilot.ui.home.HomeScreen
import de.stationpilot.gopilot.ui.home.HomeViewModel
import de.stationpilot.gopilot.ui.scanner.QrScannerScreen
import de.stationpilot.gopilot.ui.setup.SetupScreen
import de.stationpilot.gopilot.ui.setup.SetupViewModel
import de.stationpilot.gopilot.ui.splash.SplashScreen
import de.stationpilot.gopilot.ui.theme.GoPilotTheme

class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null
    internal var loginVm: LoginViewModel? = null
    internal var nfcWriterVmRef: NfcWriterViewModel? = null
    internal var currentScreenRef: String = "splash"

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
            { tag: Tag ->
                // NFC-Tag je nach aktuellem Screen weiterleiten
                if (currentScreenRef == "admin_nfc") {
                    nfcWriterVmRef?.onNfcTag(tag)
                } else {
                    loginVm?.loginWithNfc(tag)
                }
            },
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

    val context = androidx.compose.ui.platform.LocalContext.current
    val session = remember { SessionStore(context) }

    var currentScreen by remember { mutableStateOf("splash") }
    var scannerCallback by remember { mutableStateOf<((String) -> Unit)?>(null) }
    val setupVm: SetupViewModel = viewModel()
    val loginVm: LoginViewModel = viewModel()
    val nfcWriterVm: NfcWriterViewModel = viewModel()
    val homeVm: HomeViewModel = viewModel()

    // Refs für NFC-Routing in MainActivity
    val activity = context as? MainActivity
    LaunchedEffect(currentScreen) { activity?.currentScreenRef = currentScreen }
    LaunchedEffect(Unit) {
        activity?.loginVm = loginVm
        activity?.nfcWriterVmRef = nfcWriterVm
    }

    // Gerät trennen: Session löschen → zurück zu Setup
    val onResetDevice: suspend () -> Unit = {
        session.clearAll()
        currentScreen = "setup"
    }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

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

        // ── Admin: NFC Writer ─────────────────────────────────────────────────
        "admin_nfc" -> NfcWriterScreen(
            onBack = { currentScreen = "login" },
            vm     = nfcWriterVm,
        )

        // ── Mitarbeiter Login ─────────────────────────────────────────────────
        "login" -> LoginScreen(
            onLoginSuccess    = { currentScreen = "home" },
            onOpenCamera      = { currentScreen = "scanner_login" },
            connectionStatus  = appState.connectionStatus,
            onRetryConnection = { appVm.retryConnection() },
            onResetDevice     = { coroutineScope.launch { session.clearAll(); currentScreen = "setup" } },
            onAdminArea       = { currentScreen = "admin_nfc" },
            vm                = loginVm,
        )

        // ── Home / Dashboard ──────────────────────────────────────────────────
        "home" -> HomeScreen(
            onLogout = { currentScreen = "login" },
            vm       = homeVm,
        )
    }
}
