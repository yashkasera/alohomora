package io.github.yashkasera.alohomora.desktop.presentation.ui.logcat

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import io.github.yashkasera.alohomora.ui.components.ScrollToBottomButton
import io.github.yashkasera.alohomora.ui.components.fabClearanceItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.desktop.domain.model.LogEntry

@Composable
fun LogcatList(
    entries: List<LogEntry>,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val atBottom by remember {
        derivedStateOf {
            val total = listState.layoutInfo.totalItemsCount
            if (total == 0) {
                true
            } else {
                val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                lastVisible >= total - 1
            }
        }
    }

    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty() && atBottom) {
            listState.scrollToItem(entries.lastIndex)
        }
    }

    if (entries.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                .padding(12.dp)
        ) {
            Text(text = "Logcat output will appear here.", style = MaterialTheme.typography.bodySmall)
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
            .padding(8.dp)
    ) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            items(entries) { entry ->
                LogcatRow(entry)
            }
            fabClearanceItem()
        }
        // The follow-while-at-bottom effect above already keeps this pinned; this is the way
        // back after scrolling up to read something, which was the missing half.
        ScrollToBottomButton(listState, entries.size)
    }
}
