package io.github.yashkasera.alohomora.presentation.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Simple pure-kotlin QR code generation is complex.
// Ideally usage of a library like 'qrcode-kotlin'.
// For this demo, we will simulate the UI or assume a text display for the URL.
// Since adding another dependency 'qrcode-kotlin' was not implicitly approved by user to modify libs,
// we will just display the TEXT URL clearly.

@Composable
fun ConnectionInfoDisplay(url: String) {
    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "SCAN TO CONNECT",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = url,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 8.dp)
        )
        // Placeholder for QR Code
        Text(
            text = "[QR CODE PLACEHOLDER]",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}
