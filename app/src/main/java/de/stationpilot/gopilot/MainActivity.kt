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
import de.stationpilot.gopilot.ui.login.LoginScreen
import de.stationpilot.gopilot.ui.login.LoginViewModel
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
    // TODO: Navigation mit NavController erweitern wenn weitere Screens dazukommen
    // Aktuell: nur Login-Screen als Startpunkt
    var currentScreen by remember { mutableStateOf("login") }
    val loginVm: LoginViewModel = viewModel()

    when (currentScreen) {
        "login" -> LoginScreen(
            onLoginSuccess = { currentScreen = "home" },
            onOpenCamera   = { currentScreen = "camera" },
            vm             = loginVm,
        )
        "home" -> {
            // TODO: HomeScreen (Kacheln + Navigation Drawer)
            // Platzhalter bis HomeScreen implementiert ist
            LoginScreen(
                onLoginSuccess = { },
                onOpenCamera   = { },
                vm             = loginVm,
            )
        }
    }
}
