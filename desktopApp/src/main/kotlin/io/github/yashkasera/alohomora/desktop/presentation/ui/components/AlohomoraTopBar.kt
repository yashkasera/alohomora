package io.github.yashkasera.alohomora.desktop.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AlohomoraTopBar(
    title: String,
    subtitle: String? = null,
    showDivider: Boolean,
) {
    Column(modifier = Modifier.fillMaxWidth()
        .background(MaterialTheme.colorScheme.surface)) {
        TopAppBar(
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.displaySmall,
                )
            },
            subtitle = {
                subtitle?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            },
        )
        Spacer(Modifier.height(12.dp))
        if (showDivider) {
            AlohomoraHorizontalDivider()
        }
    }
}
