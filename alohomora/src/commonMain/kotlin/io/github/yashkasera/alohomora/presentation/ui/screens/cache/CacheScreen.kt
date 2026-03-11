package io.github.yashkasera.alohomora.presentation.ui.screens.cache

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.presentation.ui.components.EmptyState
import io.github.yashkasera.alohomora.presentation.ui.components.icons.Icons
import io.github.yashkasera.alohomora.presentation.ui.components.icons.database
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraOutlinedButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraOutlinedTextField

@Composable
internal fun CacheScreen(
    onBackClick: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }

    // Mock data - replace with actual data source
    val memoryStores = remember {
        listOf(
            MemoryStore(
                key = "UserSessionToken",
                value = "eyJhbGciOiJIUzI1NiIsI...",
                type = MemoryStoreType.STRING
            ),
            MemoryStore(
                key = "FeatureFlag_NewNav",
                value = "true",
                type = MemoryStoreType.BOOLEAN
            ),
            MemoryStore(
                key = "AppConfig_CacheTTL",
                value = "3600",
                type = MemoryStoreType.INT
            ),
            MemoryStore(
                key = "LastSyncTimestamp",
                value = "1699452000",
                type = MemoryStoreType.LONG
            ),
            MemoryStore(
                key = "UserProfile_LocalBuffer",
                value = """{ "id": "u_992", "role"…""",
                type = MemoryStoreType.JSON
            ),
            MemoryStore(
                key = "Theme_Preference",
                value = "dark_mode_system",
                type = MemoryStoreType.STRING
            )
        )
    }

    val filteredStores = if (searchQuery.isEmpty()) {
        memoryStores
    } else {
        memoryStores.filter { it.key.contains(searchQuery, ignoreCase = true) }
    }

    val totalSize = calculateTotalSize(memoryStores)

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            CacheTopBar(
                onBackClick = onBackClick,
                isLive = true
            )
        }
    ) { padding ->
        if (memoryStores.isEmpty()) {
            EmptyState(
                icon = Icons.database,
                title = "No Memory Stores",
                subtitle = "Cached data and preferences will appear here",
                modifier = Modifier.padding(padding)
            )
        } else if (filteredStores.isEmpty()) {
            EmptyState(
                icon = Icons.database,
                title = "No Results Found",
                subtitle = "Try adjusting your search query",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))

                    // Header with title and clear button
                    CacheHeader(
                        onClearAllClick = { /* TODO: Clear all */ }
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Search bar
                    SearchTextField(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Active keys header
                    ActiveKeysHeader(
                        count = filteredStores.size,
                        totalSize = totalSize
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Memory store items
                items(filteredStores) { store ->
                    MemoryStoreItem(
                        store = store,
                        onDeleteClick = { /* TODO: Delete item */ }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

// ============================================================================
// Top Bar
// ============================================================================

@Composable
private fun CacheTopBar(
    onBackClick: () -> Unit,
    isLive: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlohomoraIconButton(
            onClick = onBackClick,
            modifier = Modifier.size(48.dp)
        ) {
            Text("←", style = MaterialTheme.typography.headlineMedium)
        }

        if (isLive) {
            LiveIndicator()
        } else {
            Spacer(modifier = Modifier.width(48.dp))
        }
    }
}

@Composable
private fun LiveIndicator() {
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.tertiary)
        )
        Text(
            text = "LIVE",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ============================================================================
// Header Section
// ============================================================================

@Composable
private fun CacheHeader(
    onClearAllClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column {
            Text(
                text = "Memory Stores",
                style = MaterialTheme.typography.displaySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "HEAP INSPECTOR",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AlohomoraOutlinedButton(
            text = "Clear All",
            onClick = onClearAllClick,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("🗑", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "CLEAR ALL",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

// ============================================================================
// Search Bar
// ============================================================================

@Composable
private fun SearchTextField(
    query: String,
    onQueryChange: (String) -> Unit
) {
    AlohomoraOutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text(
                text = "Filter keys by name...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingIcon = {
            Text("🔍", style = MaterialTheme.typography.bodyMedium)
        },
        textStyle = MaterialTheme.typography.bodyMedium,
        singleLine = true
    )
}

// ============================================================================
// Active Keys Header
// ============================================================================

@Composable
private fun ActiveKeysHeader(
    count: Int,
    totalSize: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "ACTIVE KEYS",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = "SIZE: $totalSize",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ============================================================================
// Memory Store Item
// ============================================================================

@Composable
private fun MemoryStoreItem(
    store: MemoryStore,
    onDeleteClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = store.key,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )

            AlohomoraIconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(32.dp)
            ) {
                Text("🗑", style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Value display with type badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ValueBadge(
                value = store.value,
                type = store.type
            )
        }
    }
}

@Composable
private fun ValueBadge(
    value: String,
    type: MemoryStoreType
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(MaterialTheme.shapes.extraSmall)
                .background(getTypeColor(type))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = type.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun getTypeColor(type: MemoryStoreType) = when (type) {
    MemoryStoreType.BOOLEAN -> MaterialTheme.colorScheme.tertiaryContainer
    MemoryStoreType.INT, MemoryStoreType.LONG -> MaterialTheme.colorScheme.secondaryContainer
    MemoryStoreType.JSON -> MaterialTheme.colorScheme.primaryContainer
    MemoryStoreType.STRING -> MaterialTheme.colorScheme.surfaceVariant
}

// ============================================================================
// Data Models
// ============================================================================

data class MemoryStore(
    val key: String,
    val value: String,
    val type: MemoryStoreType
)

enum class MemoryStoreType {
    STRING,
    BOOLEAN,
    INT,
    LONG,
    JSON
}

// ============================================================================
// Helper Functions
// ============================================================================

private fun calculateTotalSize(stores: List<MemoryStore>): String {
    val totalBytes = stores.sumOf { store ->
        store.key.encodeToByteArray().size + store.value.encodeToByteArray().size
    }
    return "${totalBytes / 1024}KB"
}
