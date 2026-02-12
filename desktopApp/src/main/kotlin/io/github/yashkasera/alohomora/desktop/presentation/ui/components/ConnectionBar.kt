package io.github.yashkasera.alohomora.desktop.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevToolsViewModel
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilledButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraOutlinedTextField

@Composable
fun ConnectionBar(viewModel: DevToolsViewModel) {
    val state by viewModel.uiState.collectAsState()
    var host by remember { mutableStateOf("127.0.0.1") }
    var port by remember { mutableStateOf("53999") }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.onBackground, RectangleShape)
            .background(MaterialTheme.colorScheme.background, RectangleShape)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Device Connection",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontFamily = FontFamily.Monospace,
                )
                Spacer(modifier = Modifier.width(12.dp))
                StatusPill(state.connection)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                AlohomoraOutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    placeholder = { Text("Host") },
                    singleLine = true,
                    modifier = Modifier.width(200.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                AlohomoraOutlinedTextField(
                    value = port,
                    onValueChange = { port = it.filter { c -> c.isDigit() } },
                    placeholder = { Text("Port") },
                    singleLine = true,
                    modifier = Modifier.width(120.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                AlohomoraFilledButton(
                    text = "Connect",
                    onClick = {
                    val numericPort = port.toIntOrNull() ?: 53999
                    viewModel.switchDevice(host, numericPort, deviceId = null)
                })
                Spacer(modifier = Modifier.width(8.dp))
                AlohomoraFilledButton(
                    text = "Disconnect",
                    onClick = { viewModel.disconnect() },
                )
            }
        }
    }
}
