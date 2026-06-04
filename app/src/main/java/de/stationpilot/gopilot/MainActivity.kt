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
import de.stationpilot.gopilot.data.repository.SessionStore
import de.stationpilot.gopilot.ui.login.LoginScreen
import de.stationpilot.gopilot.ui.login.LoginViewModel
import de.stationpilot.gopilot.ui.setup.SetupScreen
import de.stationpilot.gopilot.ui.setup.SetupViewModel
import de.stationpilot.gopilot.ui.theme.GoPilotTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

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
    val context = androidx.compose.ui.platform.LocalContext.current
    val session = remember { SessionStore(context) }

    // Startscreen ermitteln: Setup (kein Token) oder Login (Token vorhanden)
    val startScreen = remember {
        val token = runBlocking { session.deviceToken.first() }
        if (token == null) "setup" else "login"
    }

    var currentScreen by remember { mutableStateOf(startScreen) }
    val loginVm: LoginViewModel = viewModel()

    when (currentScreen) {

        // ── Gerät einrichten (erster Start) ──────────────────────────────────
        "setup" -> SetupScreen(
            onSetupComplete = { currentScreen = "login" },
            onOpenCamera    = { onResult ->
                // TODO: Kamera-Screen mit Ergebnis-Callback
                // Platzhalter: direkt zurück mit leerem String
            },
            vm = viewModel(),
        )

        // ── Mitarbeiter Login ─────────────────────────────────────────────────
        "login" -> LoginScreen(
            onLoginSuccess = { currentScreen = "home" },
            onOpenCamera   = { currentScreen = "camera" },
            vm             = loginVm,
        )

        // ── Home / Dashboard ──────────────────────────────────────────────────
        "home" -> {
            // TODO: HomeScreen mit Kacheln + Navigation Drawer
            LoginScreen(
                onLoginSuccess = { },
                onOpenCamera   = { },
                vm             = loginVm,
            )
        }
    }
}
