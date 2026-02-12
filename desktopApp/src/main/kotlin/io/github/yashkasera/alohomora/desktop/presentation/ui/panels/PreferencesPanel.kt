package io.github.yashkasera.alohomora.desktop.presentation.ui.panels

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.PrefsViewModel
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider

@Composable
fun PreferencesPanel(prefsViewModel: PrefsViewModel) {
    val uiState by prefsViewModel.uiState.collectAsState()
    val prefs = uiState.state
    val keys = prefs.keys
    Column(modifier = Modifier.fillMaxSize()) {
        Text(text = "Keys", style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .verticalScroll(rememberScrollState())
        ) {
            keys.forEach { key ->
                Text(
                    text = key,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { prefsViewModel.requestPrefValue(key) }
                        .padding(vertical = 4.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "Values", style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            prefs.values.forEach { (key, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = key,
                        modifier = Modifier.width(200.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = value ?: "null",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                AlohomoraHorizontalDivider()
            }
        }
    }
}
