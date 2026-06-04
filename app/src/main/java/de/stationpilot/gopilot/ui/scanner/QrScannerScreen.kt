package de.stationpilot.gopilot.ui.scanner

import android.Manifest
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import de.stationpilot.gopilot.ui.theme.*
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun QrScannerScreen(
    onResult: (String) -> Unit,
    onBack: () -> Unit,
) {
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        when {
            cameraPermission.status.isGranted -> {
                CameraPreview(onQrDetected = onResult)
                ScannerOverlay(onBack = onBack)
            }

            cameraPermission.status.shouldShowRationale -> {
                PermissionRationale(
                    onRequest = { cameraPermission.launchPermissionRequest() },
                    onBack = onBack,
                )
            }

            else -> {
                PermissionDenied(onBack = onBack)
            }
        }
    }
}

@Composable
private fun CameraPreview(onQrDetected: (String) -> Unit) {
    val context       = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var detected      by remember { mutableStateOf(false) }
    val executor      = remember { Executors.newSingleThreadExecutor() }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(executor) { imageProxy ->
                            if (!detected) {
                                val result = decodeQr(imageProxy)
                                if (result != null) {
                                    detected = true
                                    onQrDetected(result)
                                }
                            }
                            imageProxy.close()
                        }
                    }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalyzer,
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun ScannerOverlay(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {

        // Zurück-Button
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopStart),
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Zurück", tint = Color.White)
        }

        // Titel
        Text(
            text = "QR-Code scannen",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 20.dp),
        )

        // Scan-Rahmen in der Mitte
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.Center)
                .border(3.dp, BluePrimary, RoundedCornerShape(16.dp)),
        )

        // Hinweis unten
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f)),
        ) {
            Text(
                text = "Halte den QR-Code aus StationPilot\n(Tankstelle → GoPilot QR) in den Rahmen",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PermissionRationale(onRequest: () -> Unit, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Kamera-Zugriff benötigt", style = MaterialTheme.typography.headlineMedium, color = Color.White)
        Spacer(Modifier.height(16.dp))
        Text("Um QR-Codes zu scannen wird die Kamera benötigt.", color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRequest) { Text("Erlauben") }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onBack) { Text("Zurück", color = Color.White) }
    }
}

@Composable
private fun PermissionDenied(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Kamera-Zugriff verweigert", style = MaterialTheme.typography.headlineMedium, color = Color.White)
        Spacer(Modifier.height(16.dp))
        Text("Bitte Kamera-Berechtigung in den Android-Einstellungen aktivieren.", color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onBack) { Text("Zurück", color = Color.White) }
    }
}

// ── ZXing QR-Decoder ─────────────────────────────────────────────────────────

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun decodeQr(imageProxy: ImageProxy): String? {
    val mediaImage = imageProxy.image ?: return null

    val buffer = mediaImage.planes[0].buffer
    val bytes  = ByteArray(buffer.remaining())
    buffer.get(bytes)

    val source = PlanarYUVLuminanceSource(
        bytes,
        mediaImage.width,
        mediaImage.height,
        0, 0,
        mediaImage.width,
        mediaImage.height,
        false,
    )

    return try {
        val bitmap = BinaryBitmap(HybridBinarizer(source))
        MultiFormatReader().apply {
            setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)))
        }.decode(bitmap).text
    } catch (e: NotFoundException) {
        null
    }
}
