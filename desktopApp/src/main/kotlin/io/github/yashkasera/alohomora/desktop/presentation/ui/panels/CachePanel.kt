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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.CacheViewModel
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.theme.dimens

@Composable
fun CachePanel(cacheViewModel: CacheViewModel) {
    val uiState by cacheViewModel.uiState.collectAsState()
    val cache = uiState.state
    val keys = cache.keys
    Scaffold(
        topBar = {
            AlohomoraTopBar(
                title = "Cache",
                subtitle = "Live keys and values from connected app",
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxSize(),
        ) {
            Text(text = "Keys", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.sm))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                keys.forEach { key ->
                    Text(
                        text = key,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { cacheViewModel.requestCacheValue(key) }
                            .padding(vertical = MaterialTheme.dimens.margin.xs),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.md))
            Text(text = "Values", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.sm))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                cache.values.forEach { (key, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = MaterialTheme.dimens.margin.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = key,
                            modifier = Modifier.width(200.dp),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            text = value ?: "null",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    AlohomoraHorizontalDivider()
                }
            }
        }
    }
}
