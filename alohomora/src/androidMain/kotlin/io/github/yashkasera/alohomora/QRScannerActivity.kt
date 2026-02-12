package io.github.yashkasera.alohomora

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

class QRScannerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            if (isGranted) {
                // startCamera()
            }
        }

        setContent {
            var hasPermission by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }

            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (hasPermission) {
                    Text("Camera Scanner Placeholder\n(Requires CameraX dependency)")
                    Button(
                        onClick = {
                            // Simulate Scan
                            Alohomora.connect("ws://192.168.1.5:8080")
                            finish()
                        },
                    ) {
                        Text("Simulate Scan & Connect")
                    }
                } else {
                    Text("Requesting Camera Permission...")
                }
            }
        }
    }
}

// Extension to Alohomora to support connect
fun Alohomora.connect(url: String) {
    // This should call into SyncService via Koin
    // We need to expose a connect method in Alohomora object
}
